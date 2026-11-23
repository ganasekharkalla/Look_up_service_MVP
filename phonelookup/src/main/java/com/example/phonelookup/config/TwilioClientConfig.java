package com.example.phonelookup.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class TwilioClientConfig {

    @Bean
    public RestClient twilioRestClient(
            RestClient.Builder builder,
            @Value("${twilio.base-url}") String baseUrl,
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken) {

        String credentials = accountSid + ":" + authToken;
        String basicAuth = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        return builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
