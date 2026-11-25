package com.twilio.lookup.provider.phone;

import static org.assertj.core.api.Assertions.assertThat;

import com.twilio.lookup.model.PhoneLookup;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class PhoneProviderMapperTests {

	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
	private final PhoneProviderMapper mapper = new PhoneProviderMapper(Clock.fixed(NOW, ZoneOffset.UTC));

	private static ProviderPhoneResponse response(String type) {
		return new ProviderPhoneResponse("1", "US", "+14159929960", "(415) 992-9960", true, List.of(),
				type == null ? null : new ProviderPhoneResponse.LineTypeIntelligence(type, "Mock Wireless", "310", "260", null),
				"https://example/url");
	}

	@Test
	void mapsNewEntity() {
		PhoneLookup entity = mapper.toEntity("+14159929960", response("mobile"), null);

		assertThat(entity.getId()).isEqualTo("+14159929960");
		assertThat(entity.getChannel()).isEqualTo("phone");
		assertThat(entity.getSource()).isEqualTo("phone-proxy");
		assertThat(entity.getCheckedAt()).isEqualTo(NOW);
		assertThat(entity.isValid()).isTrue();
		assertThat(entity.isMobile()).isTrue();
		assertThat(entity.getPackages()).containsExactly("line_type_intelligence");
		assertThat(entity.getResult().getCountryCode()).isEqualTo("US");
		assertThat(entity.getResult().getNationalFormat()).isEqualTo("(415) 992-9960");
		assertThat(entity.getResult().getLineTypeIntelligence().getCarrierName()).isEqualTo("Mock Wireless");
	}

	@Test
	void updatesExistingInPlaceKeepingCreatedAtAndVersion() {
		PhoneLookup existing = new PhoneLookup();
		Instant created = Instant.parse("2025-01-01T00:00:00Z");
		existing.setCreatedAt(created);
		existing.setVersion(4L);

		PhoneLookup entity = mapper.toEntity("+14159929960", response("mobile"), existing);

		assertThat(entity).isSameAs(existing);
		assertThat(entity.getCreatedAt()).isEqualTo(created);
		assertThat(entity.getVersion()).isEqualTo(4L);
		assertThat(entity.getCheckedAt()).isEqualTo(NOW);
	}

	@Test
	void mobileOnlyWhenTypeIsMobile() {
		assertThat(mapper.toEntity("+1", response("landline"), null).isMobile()).isFalse();
	}

	@Test
	void packagesEmptyWithoutLineType() {
		PhoneLookup entity = mapper.toEntity("+1", response(null), null);

		assertThat(entity.getPackages()).isEmpty();
		assertThat(entity.isMobile()).isFalse();
		assertThat(entity.getResult().getLineTypeIntelligence()).isNull();
	}

	@Test
	void packageReturnedWithErrorCodeIsStoredButNotListed() {
		ProviderPhoneResponse unsupported = new ProviderPhoneResponse("44", "GB", "+447911123456", "07911 123456", true, List.of(),
				new ProviderPhoneResponse.LineTypeIntelligence("mobile", "Mock Wireless", "234", "10", null),
				new ProviderPhoneResponse.LineStatus(null, 60607),
				new ProviderPhoneResponse.CallerName(null, null, 60607),
				null, null,
				new ProviderPhoneResponse.ReassignedNumber(null, null, "60607"),
				null, null, "https://example/url");

		PhoneLookup entity = mapper.toEntity("+447911123456", unsupported, null);

		assertThat(entity.getPackages()).containsExactly("line_type_intelligence");
		assertThat(entity.getResult().getLineStatus().getErrorCode()).isEqualTo(60607);
		assertThat(entity.getResult().getCallerName().getErrorCode()).isEqualTo(60607);
		assertThat(entity.getResult().getReassignedNumber().getErrorCode()).isEqualTo("60607");
	}

	@Test
	void mapsEveryPackageAndListsThem() {
		Instant swap = Instant.parse("2025-02-03T00:00:00Z");
		ProviderPhoneResponse full = new ProviderPhoneResponse("1", "US", "+14159929960", "(415) 992-9960", true, List.of(),
				new ProviderPhoneResponse.LineTypeIntelligence("mobile", "Mock Wireless", "310", "260", null),
				new ProviderPhoneResponse.LineStatus("active", null),
				new ProviderPhoneResponse.CallerName("JANE", "CONSUMER", null),
				new ProviderPhoneResponse.SimSwap(new ProviderPhoneResponse.LastSimSwap(swap, "PT24H", false), "Mock Wireless", "310", "260", null),
				new ProviderPhoneResponse.CallForwarding(false, null),
				new ProviderPhoneResponse.ReassignedNumber("2025-01-01", "false", null),
				new ProviderPhoneResponse.SmsPumpingRisk("low", false, null, false, 7, null),
				java.util.Map.of("score", 80),
				"https://example/url");

		PhoneLookup entity = mapper.toEntity("+14159929960", full, null);

		assertThat(entity.getPackages()).containsExactly("line_type_intelligence", "line_status", "caller_name",
				"sim_swap", "call_forwarding", "reassigned_number", "sms_pumping_risk", "phone_number_quality_score");
		assertThat(entity.getResult().getLineStatus().getStatus()).isEqualTo("active");
		assertThat(entity.getResult().getCallerName().getCallerName()).isEqualTo("JANE");
		assertThat(entity.getResult().getSimSwap().getLastSimSwap().getLastSimSwapDate()).isEqualTo(swap);
		assertThat(entity.getResult().getCallForwarding().getCallForwardingEnabled()).isFalse();
		assertThat(entity.getResult().getReassignedNumber().getIsNumberReassigned()).isEqualTo("false");
		assertThat(entity.getResult().getSmsPumpingRisk().getSmsPumpingRiskScore()).isEqualTo(7);
		assertThat(entity.getResult().getPhoneNumberQualityScore()).containsEntry("score", 80);
	}
}
