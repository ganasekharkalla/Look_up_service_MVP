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

	@Test
	void tldOfTwoOrMoreCharactersIsAccepted() {
		assertThat(normalizer.normalize("jane@example.co")).contains("jane@example.co");
		assertThat(normalizer.normalize("jane@mail.example.com")).contains("jane@mail.example.com");
	}

	@Test
	void tldShorterThanTwoCharactersIsRejected() {
		assertThat(normalizer.normalize("jane@example.c")).isEmpty();
	}

	@Test
	void domainWithoutDotHasNoTld() {
		assertThat(normalizer.normalize("jane@nodot")).isEmpty();
		assertThat(normalizer.normalize("jane@localhost")).isEmpty();
	}

	@Test
	void emptyOrMisplacedTldDotIsRejected() {
		assertThat(normalizer.normalize("jane@example.")).isEmpty();
		assertThat(normalizer.normalize("jane@.com")).isEmpty();
	}
}
