package com.twilio.lookup.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.twilio.lookup.config.LookupProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class StalenessPolicyTests {

	private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");

	private final StalenessPolicy policy = new StalenessPolicy(
			new LookupProperties(30), Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void recentRecordIsFresh() {
		assertThat(policy.isStale(NOW)).isFalse();
		assertThat(policy.isStale(NOW.minus(Duration.ofDays(29)))).isFalse();
	}

	@Test
	void recordJustInsideTtlIsFresh() {
		assertThat(policy.isStale(NOW.minus(Duration.ofDays(30)).plusSeconds(1))).isFalse();
	}

	@Test
	void recordExactlyAtTtlIsStale() {
		assertThat(policy.isStale(NOW.minus(Duration.ofDays(30)))).isTrue();
	}

	@Test
	void olderRecordIsStale() {
		assertThat(policy.isStale(NOW.minus(Duration.ofDays(31)))).isTrue();
	}

	@Test
	void ttlComesFromProperties() {
		StalenessPolicy oneDay = new StalenessPolicy(new LookupProperties(1), Clock.fixed(NOW, ZoneOffset.UTC));

		assertThat(oneDay.isStale(NOW.minus(Duration.ofHours(23)))).isFalse();
		assertThat(oneDay.isStale(NOW.minus(Duration.ofHours(25)))).isTrue();
	}

}
