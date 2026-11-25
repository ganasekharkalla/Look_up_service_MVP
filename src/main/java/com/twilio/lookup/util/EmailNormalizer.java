package com.twilio.lookup.util;

import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Trims and lower-cases an email address and rejects obviously malformed input. */
@Component
public class EmailNormalizer {

	/** The part of the domain after the last dot must be at least this long. */
	static final int MIN_TLD_LENGTH = 2;

	/**
	 * @param rawEmail address as typed
	 * @return the normalized address, or empty if it is blank, does not have exactly one @ with text on both sides,
	 *         or its domain has no top-level domain of at least {@value #MIN_TLD_LENGTH} characters
	 */
	public Optional<String> normalize(String rawEmail) {
		if (rawEmail == null) {
			return Optional.empty();
		}
		String email = rawEmail.trim().toLowerCase(Locale.ROOT);
		int at = email.indexOf('@');
		if (at < 0 || at != email.lastIndexOf('@')) {
			return Optional.empty();
		}
		String local = email.substring(0, at);
		String domain = email.substring(at + 1);
		if (local.isBlank() || domain.isBlank()) {
			return Optional.empty();
		}
		if (!hasValidTld(domain)) {
			return Optional.empty();
		}
		return Optional.of(email);
	}

	/** True when the domain has a dot with a non-empty name before it and at least {@link #MIN_TLD_LENGTH} characters after the last one. */
	private static boolean hasValidTld(String domain) {
		int lastDot = domain.lastIndexOf('.');
		return lastDot > 0 && domain.length() - lastDot - 1 >= MIN_TLD_LENGTH;
	}
}
