package com.twilio.lookup.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhoneNormalizerTests {

	private final PhoneNormalizer normalizer = new PhoneNormalizer();

	@Test
	void validNationalNumberWithRegion() {
		assertThat(normalizer.normalize("(415) 992-9960", "US")).contains("+14159929960");
		assertThat(normalizer.normalize("(415) 992-9960", "us")).contains("+14159929960");
	}

	@Test
	void validE164() {
		assertThat(normalizer.normalize("+14159929960", null)).contains("+14159929960");
		assertThat(normalizer.normalize("+14159929960", "GB")).contains("+14159929960");
	}

	@Test
	void invalidNumber() {
		assertThat(normalizer.normalize("+141599299600", null)).isEmpty();
	}

	@Test
	void blankOrNull() {
		assertThat(normalizer.normalize("", "US")).isEmpty();
		assertThat(normalizer.normalize("   ", "US")).isEmpty();
		assertThat(normalizer.normalize(null, "US")).isEmpty();
	}

	@Test
	void badCountryCode() {
		assertThat(normalizer.normalize("4159929960", "ZZ")).isEmpty();
		assertThat(normalizer.normalize("4159929960", "USA")).isEmpty();
	}

	@Test
	void nationalNumberWithoutCountryCodeIsInvalid() {
		assertThat(normalizer.normalize("4159929960", null)).isEmpty();
	}

	@Test
	void garbage() {
		assertThat(normalizer.normalize("not a number", "US")).isEmpty();
	}
}
