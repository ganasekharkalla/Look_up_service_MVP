package com.twilio.lookup.provider.phone;

/** Source of phone data. Services depend only on this interface. */
public interface PhoneLookupProvider {

	/**
	 * @param normalizedNumber E.164 number, already validated locally
	 * @return the provider's answer, including a verdict of invalid
	 * @throws com.twilio.lookup.exception.TransientProviderException for retryable failures
	 * @throws com.twilio.lookup.exception.PermanentProviderException for failures that retrying cannot fix
	 */
	ProviderPhoneResponse lookup(String normalizedNumber);
}
