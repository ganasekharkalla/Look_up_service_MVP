package com.twilio.lookup.exception;

/** A provider failure that retrying cannot fix: 400 responses, auth errors and unparseable responses. Maps to HTTP 502. */
public class PermanentProviderException extends ProviderException {

	public PermanentProviderException(String message) {
		super(message);
	}

	public PermanentProviderException(String message, Throwable cause) {
		super(message, cause);
	}
}
