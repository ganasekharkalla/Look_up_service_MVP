package com.example.phonelookup.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.example.phonelookup.dto.PhoneLookupRequest;
import com.example.phonelookup.dto.PhoneLookupResponse;
import com.example.phonelookup.dto.TwilioLookupResult;
import com.example.phonelookup.model.PhoneLookupDocument;
import com.example.phonelookup.repository.PhoneLookupRepository;

@Service
public class PhoneLookupService {

    private final PhoneSanitizerService phoneSanitizerService;
    private final PhoneLookupRepository phoneLookupRepository;
    private final TwilioProxyService twilioProxyService;
    private final MongoTemplate mongoTemplate;
    private final int freshnessDays;

    public PhoneLookupService(
            PhoneSanitizerService phoneSanitizerService,
            PhoneLookupRepository phoneLookupRepository,
            TwilioProxyService twilioProxyService,
            MongoTemplate mongoTemplate,
            @Value("${lookup.freshness-days}") int freshnessDays) {
        this.phoneSanitizerService = phoneSanitizerService;
        this.phoneLookupRepository = phoneLookupRepository;
        this.twilioProxyService = twilioProxyService;
        this.mongoTemplate = mongoTemplate;
        this.freshnessDays = freshnessDays;
    }

    public PhoneLookupResponse lookup(PhoneLookupRequest request) {
        String e164 = phoneSanitizerService.sanitizeAndFormatE164(
                request.getPhoneNumber(),
                request.getDefaultRegion());

        return phoneLookupRepository.findByPhoneE164(e164)
                .filter(this::isFresh)
                .map(doc -> toResponse(doc, "DATABASE"))
                .orElseGet(() -> fetchFromTwilioAndUpsert(e164));
    }

    private boolean isFresh(PhoneLookupDocument document) {
        Instant updatedAt = document.getUpdatedAt();
        if (updatedAt == null) {
            return false;
        }
        Instant threshold = Instant.now().minus(freshnessDays, ChronoUnit.DAYS);
        return !updatedAt.isBefore(threshold);
    }

    private PhoneLookupResponse fetchFromTwilioAndUpsert(String e164) {
        TwilioLookupResult twilioResult = twilioProxyService.fetchPhoneDetails(e164);
        Instant now = Instant.now();

        Query query = Query.query(Criteria.where("phoneE164").is(e164));
        Update update = new Update()
                .set("phoneE164", e164)
                .set("valid", twilioResult.getValid())
                .set("countryCode", twilioResult.getCountryCode())
                .set("callingCountryCode", twilioResult.getCallingCountryCode())
                .set("nationalFormat", twilioResult.getNationalFormat())
                .set("lineType", twilioResult.getLineType())
                .set("carrierName", twilioResult.getCarrierName())
                .set("rawTwilioPayload", twilioResult.getRawTwilioPayload())
                .set("updatedAt", now)
                .setOnInsert("createdAt", now);

        mongoTemplate.upsert(query, update, PhoneLookupDocument.class);

        return PhoneLookupResponse.builder()
                .phoneE164(e164)
                .valid(twilioResult.getValid())
                .countryCode(twilioResult.getCountryCode())
                .callingCountryCode(twilioResult.getCallingCountryCode())
                .nationalFormat(twilioResult.getNationalFormat())
                .lineType(twilioResult.getLineType())
                .carrierName(twilioResult.getCarrierName())
                .source("TWILIO")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private PhoneLookupResponse toResponse(PhoneLookupDocument document, String source) {
        return PhoneLookupResponse.builder()
                .phoneE164(document.getPhoneE164())
                .valid(document.getValid())
                .countryCode(document.getCountryCode())
                .callingCountryCode(document.getCallingCountryCode())
                .nationalFormat(document.getNationalFormat())
                .lineType(document.getLineType())
                .carrierName(document.getCarrierName())
                .source(source)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
