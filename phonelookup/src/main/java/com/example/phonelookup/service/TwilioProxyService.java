package com.example.phonelookup.service;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.phonelookup.dto.TwilioLookupResult;
import com.example.phonelookup.exception.UpstreamProviderException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TwilioProxyService {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient twilioRestClient;

    public TwilioLookupResult fetchPhoneDetails(String phoneE164) {
        try {
            Map<String, Object> payload = twilioRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/PhoneNumbers/{phoneE164}")
                            .queryParam("Fields", "line_type_intelligence")
                            .build(phoneE164))
                    .retrieve()
                    .body(MAP_TYPE);

            if (payload == null || payload.isEmpty()) {
                throw new UpstreamProviderException("Empty response from Twilio Lookup API");
            }

            return mapToResult(payload);
        } catch (UpstreamProviderException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new UpstreamProviderException("Twilio Lookup API call failed", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private TwilioLookupResult mapToResult(Map<String, Object> payload) {
        String lineType = null;
        String carrierName = null;

        Object intelligenceObj = payload.get("line_type_intelligence");
        if (intelligenceObj instanceof Map<?, ?> intelligence) {
            Map<String, Object> lineTypeIntelligence = (Map<String, Object>) intelligence;
            Object type = lineTypeIntelligence.get("type");
            Object carrier = lineTypeIntelligence.get("carrier_name");
            lineType = type != null ? String.valueOf(type) : null;
            carrierName = carrier != null ? String.valueOf(carrier) : null;
        }

        return TwilioLookupResult.builder()
                .valid(asBoolean(payload.get("valid")))
                .countryCode(asString(payload.get("country_code")))
                .callingCountryCode(asString(payload.get("calling_country_code")))
                .nationalFormat(asString(payload.get("national_format")))
                .lineType(lineType)
                .carrierName(carrierName)
                .rawTwilioPayload(payload)
                .build();
    }

    private Boolean asBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    private String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }
}
