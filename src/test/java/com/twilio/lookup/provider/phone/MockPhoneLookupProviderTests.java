package com.twilio.lookup.provider.phone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.twilio.lookup.config.ProviderProperties;
import com.twilio.lookup.exception.PermanentProviderException;
import com.twilio.lookup.exception.TransientProviderException;
import com.twilio.lookup.provider.MockFailureSimulator;
import org.junit.jupiter.api.Test;

class MockPhoneLookupProviderTests {

	private static MockPhoneLookupProvider provider(double failure, double permanent) {
		return new MockPhoneLookupProvider(new MockFailureSimulator(new ProviderProperties(5000,
				new ProviderProperties.Retry(3, 1, 2.0), new ProviderProperties.Mock(failure, permanent, 0))));
	}

	@Test
	void validNumberIsDeterministicAndHasLineType() {
		MockPhoneLookupProvider phone = provider(0, 0);
		ProviderPhoneResponse first = phone.lookup("+14159929960");

		assertThat(first.valid()).isTrue();
		assertThat(first.countryCode()).isEqualTo("US");
		assertThat(first.callingCountryCode()).isEqualTo("1");
		assertThat(first.lineTypeIntelligence()).isNotNull();
		assertThat(first.lineTypeIntelligence().type()).isEqualTo("mobile");
		assertThat(phone.lookup("+14159929960")).isEqualTo(first);
	}

	@Test
	void usMobileNumberGetsEveryApplicablePackage() {
		ProviderPhoneResponse response = provider(0, 0).lookup("+14159929960");

		assertThat(response.lineStatus()).isNotNull();
		assertThat(response.callerName()).isNotNull();
		assertThat(response.simSwap()).isNotNull();
		assertThat(response.reassignedNumber()).isNotNull();
		assertThat(response.smsPumpingRisk()).isNotNull();
		assertThat(response.phoneNumberQualityScore()).containsKey("score");
		assertThat(response.callForwarding()).isNull();
	}

	@Test
	void gbNumberGetsCallForwardingButNoUsOnlyPackages() {
		ProviderPhoneResponse response = provider(0, 0).lookup("+442071838750");

		assertThat(response.valid()).isTrue();
		assertThat(response.callForwarding()).isNotNull();
		assertThat(response.callerName()).isNull();
		assertThat(response.reassignedNumber()).isNull();
	}

	@Test
	void invalidNumberHasNoLineType() {
		ProviderPhoneResponse response = provider(0, 0).lookup("+141599299600");

		assertThat(response.valid()).isFalse();
		assertThat(response.validationErrors()).isNotEmpty();
		assertThat(response.lineTypeIntelligence()).isNull();
	}

	@Test
	void failureRateThrowsTransient() {
		assertThatThrownBy(() -> provider(1.0, 0).lookup("+14159929960")).isInstanceOf(TransientProviderException.class);
	}

	@Test
	void permanentFailureRateThrowsPermanent() {
		assertThatThrownBy(() -> provider(0, 1.0).lookup("+14159929960")).isInstanceOf(PermanentProviderException.class);
	}
}
