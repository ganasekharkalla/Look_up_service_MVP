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
	void rootForwardsToTheIndexPage() throws Exception {
		mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
	}

	@Test
	void indexPageContainsThePhoneLookupForm() throws Exception {
		mvc.perform(get("/index.html")).andExpect(status().isOk())
				.andExpect(content().string(Matchers.allOf(
						Matchers.containsString("id=\"phone-form\""),
						Matchers.containsString("id=\"country-button\""),
						Matchers.containsString("id=\"country-search\""),
						Matchers.containsString("id=\"national-number\""),
						Matchers.containsString("id=\"submit-btn\""),
						Matchers.containsString("id=\"result\""),
						Matchers.containsString("/api/v1/lookups/phone"))));
	}
}
