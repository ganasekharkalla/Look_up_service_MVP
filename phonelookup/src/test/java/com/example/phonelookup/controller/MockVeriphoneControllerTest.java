package com.example.phonelookup.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class MockVeriphoneControllerTest {

    private final MockVeriphoneController controller = new MockVeriphoneController();

    @Test
    void returnsEveryVeriphoneFieldForAnIndianMobile() {
        Map<String, Object> body = controller.verify("+919876543210");

        assertThat(body).containsOnlyKeys(
                "status",
                "phone",
                "phone_valid",
                "phone_type",
                "phone_region",
                "country",
                "country_code",
                "country_prefix",
                "international_number",
                "local_number",
                "e164",
                "carrier");
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("phone_valid")).isEqualTo(true);
        assertThat(body.get("phone_type")).isEqualTo("mobile");
        assertThat(body.get("country")).isEqualTo("India");
        assertThat(body.get("country_code")).isEqualTo("IN");
        assertThat(body.get("country_prefix")).isEqualTo("91");
        assertThat(body.get("e164")).isEqualTo("+919876543210");
        assertThat(body.get("carrier")).isEqualTo("mock");
        assertThat(body.get("international_number")).isEqualTo("+91 98765 43210");
        assertThat(body.get("local_number")).isEqualTo("098765 43210");
    }

    @Test
    void sameNumberReturnsTheSameCarrier() {
        Map<String, Object> first = controller.verify("+14155552671");
        Map<String, Object> second = controller.verify("+14155552671");

        assertThat(first.get("phone_type")).isEqualTo("fixed_line");
        assertThat(first.get("country_code")).isEqualTo("US");
        assertThat(first.get("local_number")).isEqualTo("(415) 555-2671");
        assertThat(second).isEqualTo(first);
    }
}
