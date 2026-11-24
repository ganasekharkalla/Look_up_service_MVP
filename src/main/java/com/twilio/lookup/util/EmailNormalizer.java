package com.twilio.lookup.util;

import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Trims and lower-cases an email address and rejects obviously malformed input. */
@Component
public class EmailNormalizer {

	/**
	 * @param rawEmail address as typed
	 * @return the normalized address, or empty if it is blank or does not have exactly one @ with text on both sides
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
		return Optional.of(email);
	}
}
