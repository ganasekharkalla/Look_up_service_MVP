package com.example.phonelookup.dto;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneLookupResponse {

    private String phoneE164;
    private String requestedRegion;
    private String appliedRegion;
    private String regionSource;
    private String source;
    private String provider;
    private Instant createdAt;
    private Instant updatedAt;
    private Map<String, Object> veriphone;
}
