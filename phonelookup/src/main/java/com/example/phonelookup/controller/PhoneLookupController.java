package com.example.phonelookup.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.phonelookup.dto.PhoneLookupRequest;
import com.example.phonelookup.dto.PhoneLookupResponse;
import com.example.phonelookup.service.PhoneLookupService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/lookup")
@RequiredArgsConstructor
public class PhoneLookupController {

    private final PhoneLookupService phoneLookupService;

    @PostMapping("/phone")
    public ResponseEntity<PhoneLookupResponse> lookupPhone(
            @Valid @RequestBody PhoneLookupRequest request) {
        return ResponseEntity.ok(phoneLookupService.lookup(request));
    }
}
