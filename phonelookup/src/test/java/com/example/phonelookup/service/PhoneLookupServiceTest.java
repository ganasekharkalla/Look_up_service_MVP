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

import com.example.phonelookup.dto.PhoneLookupRequest;
import com.example.phonelookup.dto.PhoneLookupResponse;
import com.example.phonelookup.dto.TwilioLookupResult;
import com.example.phonelookup.exception.InvalidPhoneException;
import com.example.phonelookup.model.PhoneLookupDocument;
import com.example.phonelookup.repository.PhoneLookupRepository;

@ExtendWith(MockitoExtension.class)
class PhoneLookupServiceTest {

    private static final String E164 = "+919876543210";

    @Mock
    private PhoneSanitizerService phoneSanitizerService;

    @Mock
    private PhoneLookupRepository phoneLookupRepository;

    @Mock
    private TwilioProxyService twilioProxyService;

    @Mock
    private MongoTemplate mongoTemplate;

    private PhoneLookupService phoneLookupService;

    @BeforeEach
    void setUp() {
        phoneLookupService = new PhoneLookupService(
                phoneSanitizerService,
                phoneLookupRepository,
                twilioProxyService,
                mongoTemplate,
                90);
    }

    @Test
    void invalidPhoneFormatThrowsWithoutDownstreamCalls() {
        PhoneLookupRequest request = new PhoneLookupRequest("not-a-phone", "IN");
        when(phoneSanitizerService.sanitizeAndFormatE164("not-a-phone", "IN"))
                .thenThrow(new InvalidPhoneException("Invalid phone number: not-a-phone"));

        assertThatThrownBy(() -> phoneLookupService.lookup(request))
                .isInstanceOf(InvalidPhoneException.class)
                .hasMessageContaining("Invalid phone number");

        verifyNoInteractions(phoneLookupRepository, mongoTemplate, twilioProxyService);
    }

    @Test
    void freshDatabaseHitReturnsDatabaseSourceWithoutTwilio() {
        PhoneLookupRequest request = new PhoneLookupRequest("9876543210", "IN");
        when(phoneSanitizerService.sanitizeAndFormatE164("9876543210", "IN")).thenReturn(E164);

        Instant updatedAt = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant createdAt = Instant.now().minus(30, ChronoUnit.DAYS);
        PhoneLookupDocument document = PhoneLookupDocument.builder()
                .phoneE164(E164)
                .valid(true)
                .countryCode("IN")
                .callingCountryCode("91")
                .nationalFormat("098765 43210")
                .lineType("mobile")
                .carrierName("Airtel")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        when(phoneLookupRepository.findByPhoneE164(E164)).thenReturn(Optional.of(document));

        PhoneLookupResponse response = phoneLookupService.lookup(request);

        assertThat(response.getSource()).isEqualTo("DATABASE");
        assertThat(response.getPhoneE164()).isEqualTo(E164);
        assertThat(response.getCarrierName()).isEqualTo("Airtel");
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);

        verifyNoInteractions(twilioProxyService);
        verify(mongoTemplate, never()).upsert(any(Query.class), any(Update.class), eq(PhoneLookupDocument.class));
    }

    @Test
    void missingDatabaseEntryCallsTwilioAndUpserts() {
        PhoneLookupRequest request = new PhoneLookupRequest("9876543210", "IN");
        when(phoneSanitizerService.sanitizeAndFormatE164("9876543210", "IN")).thenReturn(E164);
        when(phoneLookupRepository.findByPhoneE164(E164)).thenReturn(Optional.empty());

        TwilioLookupResult twilioResult = sampleTwilioResult();
        when(twilioProxyService.fetchPhoneDetails(E164)).thenReturn(twilioResult);

        PhoneLookupResponse response = phoneLookupService.lookup(request);

        assertThat(response.getSource()).isEqualTo("TWILIO");
        assertThat(response.getPhoneE164()).isEqualTo(E164);
        assertThat(response.getLineType()).isEqualTo("mobile");
        assertThat(response.getCarrierName()).isEqualTo("Airtel");

        verify(twilioProxyService).fetchPhoneDetails(E164);
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).upsert(queryCaptor.capture(), updateCaptor.capture(), eq(PhoneLookupDocument.class));
        assertThat(queryCaptor.getValue().getQueryObject().get("phoneE164")).isEqualTo(E164);
        assertThat(updateCaptor.getValue().getUpdateObject()).isNotEmpty();
    }

    @Test
    void staleDatabaseEntryCallsTwilioAndUpsertsInPlace() {
        PhoneLookupRequest request = new PhoneLookupRequest("9876543210", "IN");
        when(phoneSanitizerService.sanitizeAndFormatE164("9876543210", "IN")).thenReturn(E164);

        PhoneLookupDocument staleDocument = PhoneLookupDocument.builder()
                .phoneE164(E164)
                .valid(true)
                .countryCode("IN")
                .callingCountryCode("91")
                .nationalFormat("098765 43210")
                .lineType("mobile")
                .carrierName("OldCarrier")
                .createdAt(Instant.now().minus(200, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(95, ChronoUnit.DAYS))
                .build();

        when(phoneLookupRepository.findByPhoneE164(E164)).thenReturn(Optional.of(staleDocument));
        when(twilioProxyService.fetchPhoneDetails(E164)).thenReturn(sampleTwilioResult());

        PhoneLookupResponse response = phoneLookupService.lookup(request);

        assertThat(response.getSource()).isEqualTo("TWILIO");
        assertThat(response.getCarrierName()).isEqualTo("Airtel");

        verify(twilioProxyService).fetchPhoneDetails(E164);
        verify(mongoTemplate).upsert(any(Query.class), any(Update.class), eq(PhoneLookupDocument.class));
    }

    private TwilioLookupResult sampleTwilioResult() {
        return TwilioLookupResult.builder()
                .valid(true)
                .countryCode("IN")
                .callingCountryCode("91")
                .nationalFormat("098765 43210")
                .lineType("mobile")
                .carrierName("Airtel")
                .rawTwilioPayload(Map.of(
                        "valid", true,
                        "country_code", "IN",
                        "calling_country_code", "91",
                        "national_format", "098765 43210",
                        "line_type_intelligence", Map.of(
                                "type", "mobile",
                                "carrier_name", "Airtel")))
                .build();
    }
}
