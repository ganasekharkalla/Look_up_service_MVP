package com.twilio.lookup.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailNormalizerTests {

	private final EmailNormalizer normalizer = new EmailNormalizer();

	@Test
	void lowercasesAndTrims() {
		assertThat(normalizer.normalize("  Jane.Doe@Example.COM  ")).contains("jane.doe@example.com");
	}

	@Test
	void missingAt() {
		assertThat(normalizer.normalize("jane.example.com")).isEmpty();
	}

	@Test
	void twoAts() {
		assertThat(normalizer.normalize("a@b@c.com")).isEmpty();
	}

	@Test
	void blankOrNull() {
		assertThat(normalizer.normalize("")).isEmpty();
		assertThat(normalizer.normalize("   ")).isEmpty();
		assertThat(normalizer.normalize(null)).isEmpty();
	}

	@Test
	void emptyLocalOrDomain() {
		assertThat(normalizer.normalize("@example.com")).isEmpty();
		assertThat(normalizer.normalize("jane@")).isEmpty();
		assertThat(normalizer.normalize("jane@  ")).isEmpty();
	}
}
