package com.example.phonelookup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.example.phonelookup.dto.VeriphoneLookupResult;
import com.example.phonelookup.exception.UpstreamProviderException;
import com.sun.net.httpserver.HttpServer;

class VeriphoneProxyServiceTest {

    private static final String SUCCESS_JSON = """
            {
              "status": "success",
              "phone": "+919876543210",
              "phone_valid": true,
              "phone_type": "mobile",
              "phone_region": "India",
              "country": "India",
              "country_code": "IN",
              "country_prefix": "91",
              "international_number": "+91 98765 43210",
              "local_number": "098765 43210",
              "e164": "+919876543210",
              "carrier": "Airtel"
            }
            """;

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void returnsPayloadFromASingleCall() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        start(exchange -> {
            hits.incrementAndGet();
            write(exchange, 200, SUCCESS_JSON);
        });

        VeriphoneLookupResult result = client().fetchPhoneDetails("+919876543210");

        assertThat(hits).hasValue(1);
        assertThat(result.getPayload()).containsEntry("carrier", "Airtel");
        assertThat(result.getPayload()).containsEntry("e164", "+919876543210");
    }

    @Test
    void returnsProviderHttpErrorOnce() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        start(exchange -> {
            hits.incrementAndGet();
            write(exchange, 400, "{\"status\":\"error\",\"message\":\"invalid number\"}");
        });

        assertThatThrownBy(() -> client().fetchPhoneDetails("+919876543210"))
                .isInstanceOf(UpstreamProviderException.class)
                .hasMessageContaining("invalid number")
                .extracting(ex -> ((UpstreamProviderException) ex).getStatusCode())
                .isEqualTo(400);
        assertThat(hits).hasValue(1);
    }

    @Test
    void returnsProviderStatusErrorWithoutCachingPath() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        start(exchange -> {
            hits.incrementAndGet();
            write(exchange, 200, """
                    {"status":"error","phone":"+00000000","phone_valid":false,"message":"invalid number"}
                    """);
        });

        assertThatThrownBy(() -> client().fetchPhoneDetails("+00000000"))
                .isInstanceOf(UpstreamProviderException.class)
                .hasMessageContaining("invalid number");
        assertThat(hits).hasValue(1);
    }

    @Test
    void encodesLeadingPlusSoTheProviderKeepsE164() throws Exception {
        AtomicReference<String> rawQuery = new AtomicReference<>();
        start(exchange -> {
            rawQuery.set(exchange.getRequestURI().getRawQuery());
            write(exchange, 200, SUCCESS_JSON);
        });

        client().fetchPhoneDetails("+919876543210");

        assertThat(rawQuery.get()).contains("phone=%2B919876543210");
    }

    private VeriphoneProxyService client() {
        RestClient restClient = RestClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .build();
        return new VeriphoneProxyService(restClient, "test-key");
    }

    private void start(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v2/verify", handler);
        server.start();
    }

    private void write(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
