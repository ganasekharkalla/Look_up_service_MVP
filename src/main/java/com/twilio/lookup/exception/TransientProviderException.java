package com.twilio.lookup.exception;

/** A provider failure that may succeed on retry: timeouts, 5xx responses and 429 rate limiting. Maps to HTTP 503. */
public class TransientProviderException extends ProviderException {

	public TransientProviderException(String message) {
		super(message);
	}

	public TransientProviderException(String message, Throwable cause) {
		super(message, cause);
	}
}
