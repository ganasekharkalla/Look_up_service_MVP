package com.twilio.lookup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;

class KeyLockTests {

	@Test
	void sameKeySerializesConcurrentCallersAndEvictsEntry() throws Exception {
		KeyLock keyLock = new KeyLock();
		int threads = 8;
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger activeInside = new AtomicInteger();
		AtomicInteger maxConcurrent = new AtomicInteger();
		AtomicInteger executions = new AtomicInteger();

		ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			List<Future<Integer>> futures = new ArrayList<>();
			for (int i = 0; i < threads; i++) {
				futures.add(pool.submit(() -> {
					ready.countDown();
					assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
					return keyLock.withLock("+14159929960", () -> {
						int current = activeInside.incrementAndGet();
						maxConcurrent.accumulateAndGet(current, Math::max);
						LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
						activeInside.decrementAndGet();
						return executions.incrementAndGet();
					});
				}));
			}

			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			for (Future<Integer> future : futures) {
				future.get(5, TimeUnit.SECONDS);
			}
		}
		finally {
			pool.shutdownNow();
		}

		assertThat(executions.get()).isEqualTo(threads);
		assertThat(maxConcurrent.get()).isEqualTo(1);
		assertThat(keyLock.activeKeyCount()).isZero();
	}

	@Test
	void distinctKeysRunConcurrentlyWithoutBlockingEachOther() throws Exception {
		KeyLock keyLock = new KeyLock();
		CountDownLatch bothInside = new CountDownLatch(2);

		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			Future<Boolean> first = pool.submit(() -> keyLock.withLock("key-a", () -> {
				bothInside.countDown();
				try {
					return bothInside.await(5, TimeUnit.SECONDS);
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					return false;
				}
			}));
			Future<Boolean> second = pool.submit(() -> keyLock.withLock("key-b", () -> {
				bothInside.countDown();
				try {
					return bothInside.await(5, TimeUnit.SECONDS);
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					return false;
				}
			}));

			assertThat(first.get(5, TimeUnit.SECONDS)).isTrue();
			assertThat(second.get(5, TimeUnit.SECONDS)).isTrue();
		}
		finally {
			pool.shutdownNow();
		}

		assertThat(keyLock.activeKeyCount()).isZero();
	}

	@Test
	void cleansUpMapEntryWhenActionThrows() {
		KeyLock keyLock = new KeyLock();

		assertThatThrownBy(() -> keyLock.withLock("boom", () -> {
			throw new IllegalStateException("fail");
		})).isInstanceOf(IllegalStateException.class).hasMessage("fail");

		assertThat(keyLock.activeKeyCount()).isZero();
		assertThat(keyLock.withLock("boom", () -> "recovered")).isEqualTo("recovered");
		assertThat(keyLock.activeKeyCount()).isZero();
	}
}
