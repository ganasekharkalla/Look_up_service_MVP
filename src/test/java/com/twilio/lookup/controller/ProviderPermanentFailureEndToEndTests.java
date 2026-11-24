package com.twilio.lookup.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.twilio.lookup.AbstractMongoIntegrationTest;
import com.twilio.lookup.provider.email.EmailLookupProvider;
import com.twilio.lookup.provider.phone.PhoneLookupProvider;
import com.twilio.lookup.provider.email.ResilientEmailLookupProvider;
import com.twilio.lookup.provider.phone.ResilientPhoneLookupProvider;
import com.twilio.lookup.repository.EmailLookupRepository;
import com.twilio.lookup.repository.PhoneLookupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Mock provider fails permanently on every call: 502 with ApiError, nothing saved. Also checks the provider bean wiring. */
@AutoConfigureMockMvc
@TestPropertySource(properties = "provider.mock.permanent-failure-rate=1.0")
class ProviderPermanentFailureEndToEndTests extends AbstractMongoIntegrationTest {

	@Autowired MockMvc mvc;
	@Autowired PhoneLookupRepository phones;
	@Autowired EmailLookupRepository emails;
	@Autowired PhoneLookupProvider phoneProvider;
	@Autowired EmailLookupProvider emailProvider;

	@BeforeEach
	void clean() {
		phones.deleteAll();
		emails.deleteAll();
	}

	@Test
	void servicesGetTheResilientWrapperAsTheOnlyPrimaryBean() {
		assertThat(phoneProvider).isInstanceOf(ResilientPhoneLookupProvider.class);
		assertThat(emailProvider).isInstanceOf(ResilientEmailLookupProvider.class);
	}

	@Test
	void phoneIs502() throws Exception {
		mvc.perform(post("/api/v1/lookups/phone").contentType(MediaType.APPLICATION_JSON)
				.content("{\"phoneNumber\":\"+14159929960\"}"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.status").value(502))
				.andExpect(jsonPath("$.error").value("Bad Gateway"));
		assertThat(phones.count()).isZero();
	}

	@Test
	void emailIs502() throws Exception {
		mvc.perform(post("/api/v1/lookups/email").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"jane@example.com\"}"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.status").value(502));
		assertThat(emails.count()).isZero();
	}
}
