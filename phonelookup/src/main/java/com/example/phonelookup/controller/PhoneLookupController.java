package com.example.phonelookup.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.phonelookup.dto.PhoneLookupRequest;
import com.example.phonelookup.dto.PhoneLookupResponse;
import com.example.phonelookup.service.PhoneLookupService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/lookup")
@RequiredArgsConstructor
@Tag(name = "Phone lookup")
public class PhoneLookupController {

    private final PhoneLookupService phoneLookupService;

    @PostMapping("/phone")
    @Operation(summary = "Look up one phone number")
    public ResponseEntity<PhoneLookupResponse> lookupPhone(
            @Valid @RequestBody PhoneLookupRequest request) {
        return ResponseEntity.ok(phoneLookupService.lookup(request));
    }
}
