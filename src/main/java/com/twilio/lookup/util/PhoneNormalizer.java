package com.twilio.lookup.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Normalizes phone numbers to E.164 with libphonenumber. This is the final authority on validity. */
@Component
public class PhoneNormalizer {

	private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

	/**
	 * @param rawNumber   number as typed, national or international
	 * @param countryCode optional two-letter ISO region used for national numbers
	 * @return the E.164 number, or empty if the input is blank, the region is unknown, or the number is not valid
	 */
	public Optional<String> normalize(String rawNumber, String countryCode) {
		if (rawNumber == null || rawNumber.isBlank()) {
			return Optional.empty();
		}
		String region = null;
		if (countryCode != null && !countryCode.isBlank()) {
			region = countryCode.trim().toUpperCase(Locale.ROOT);
			if (!phoneUtil.getSupportedRegions().contains(region)) {
				return Optional.empty();
			}
		}
		try {
			PhoneNumber number = phoneUtil.parse(rawNumber.trim(), region);
			if (!phoneUtil.isValidNumber(number)) {
				return Optional.empty();
			}
			return Optional.of(phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164));
		}
		catch (NumberParseException ex) {
			return Optional.empty();
		}
	}
}
