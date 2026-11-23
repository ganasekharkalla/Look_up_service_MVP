package com.example.phonelookup.dto;

import java.time.Instant;

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
    private Boolean valid;
    private String countryCode;
    private String callingCountryCode;
    private String nationalFormat;
    private String lineType;
    private String carrierName;
    private String source;
    private Instant createdAt;
    private Instant updatedAt;
}
