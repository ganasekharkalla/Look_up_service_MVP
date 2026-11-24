package com.twilio.lookup.exception;

/**
 * Base class for failures of an upstream lookup provider. Callers should catch the two subclasses
 * ({@link TransientProviderException} and {@link PermanentProviderException}) to tell retryable from
 * non-retryable failures.
 */
public abstract class ProviderException extends RuntimeException {

	protected ProviderException(String message) {
		super(message);
	}

	protected ProviderException(String message, Throwable cause) {
		super(message, cause);
	}
}
