package com.twilio.lookup.dto.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * @param email address to check; the domain must end in a top-level domain of at least two characters
 *              (so {@code jane@example.c} and {@code jane@localhost} are rejected)
 */
public record EmailLookupRequest(
		@NotBlank @Email @Pattern(regexp = "^[^@]+@[^@]+\\.[^@.]{2,}$",
				message = "must end in a top-level domain of at least 2 characters") String email) {
}
