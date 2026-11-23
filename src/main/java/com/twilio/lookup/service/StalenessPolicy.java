package com.twilio.lookup.service;

import com.twilio.lookup.config.LookupProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Decides whether a stored lookup is past its freshness window. */
@Component
public class StalenessPolicy {

	private final LookupProperties properties;
	private final Clock clock;

	public StalenessPolicy(LookupProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * A lookup is stale once {@code checkedAt + stalenessTtlDays} is reached. The exact expiry instant counts as stale.
	 *
	 * @param checkedAt when the provider result was stored
	 * @return true if the record must be refreshed from the provider
	 */
	public boolean isStale(Instant checkedAt) {
		Instant expiresAt = checkedAt.plus(Duration.ofDays(properties.stalenessTtlDays()));
		return !clock.instant().isBefore(expiresAt);
	}
}
