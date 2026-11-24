package com.twilio.lookup.provider.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.twilio.lookup.config.ProviderProperties;
import com.twilio.lookup.exception.PermanentProviderException;
import com.twilio.lookup.exception.TransientProviderException;
import com.twilio.lookup.provider.MockFailureSimulator;
import org.junit.jupiter.api.Test;

class MockEmailLookupProviderTests {

	private static MockEmailLookupProvider provider(double failure, double permanent) {
		return new MockEmailLookupProvider(new MockFailureSimulator(new ProviderProperties(5000,
				new ProviderProperties.Retry(3, 1, 2.0), new ProviderProperties.Mock(failure, permanent, 0))));
	}

	@Test
	void verdictsDependOnAddressAndAreDeterministic() {
		MockEmailLookupProvider email = provider(0, 0);

		assertThat(email.lookup("jane@example.com").verdict()).isEqualTo("Valid");
		assertThat(email.lookup("info@example.com").verdict()).isEqualTo("Risky");
		assertThat(email.lookup("jane@mailinator.com").verdict()).isEqualTo("Risky");
		assertThat(email.lookup("jane@nodot").verdict()).isEqualTo("Invalid");
		assertThat(email.lookup("jane@example.com")).isEqualTo(email.lookup("jane@example.com"));
	}

	@Test
	void failureRateThrowsTransient() {
		assertThatThrownBy(() -> provider(1.0, 0).lookup("a@b.com")).isInstanceOf(TransientProviderException.class);
	}

	@Test
	void permanentFailureRateThrowsPermanent() {
		assertThatThrownBy(() -> provider(0, 1.0).lookup("a@b.com")).isInstanceOf(PermanentProviderException.class);
	}
}
