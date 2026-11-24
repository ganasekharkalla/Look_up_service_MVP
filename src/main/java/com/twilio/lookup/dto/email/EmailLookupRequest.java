package com.twilio.lookup.dto.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** @param email address to check */
public record EmailLookupRequest(@NotBlank @Email String email) {
}
