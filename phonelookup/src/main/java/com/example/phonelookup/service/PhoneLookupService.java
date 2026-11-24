package com.example.phonelookup.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.example.phonelookup.dto.NormalizedPhone;
import com.example.phonelookup.dto.PhoneLookupRequest;
import com.example.phonelookup.dto.PhoneLookupResponse;
import com.example.phonelookup.model.PhoneLookupDocument;
import com.example.phonelookup.repository.PhoneLookupRepository;

@Service
public class PhoneLookupService {

    private final PhoneSanitizerService phoneSanitizerService;
    private final PhoneLookupRepository phoneLookupRepository;
    private final VeriphoneProxyService veriphoneProxyService;
    private final MongoTemplate mongoTemplate;
    private final int freshnessDays;
    private final String veriphoneBaseUrl;

    public PhoneLookupService(
            PhoneSanitizerService phoneSanitizerService,
            PhoneLookupRepository phoneLookupRepository,
            VeriphoneProxyService veriphoneProxyService,
            MongoTemplate mongoTemplate,
            @Value("${lookup.freshness-days}") int freshnessDays,
            @Value("${veriphone.base-url}") String veriphoneBaseUrl) {
        this.phoneSanitizerService = phoneSanitizerService;
        this.phoneLookupRepository = phoneLookupRepository;
        this.veriphoneProxyService = veriphoneProxyService;
        this.mongoTemplate = mongoTemplate;
        this.freshnessDays = freshnessDays;
        this.veriphoneBaseUrl = veriphoneBaseUrl;
    }

    public PhoneLookupResponse lookup(PhoneLookupRequest request) {
        NormalizedPhone normalized = phoneSanitizerService.sanitizeAndFormatE164(
                request.getPhoneNumber(),
                request.getDefaultRegion());
        String e164 = normalized.getPhoneE164();

        Optional<PhoneLookupDocument> existing = phoneLookupRepository.findByPhoneE164(e164);
        if (existing.isPresent() && isFresh(existing.get())) {
            return applyNormalization(toResponse(existing.get(), "DATABASE"), normalized);
        }

        Instant createdAt = existing.map(PhoneLookupDocument::getCreatedAt).orElse(null);
        return applyNormalization(fetchFromVeriphoneAndUpsert(e164, createdAt), normalized);
    }

    private boolean isFresh(PhoneLookupDocument document) {
        if (document.getVeriphone() == null || document.getVeriphone().isEmpty()) {
            return false;
        }
        Instant updatedAt = document.getUpdatedAt();
        if (updatedAt == null) {
            return false;
        }
        Instant threshold = Instant.now().minus(freshnessDays, ChronoUnit.DAYS);
        return !updatedAt.isBefore(threshold);
    }

    private PhoneLookupResponse fetchFromVeriphoneAndUpsert(String e164, Instant existingCreatedAt) {
        Map<String, Object> payload = veriphoneProxyService.fetchPhoneDetails(e164).getPayload();
        Instant now = Instant.now();
        Instant createdAt = existingCreatedAt != null ? existingCreatedAt : now;

        Query query = Query.query(Criteria.where("phoneE164").is(e164));
        Update update = new Update()
                .set("phoneE164", e164)
                .set("veriphone", payload)
                .set("createdAt", createdAt)
                .set("updatedAt", now)
                .unset("valid")
                .unset("countryCode")
                .unset("callingCountryCode")
                .unset("nationalFormat")
                .unset("lineType")
                .unset("carrierName")
                .unset("rawTwilioPayload");

        mongoTemplate.upsert(query, update, PhoneLookupDocument.class);

        return PhoneLookupResponse.builder()
                .phoneE164(e164)
                .source("VERIPHONE")
                .provider(veriphoneBaseUrl)
                .createdAt(createdAt)
                .updatedAt(now)
                .veriphone(payload)
                .build();
    }

    private PhoneLookupResponse applyNormalization(PhoneLookupResponse response, NormalizedPhone normalized) {
        response.setRequestedRegion(normalized.getRequestedRegion());
        response.setAppliedRegion(normalized.getAppliedRegion());
        response.setRegionSource(normalized.getRegionSource());
        return response;
    }

    private PhoneLookupResponse toResponse(PhoneLookupDocument document, String source) {
        return PhoneLookupResponse.builder()
                .phoneE164(document.getPhoneE164())
                .source(source)
                .provider(veriphoneBaseUrl)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .veriphone(document.getVeriphone())
                .build();
    }
}
