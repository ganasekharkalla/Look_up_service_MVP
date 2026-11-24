package com.example.phonelookup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.example.phonelookup.dto.NormalizedPhone;
import com.example.phonelookup.dto.PhoneLookupRequest;
import com.example.phonelookup.dto.PhoneLookupResponse;
import com.example.phonelookup.dto.VeriphoneLookupResult;
import com.example.phonelookup.exception.InvalidPhoneException;
import com.example.phonelookup.model.PhoneLookupDocument;
import com.example.phonelookup.repository.PhoneLookupRepository;

@ExtendWith(MockitoExtension.class)
class PhoneLookupServiceTest {

    private static final String E164 = "+919876543210";
    private static final String PROVIDER = "http://127.0.0.1:8080";

    @Mock
    private PhoneSanitizerService phoneSanitizerService;

    @Mock
    private PhoneLookupRepository phoneLookupRepository;

    @Mock
    private VeriphoneProxyService veriphoneProxyService;

    @Mock
    private MongoTemplate mongoTemplate;

    private PhoneLookupService phoneLookupService;

    @BeforeEach
    void setUp() {
        phoneLookupService = new PhoneLookupService(
                phoneSanitizerService,
                phoneLookupRepository,
                veriphoneProxyService,
                mongoTemplate,
                90,
                PROVIDER);
    }

    @Test
    void invalidPhoneFormatThrowsWithoutDownstreamCalls() {
        PhoneLookupRequest request = new PhoneLookupRequest("not-a-phone", "IN");
        when(phoneSanitizerService.sanitizeAndFormatE164("not-a-phone", "IN"))
                .thenThrow(new InvalidPhoneException("Invalid phone number: not-a-phone"));

        assertThatThrownBy(() -> phoneLookupService.lookup(request))
                .isInstanceOf(InvalidPhoneException.class)
                .hasMessageContaining("Invalid phone number");

        verifyNoInteractions(phoneLookupRepository, mongoTemplate, veriphoneProxyService);
    }

    @Test
    void freshDatabaseHitReturnsDatabaseSourceWithoutVeriphone() {
        PhoneLookupRequest request = new PhoneLookupRequest("9876543210", "IN");
        when(phoneSanitizerService.sanitizeAndFormatE164("9876543210", "IN")).thenReturn(normalized());

        Instant updatedAt = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant createdAt = Instant.now().minus(30, ChronoUnit.DAYS);
        PhoneLookupDocument document = PhoneLookupDocument.builder()
                .phoneE164(E164)
                .veriphone(samplePayload())
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        when(phoneLookupRepository.findByPhoneE164(E164)).thenReturn(Optional.of(document));

        PhoneLookupResponse response = phoneLookupService.lookup(request);

        assertThat(response.getSource()).isEqualTo("DATABASE");
        assertThat(response.getAppliedRegion()).isEqualTo("IN");
        assertThat(response.getRegionSource()).isEqualTo("REGION");
        assertThat(response.getProvider()).isEqualTo(PROVIDER);
        assertThat(response.getPhoneE164()).isEqualTo(E164);
        assertThat(response.getVeriphone()).containsEntry("carrier", "Airtel");
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);

