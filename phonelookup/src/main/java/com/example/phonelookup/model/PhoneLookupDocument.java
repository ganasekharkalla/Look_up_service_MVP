package com.example.phonelookup.model;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "phone_lookups")
public class PhoneLookupDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String phoneE164;

    private Boolean valid;
    private String countryCode;
    private String callingCountryCode;
    private String nationalFormat;
    private String lineType;
    private String carrierName;
    private Map<String, Object> rawTwilioPayload;
    private Instant createdAt;
    private Instant updatedAt;
}
