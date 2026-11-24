package com.twilio.lookup.dto.phone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * @param phoneNumber national or international number
 * @param countryCode optional two-letter ISO region, used when {@code phoneNumber} has no leading +
 */
public record PhoneLookupRequest(
		@NotBlank String phoneNumber,
		@Pattern(regexp = "^[A-Za-z]{2}$", message = "must be a two-letter ISO country code") String countryCode) {
}
