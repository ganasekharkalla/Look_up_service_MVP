package com.example.phonelookup.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwilioLookupResult {

    private Boolean valid;
    private String countryCode;
    private String callingCountryCode;
    private String nationalFormat;
    private String lineType;
    private String carrierName;
    private Map<String, Object> rawTwilioPayload;
}