        verifyNoInteractions(veriphoneProxyService);
        verify(mongoTemplate, never()).upsert(any(Query.class), any(Update.class), eq(PhoneLookupDocument.class));
    }

    @Test
    void freshDocumentWithoutVeriphonePayloadCallsProvider() {
        PhoneLookupRequest request = new PhoneLookupRequest("9876543210", "IN");
        when(phoneSanitizerService.sanitizeAndFormatE164("9876543210", "IN")).thenReturn(normalized());
        when(phoneLookupRepository.findByPhoneE164(E164)).thenReturn(Optional.of(
                PhoneLookupDocument.builder()
                        .phoneE164(E164)
                        .updatedAt(Instant.now())
                        .build()));
        when(veriphoneProxyService.fetchPhoneDetails(E164))
                .thenReturn(VeriphoneLookupResult.builder().payload(samplePayload()).build());

        PhoneLookupResponse response = phoneLookupService.lookup(request);

        assertThat(response.getSource()).isEqualTo("VERIPHONE");
        verify(veriphoneProxyService).fetchPhoneDetails(E164);
    }

    @Test
    void missingDatabaseEntryCallsVeriphoneAndUpserts() {
        PhoneLookupRequest request = new PhoneLookupRequest("9876543210", "IN");
        when(phoneSanitizerService.sanitizeAndFormatE164("9876543210", "IN")).thenReturn(normalized());
        when(phoneLookupRepository.findByPhoneE164(E164)).thenReturn(Optional.empty());
        when(veriphoneProxyService.fetchPhoneDetails(E164))
                .thenReturn(VeriphoneLookupResult.builder().payload(samplePayload()).build());

        PhoneLookupResponse response = phoneLookupService.lookup(request);

        assertThat(response.getSource()).isEqualTo("VERIPHONE");
        assertThat(response.getPhoneE164()).isEqualTo(E164);
        assertThat(response.getVeriphone()).containsEntry("phone_type", "mobile");
        assertThat(response.getVeriphone()).containsEntry("country_code", "IN");
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isEqualTo(response.getCreatedAt());

        verify(veriphoneProxyService).fetchPhoneDetails(E164);
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).upsert(queryCaptor.capture(), updateCaptor.capture(), eq(PhoneLookupDocument.class));
        assertThat(queryCaptor.getValue().getQueryObject().get("phoneE164")).isEqualTo(E164);
        assertThat(updateCaptor.getValue().getUpdateObject().toString()).contains("veriphone");
    }

    @Test
    void staleDatabaseEntryCallsVeriphoneAndKeepsCreatedAt() {
        PhoneLookupRequest request = new PhoneLookupRequest("9876543210", "IN");
        when(phoneSanitizerService.sanitizeAndFormatE164("9876543210", "IN")).thenReturn(normalized());

        Instant createdAt = Instant.now().minus(200, ChronoUnit.DAYS);
        PhoneLookupDocument staleDocument = PhoneLookupDocument.builder()
                .phoneE164(E164)
                .veriphone(Map.of("carrier", "OldCarrier"))
                .createdAt(createdAt)
                .updatedAt(Instant.now().minus(95, ChronoUnit.DAYS))
                .build();

        when(phoneLookupRepository.findByPhoneE164(E164)).thenReturn(Optional.of(staleDocument));
        when(veriphoneProxyService.fetchPhoneDetails(E164))
                .thenReturn(VeriphoneLookupResult.builder().payload(samplePayload()).build());

        PhoneLookupResponse response = phoneLookupService.lookup(request);

        assertThat(response.getSource()).isEqualTo("VERIPHONE");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getVeriphone()).containsEntry("carrier", "Airtel");

        verify(veriphoneProxyService).fetchPhoneDetails(E164);
        verify(mongoTemplate).upsert(any(Query.class), any(Update.class), eq(PhoneLookupDocument.class));
    }

    private NormalizedPhone normalized() {
        return NormalizedPhone.builder()
                .phoneE164(E164)
                .requestedRegion("IN")
                .appliedRegion("IN")
                .regionSource("REGION")
                .build();
    }

    private Map<String, Object> samplePayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "success");
        payload.put("phone", E164);
        payload.put("phone_valid", true);
        payload.put("phone_type", "mobile");
        payload.put("phone_region", "India");
        payload.put("country", "India");
        payload.put("country_code", "IN");
        payload.put("country_prefix", "91");
        payload.put("international_number", "+91 98765 43210");
        payload.put("local_number", "098765 43210");
        payload.put("e164", E164);
        payload.put("carrier", "Airtel");
        return payload;
    }
}
