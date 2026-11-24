package com.twilio.lookup.service;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * In-process, reference-counted per-key lock. Serializes concurrent callers requesting the same
 * normalized identifier while allowing distinct keys to run concurrently and evicting entries once
 * no threads hold or wait on a key.
 */
final class KeyLock {

	private static final class Entry {
		final ReentrantLock lock = new ReentrantLock();
		int waiters;
	}

	private final ConcurrentHashMap<String, Entry> locks = new ConcurrentHashMap<>();

	<T> T withLock(String key, Supplier<T> action) {
		Objects.requireNonNull(key, "key must not be null");
		Objects.requireNonNull(action, "action must not be null");
		Entry entry = locks.compute(key, (k, existing) -> {
			Entry e = existing != null ? existing : new Entry();
			e.waiters++;
			return e;
		});
		try {
			entry.lock.lock();
			try {
				return action.get();
			}
			finally {
				entry.lock.unlock();
			}
		}
		finally {
			locks.compute(key, (k, current) -> (current == null || --current.waiters == 0) ? null : current);
		}
	}

	int activeKeyCount() {
		return locks.size();
	}
}
