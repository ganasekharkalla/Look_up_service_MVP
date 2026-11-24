package com.twilio.lookup.controller;

import com.twilio.lookup.dto.email.EmailLookupRequest;
import com.twilio.lookup.dto.email.EmailLookupResponse;
import com.twilio.lookup.service.EmailLookupService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code POST /api/v1/lookups/email}. */
@RestController
@RequestMapping("/api/v1/lookups/email")
@Validated
public class EmailLookupController {

	private final EmailLookupService service;

	public EmailLookupController(EmailLookupService service) {
		this.service = service;
	}

	@PostMapping
	public EmailLookupResponse lookup(@Valid @RequestBody EmailLookupRequest request) {
		return service.lookup(request);
	}
}
