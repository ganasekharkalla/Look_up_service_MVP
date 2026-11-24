package com.twilio.lookup.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTests {

	record Body(@NotBlank String name) {
	}

	@RestController
	static class ThrowingController {

		@GetMapping("/invalid")
		String invalid() {
			throw new InvalidLookupException("bad input");
		}

		@GetMapping("/transient")
		String transientFailure() {
			throw new TransientProviderException("slow");
		}

		@GetMapping("/permanent")
		String permanent() {
			throw new PermanentProviderException("rejected");
		}

		@GetMapping("/boom")
		String boom() {
			throw new IllegalStateException("secret detail");
		}

		@PostMapping("/body")
		String body(@Valid @RequestBody Body body) {
			return body.name();
		}
	}

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
		mvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
				.setControllerAdvice(new GlobalExceptionHandler(clock)).build();
	}

	@Test
	void invalidLookupIs400() throws Exception {
		mvc.perform(get("/invalid")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value("bad input"))
				.andExpect(jsonPath("$.path").value("/invalid"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void validationFailureIs400() throws Exception {
		mvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name")));
	}

	@Test
	void unreadableBodyIs400() throws Exception {
		mvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{not json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.path").value("/body"));
	}

	@Test
	void transientIs503() throws Exception {
		mvc.perform(get("/transient")).andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value(503))
				.andExpect(jsonPath("$.error").value("Service Unavailable"));
	}

	@Test
	void permanentIs502() throws Exception {
		mvc.perform(get("/permanent")).andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.status").value(502))
				.andExpect(jsonPath("$.error").value("Bad Gateway"));
	}

	@Test
	void anythingElseIs500WithoutLeakingDetail() throws Exception {
		mvc.perform(get("/boom")).andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.message").value("Unexpected error"));
	}

	@Test
	void wrongMethodKeepsItsStatus() throws Exception {
		mvc.perform(post("/invalid")).andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.status").value(405));
	}
}
