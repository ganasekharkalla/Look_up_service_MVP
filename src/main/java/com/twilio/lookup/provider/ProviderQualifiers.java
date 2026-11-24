package com.twilio.lookup.provider;

/** Qualifier shared by the raw provider beans (mocks now, real clients later) that the resilient wrappers delegate to. */
public final class ProviderQualifiers {

	public static final String DELEGATE = "providerDelegate";

	private ProviderQualifiers() {
	}
}
