package com.twilio.lookup.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.twilio.lookup.AbstractMongoIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class StaticPageTests extends AbstractMongoIntegrationTest {

	@Autowired MockMvc mvc;

	@Test
	void rootServesTheLandingPage() throws Exception {
		mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
		mvc.perform(get("/index.html")).andExpect(status().isOk())
				.andExpect(content().string(Matchers.allOf(
						Matchers.containsString("href=\"/phone\""),
						Matchers.containsString("href=\"/email\""))));
	}

	@Test
	void phonePathForwardsToThePhonePage() throws Exception {
		mvc.perform(get("/phone")).andExpect(status().isOk()).andExpect(forwardedUrl("/phone.html"));
		mvc.perform(get("/phone.html")).andExpect(status().isOk())
				.andExpect(content().string(Matchers.allOf(
						Matchers.containsString("id=\"phone-form\""),
						Matchers.containsString("id=\"country-button\""),
						Matchers.containsString("id=\"country-search\""),
						Matchers.containsString("id=\"national-number\""),
						Matchers.containsString("id=\"submit-btn\""),
						Matchers.containsString("id=\"result\""),
						Matchers.containsString("/api/v1/lookups/phone"))));
	}

	@Test
	void emailPathForwardsToTheEmailPage() throws Exception {
		mvc.perform(get("/email")).andExpect(status().isOk()).andExpect(forwardedUrl("/email.html"));
		mvc.perform(get("/email.html")).andExpect(status().isOk())
				.andExpect(content().string(Matchers.allOf(
						Matchers.containsString("id=\"email-form\""),
						Matchers.containsString("id=\"email\""),
						Matchers.containsString("id=\"submit-btn\""),
						Matchers.containsString("/api/v1/lookups/email"))));
	}
}
