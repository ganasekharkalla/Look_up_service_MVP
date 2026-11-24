package com.example.phonelookup.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.phonelookup.dto.VeriphoneLookupResult;
import com.example.phonelookup.exception.UpstreamProviderException;

@Service
public class VeriphoneProxyService {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient veriphoneRestClient;
    private final String apiKey;

    public VeriphoneProxyService(
            RestClient veriphoneRestClient,
            @Value("${veriphone.api-key:}") String apiKey) {
        this.veriphoneRestClient = veriphoneRestClient;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public VeriphoneLookupResult fetchPhoneDetails(String phoneE164) {
        try {
            Map<String, Object> payload = veriphoneRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/verify")
                            .queryParam("phone", "{phone}")
                            .build(phoneE164))
                    .headers(headers -> {
                        if (StringUtils.hasText(apiKey)) {
                            headers.setBearerAuth(apiKey);
                        }
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        int code = response.getStatusCode().value();
                        String details = readErrorBody(response);
                        String message = StringUtils.hasText(details)
                                ? details
                                : "Veriphone lookup failed with HTTP " + code;
                        throw new UpstreamProviderException(message, code, details);
                    })
                    .body(MAP_TYPE);

            if (payload == null || payload.isEmpty()) {
                throw new UpstreamProviderException("Empty response from Veriphone", 502, null);
            }

            Object status = payload.get("status");
            if (status != null && !"success".equalsIgnoreCase(String.valueOf(status))) {
                String detail = providerDetail(payload);
                throw new UpstreamProviderException(detail, 422, detail);
            }

            return VeriphoneLookupResult.builder().payload(payload).build();
        } catch (UpstreamProviderException ex) {
            throw ex;
        } catch (RestClientException ex) {
            if (ex.getCause() instanceof UpstreamProviderException upstream) {
                throw upstream;
            }
            throw new UpstreamProviderException(
                    "Veriphone lookup call failed: " + ex.getMessage(), ex);
        }
    }

    private String providerDetail(Map<String, Object> payload) {
        Object message = payload.get("message");
        if (message == null) {
            message = payload.get("error");
        }
        if (message == null) {
            message = payload.toString();
        }
        return "Veriphone returned status " + payload.get("status") + ": " + message;
    }

    private String readErrorBody(ClientHttpResponse response) {
        try {
            byte[] bytes = response.getBody().readAllBytes();
            String text = new String(bytes, StandardCharsets.UTF_8).replaceAll("\\s+", " ").trim();
            if (text.length() > 500) {
                return text.substring(0, 500);
            }
            return text;
        } catch (IOException ex) {
            return "";
        }
    }
}
