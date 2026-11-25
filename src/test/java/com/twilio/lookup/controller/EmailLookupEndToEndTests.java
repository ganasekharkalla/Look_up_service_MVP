package com.twilio.lookup.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.twilio.lookup.AbstractMongoIntegrationTest;
import com.twilio.lookup.repository.EmailLookupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@AutoConfigureMockMvc
class EmailLookupEndToEndTests extends AbstractMongoIntegrationTest {

	private static final String URL = "/api/v1/lookups/email";

	@Autowired MockMvc mvc;
	@Autowired EmailLookupRepository repository;

	@BeforeEach
	void clean() {
		repository.deleteAll();
	}

	private ResultActions postJson(String json) throws Exception {
		return mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	@Test
	void providerCallThenDatabaseHit() throws Exception {
		postJson("{\"email\":\"Jane@Example.com\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.normalizedEmail").value("jane@example.com"))
				.andExpect(jsonPath("$.status").value("VALID"))
				.andExpect(jsonPath("$.source").value("PROVIDER"))
				.andExpect(jsonPath("$.verdict").value("Valid"))
				.andExpect(jsonPath("$.checks.hasMxOrARecord").value(true))
				.andExpect(jsonPath("$.stale").value(false));

		postJson("{\"email\":\"jane@example.com\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.source").value("DATABASE"));
	}

	@Test
	void riskyVerdictIsValidStatus() throws Exception {
		postJson("{\"email\":\"info@example.com\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("VALID"))
				.andExpect(jsonPath("$.verdict").value("Risky"))
				.andExpect(jsonPath("$.checks.suspectedRoleAddress").value(true));
	}

	@Test
	void tldShorterThanTwoCharactersIs400AndNothingStored() throws Exception {
		postJson("{\"email\":\"jane@example.c\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
		postJson("{\"email\":\"jane@nodot\"}").andExpect(status().isBadRequest());
		postJson("{\"email\":\"jane@example.\"}").andExpect(status().isBadRequest());
		org.assertj.core.api.Assertions.assertThat(repository.count()).isZero();
	}

	@Test
	void twoCharacterTldIsAccepted() throws Exception {
		postJson("{\"email\":\"jane@example.co\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.normalizedEmail").value("jane@example.co"));
	}

	@Test
	void missingOrMalformedEmailIs400() throws Exception {
		postJson("{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
		postJson("{\"email\":\"not-an-email\"}").andExpect(status().isBadRequest());
		org.assertj.core.api.Assertions.assertThat(repository.count()).isZero();
	}
}
