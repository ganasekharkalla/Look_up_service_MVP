package com.twilio.lookup.controller;

import com.twilio.lookup.dto.phone.PhoneLookupRequest;
import com.twilio.lookup.dto.phone.PhoneLookupResponse;
import com.twilio.lookup.service.PhoneLookupService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code POST /api/v1/lookups/phone}. */
@RestController
@RequestMapping("/api/v1/lookups/phone")
@Validated
public class PhoneLookupController {

	private final PhoneLookupService service;

	public PhoneLookupController(PhoneLookupService service) {
		this.service = service;
	}

	@PostMapping
	public PhoneLookupResponse lookup(@Valid @RequestBody PhoneLookupRequest request) {
		return service.lookup(request);
	}
}
