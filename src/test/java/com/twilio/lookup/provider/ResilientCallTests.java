package com.twilio.lookup.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.twilio.lookup.config.ProviderProperties;
import com.twilio.lookup.exception.PermanentProviderException;
import com.twilio.lookup.exception.TransientProviderException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ResilientCallTests {

	private ResilientCall call;

	private ResilientCall create(long timeoutMs, int maxAttempts) {
		call = new ResilientCall(new ProviderProperties(timeoutMs,
				new ProviderProperties.Retry(maxAttempts, 2, 2.0), new ProviderProperties.Mock(0, 0, 0)));
		return call;
	}

	@AfterEach
	void tearDown() {
		call.shutdown();
	}

	@Test
	void recoversAfterTransientFailures() {
		AtomicInteger calls = new AtomicInteger();
		String result = create(1000, 3).execute(() -> {
			if (calls.incrementAndGet() < 3) {
				throw new TransientProviderException("boom");
			}
			return "ok";
		});

		assertThat(result).isEqualTo("ok");
		assertThat(calls).hasValue(3);
	}

	@Test
	void exhaustsRetriesAndThrowsTransient() {
		AtomicInteger calls = new AtomicInteger();
		ResilientCall resilient = create(1000, 3);

		assertThatThrownBy(() -> resilient.execute(() -> {
			calls.incrementAndGet();
			throw new TransientProviderException("boom");
		})).isInstanceOf(TransientProviderException.class);
		assertThat(calls).hasValue(3);
	}

	@Test
	void permanentFailureIsNotRetried() {
		AtomicInteger calls = new AtomicInteger();
		ResilientCall resilient = create(1000, 3);

		assertThatThrownBy(() -> resilient.execute(() -> {
			calls.incrementAndGet();
			throw new PermanentProviderException("nope");
		})).isInstanceOf(PermanentProviderException.class);
		assertThat(calls).hasValue(1);
	}

	@Test
	void timeoutBecomesTransientAndIsRetried() {
		AtomicInteger calls = new AtomicInteger();
		ResilientCall resilient = create(30, 2);

		assertThatThrownBy(() -> resilient.execute(() -> {
			calls.incrementAndGet();
			try {
				Thread.sleep(2000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return "late";
		})).isInstanceOf(TransientProviderException.class).hasMessageContaining("timed out");
		assertThat(calls).hasValue(2);
	}

	@Test
	void backoffGrowsAndStaysBounded() {
		ResilientCall resilient = new ResilientCall(new ProviderProperties(1000,
				new ProviderProperties.Retry(20, 100, 10.0), new ProviderProperties.Mock(0, 0, 0)));
		call = resilient;

		for (int i = 0; i < 50; i++) {
			assertThat(resilient.backoffMs(1)).isBetween(50L, 100L);
			assertThat(resilient.backoffMs(2)).isBetween(500L, 1000L);
			assertThat(resilient.backoffMs(15)).isBetween(ResilientCall.MAX_BACKOFF_MS / 2, ResilientCall.MAX_BACKOFF_MS);
		}
	}
}
