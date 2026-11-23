package com.twilio.lookup.dto;

/** Where a lookup result came from. v1 has no cache, so only these two values exist. */
public enum LookupSource {
	DATABASE,
	PROVIDER
}
