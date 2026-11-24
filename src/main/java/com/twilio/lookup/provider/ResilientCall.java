package com.twilio.lookup.provider;

import com.twilio.lookup.config.ProviderProperties;
import com.twilio.lookup.exception.TransientProviderException;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Applies the strict timeout and the retry policy to one provider call. A call that exceeds
 * {@code provider.timeout-ms} becomes a {@link TransientProviderException}. Only transient failures are retried, up
 * to {@code provider.retry.max-attempts} calls in total, with exponential backoff and jitter. Permanent failures
 * propagate immediately.
 */
@Component
public class ResilientCall {

	private static final Logger log = LoggerFactory.getLogger(ResilientCall.class);
	/** Upper bound for a single backoff wait, so a large multiplier cannot stall a request. */
	static final long MAX_BACKOFF_MS = 10_000;

	private final ProviderProperties properties;
	private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
		Thread thread = new Thread(runnable, "provider-call");
		thread.setDaemon(true);
		return thread;
	});

	public ResilientCall(ProviderProperties properties) {
		this.properties = properties;
	}

	public <T> T execute(Supplier<T> call) {
		int maxAttempts = Math.max(1, properties.retry().maxAttempts());
		TransientProviderException last = null;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				return callWithTimeout(call);
			}
			catch (TransientProviderException ex) {
				last = ex;
				if (attempt < maxAttempts) {
					long wait = backoffMs(attempt);
					log.warn("Provider call failed (attempt {}/{}): {}. Retrying in {} ms", attempt, maxAttempts, ex.getMessage(), wait);
					sleep(wait);
				}
			}
		}
		throw last;
	}

	/** Backoff before retry number {@code attempt}: between half and all of {@code initial * multiplier^(attempt-1)}, capped. */
	long backoffMs(int attempt) {
		double base = properties.retry().initialBackoffMs() * Math.pow(properties.retry().multiplier(), attempt - 1);
		long capped = (long) Math.min(base, MAX_BACKOFF_MS);
		if (capped <= 0) {
			return 0;
		}
		long half = capped / 2;
		return half + ThreadLocalRandom.current().nextLong(capped - half + 1);
	}

	private <T> T callWithTimeout(Supplier<T> call) {
		Future<T> future = executor.submit(call::get);
		try {
			return future.get(properties.timeoutMs(), TimeUnit.MILLISECONDS);
		}
		catch (TimeoutException ex) {
			future.cancel(true);
			throw new TransientProviderException("Provider call timed out after " + properties.timeoutMs() + " ms", ex);
		}
		catch (ExecutionException ex) {
			if (ex.getCause() instanceof RuntimeException runtime) {
				throw runtime;
			}
			throw new IllegalStateException(ex.getCause());
		}
		catch (InterruptedException ex) {
			future.cancel(true);
			Thread.currentThread().interrupt();
			throw new TransientProviderException("Provider call interrupted", ex);
		}
	}

	private void sleep(long millis) {
		if (millis <= 0) {
			return;
		}
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new TransientProviderException("Interrupted while backing off", ex);
		}
	}

	@PreDestroy
	void shutdown() {
		executor.shutdownNow();
	}
}
