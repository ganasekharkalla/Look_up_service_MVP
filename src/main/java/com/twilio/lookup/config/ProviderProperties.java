package com.twilio.lookup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Provider settings, bound from the {@code provider.*} properties.
 *
 * @param timeoutMs strict timeout for one provider call, in milliseconds
 * @param retry     retry policy for transient failures
 * @param mock      behaviour of the mock providers
 */
@ConfigurationProperties(prefix = "provider")
public record ProviderProperties(
		@DefaultValue("5000") long timeoutMs,
		@DefaultValue Retry retry,
		@DefaultValue Mock mock) {

	/**
	 * Retry policy.
	 *
	 * @param maxAttempts      total attempts, counting the first call
	 * @param initialBackoffMs wait before the first retry, in milliseconds
	 * @param multiplier       factor applied to the backoff after each failed attempt
	 */
	public record Retry(
			@DefaultValue("3") int maxAttempts,
			@DefaultValue("200") long initialBackoffMs,
			@DefaultValue("2.0") double multiplier) {
	}

	/**
	 * Mock provider behaviour.
	 *
	 * @param failureRate          probability (0 to 1) of a transient failure
	 * @param permanentFailureRate probability (0 to 1) of a permanent failure
	 * @param latencyMs            artificial latency per call, in milliseconds
	 */
	public record Mock(
			@DefaultValue("0.0") double failureRate,
			@DefaultValue("0.0") double permanentFailureRate,
			@DefaultValue("0") long latencyMs) {
	}
}
