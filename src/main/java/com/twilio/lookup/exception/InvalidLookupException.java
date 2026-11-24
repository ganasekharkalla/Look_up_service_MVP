package com.twilio.lookup.exception;

/** The input could not be normalized locally (bad phone number or email). Nothing is stored and the provider is never called. Maps to HTTP 400. */
public class InvalidLookupException extends RuntimeException {

	public InvalidLookupException(String message) {
		super(message);
	}
}
