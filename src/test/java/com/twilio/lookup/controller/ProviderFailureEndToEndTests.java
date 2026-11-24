package com.twilio.lookup.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.twilio.lookup.AbstractMongoIntegrationTest;
import com.twilio.lookup.model.EmailLookup;
import com.twilio.lookup.model.PhoneLookup;
import com.twilio.lookup.repository.EmailLookupRepository;
import com.twilio.lookup.repository.PhoneLookupRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Mock provider fails transiently on every call: retries run out, so no record gives 503 and an old record is served stale. */
@AutoConfigureMockMvc
@TestPropertySource(properties = "provider.mock.failure-rate=1.0")
class ProviderFailureEndToEndTests extends AbstractMongoIntegrationTest {

	@Autowired MockMvc mvc;
	@Autowired PhoneLookupRepository phones;
	@Autowired EmailLookupRepository emails;

	@BeforeEach
	void clean() {
		phones.deleteAll();
		emails.deleteAll();
	}

	private ResultActions postJson(String url, String json) throws Exception {
		return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	@Test
	void phoneWithoutRecordIs503AndNothingSaved() throws Exception {
		postJson("/api/v1/lookups/phone", "{\"phoneNumber\":\"+14159929960\"}")
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value(503))
				.andExpect(jsonPath("$.error").value("Service Unavailable"))
				.andExpect(jsonPath("$.path").value("/api/v1/lookups/phone"));
		assertThat(phones.count()).isZero();
	}

	@Test
	void emailWithoutRecordIs503AndNothingSaved() throws Exception {
		postJson("/api/v1/lookups/email", "{\"email\":\"jane@example.com\"}")
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value(503));
		assertThat(emails.count()).isZero();
	}

	@Test
	void phoneStaleRecordIsServedWhenProviderFails() throws Exception {
		PhoneLookup old = new PhoneLookup();
		old.setId("+14159929960");
		old.setValid(true);
		old.setCheckedAt(Instant.now().minus(60, ChronoUnit.DAYS));
		phones.save(old);

		postJson("/api/v1/lookups/phone", "{\"phoneNumber\":\"+14159929960\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.source").value("DATABASE"))
				.andExpect(jsonPath("$.stale").value(true));
	}

	@Test
	void emailStaleRecordIsServedWhenProviderFails() throws Exception {
		EmailLookup old = new EmailLookup();
		old.setId("jane@example.com");
		old.setVerdict("Valid");
		old.setCheckedAt(Instant.now().minus(60, ChronoUnit.DAYS));
		emails.save(old);

		postJson("/api/v1/lookups/email", "{\"email\":\"jane@example.com\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.source").value("DATABASE"))
				.andExpect(jsonPath("$.stale").value(true));
	}
}
