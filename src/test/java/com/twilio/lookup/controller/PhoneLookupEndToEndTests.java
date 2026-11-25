package com.twilio.lookup.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.twilio.lookup.AbstractMongoIntegrationTest;
import com.twilio.lookup.repository.PhoneLookupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class PhoneLookupEndToEndTests extends AbstractMongoIntegrationTest {

	private static final String URL = "/api/v1/lookups/phone";

	@Autowired MockMvc mvc;
	@Autowired PhoneLookupRepository repository;

	@BeforeEach
	void clean() {
		repository.deleteAll();
	}

	private org.springframework.test.web.servlet.ResultActions postJson(String json) throws Exception {
		return mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	@Test
	void providerCallThenDatabaseHit() throws Exception {
		String body = "{\"phoneNumber\":\"(415) 992-9960\",\"countryCode\":\"US\"}";

		postJson(body).andExpect(status().isOk())
				.andExpect(jsonPath("$.normalizedNumber").value("+14159929960"))
				.andExpect(jsonPath("$.status").value("VALID"))
				.andExpect(jsonPath("$.source").value("PROVIDER"))
				.andExpect(jsonPath("$.stale").value(false))
				.andExpect(jsonPath("$.lineType").value("mobile"))
				.andExpect(jsonPath("$.mobile").value(true))
				.andExpect(jsonPath("$.countryCode").value("US"))
				.andExpect(jsonPath("$.packages[0]").value("line_type_intelligence"))
				.andExpect(jsonPath("$.lineTypeIntelligence.type").value("mobile"))
				.andExpect(jsonPath("$.lineStatus.status").value("active"))
				.andExpect(jsonPath("$.callerName.callerType").value("CONSUMER"))
				.andExpect(jsonPath("$.simSwap.lastSimSwap.swappedPeriod").value("PT24H"))
				.andExpect(jsonPath("$.reassignedNumber.isNumberReassigned").value("false"))
				.andExpect(jsonPath("$.smsPumpingRisk.carrierRiskCategory").value("low"))
				.andExpect(jsonPath("$.phoneNumberQualityScore.score").exists());

		postJson("{\"phoneNumber\":\"+14159929960\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.source").value("DATABASE"))
				.andExpect(jsonPath("$.simSwap.lastSimSwap.swappedPeriod").value("PT24H"))
				.andExpect(jsonPath("$.callerName.callerType").value("CONSUMER"))
				.andExpect(jsonPath("$.stale").value(false));
	}

	@Test
	void invalidNumberIs400AndNothingStored() throws Exception {
		postJson("{\"phoneNumber\":\"+141599299600\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.path").value(URL));
		org.assertj.core.api.Assertions.assertThat(repository.count()).isZero();
	}

	@Test
	void missingFieldIs400() throws Exception {
		postJson("{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void badCountryCodeIs400() throws Exception {
		postJson("{\"phoneNumber\":\"4159929960\",\"countryCode\":\"USA\"}").andExpect(status().isBadRequest());
	}

	@Test
	void nationalNumberWithoutCountryIs400() throws Exception {
		postJson("{\"phoneNumber\":\"4159929960\"}").andExpect(status().isBadRequest());
	}

	@Test
	void plusNumberWinsOverMismatchedCountryCode() throws Exception {
		postJson("{\"phoneNumber\":\"+14159929960\",\"countryCode\":\"DE\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.normalizedNumber").value("+14159929960"))
				.andExpect(jsonPath("$.countryCode").value("US"));
	}

	@Test
	void invalidPlusNumberWithCountryCodeIs400() throws Exception {
		postJson("{\"phoneNumber\":\"+141599299600\",\"countryCode\":\"US\"}").andExpect(status().isBadRequest());
		org.assertj.core.api.Assertions.assertThat(repository.count()).isZero();
	}
}
