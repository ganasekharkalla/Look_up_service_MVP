package com.twilio.lookup.provider.email;

/** Source of email validation data. Services depend only on this interface. */
public interface EmailLookupProvider {

	/**
	 * @param normalizedEmail trimmed, lower-case address
	 * @return the provider's answer, including a verdict of {@code Invalid}
	 * @throws com.twilio.lookup.exception.TransientProviderException for retryable failures
	 * @throws com.twilio.lookup.exception.PermanentProviderException for failures that retrying cannot fix
	 */
	ProviderEmailResponse lookup(String normalizedEmail);
}
