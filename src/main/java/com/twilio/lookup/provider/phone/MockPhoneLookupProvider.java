package com.twilio.lookup.provider.phone;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.twilio.lookup.provider.MockFailureSimulator;
import com.twilio.lookup.provider.ProviderQualifiers;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Twilio-shaped mock. Numbers that libphonenumber considers valid get {@code valid=true} and every data package that
 * applies to their country and line type (caller name and reassigned number for US, SIM swap for mobile, call
 * forwarding for GB). Values derive from the number, so the same input always gives the same answer.
 */
@Component
@Profile("!real-provider")
@Qualifier(ProviderQualifiers.DELEGATE)
public class MockPhoneLookupProvider implements PhoneLookupProvider {

	private static final List<String> CARRIERS = List.of("Mock Wireless", "Example Telecom", "Sample Mobile");

	private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
	private final MockFailureSimulator simulator;

	MockPhoneLookupProvider(MockFailureSimulator simulator) {
		this.simulator = simulator;
	}

	@Override
	public ProviderPhoneResponse lookup(String normalizedNumber) {
		simulator.simulate();
		String url = "https://lookups.twilio.com/v2/PhoneNumbers/" + normalizedNumber;
		PhoneNumber number;
		try {
			number = phoneUtil.parse(normalizedNumber, null);
		}
		catch (NumberParseException ex) {
			return invalid(normalizedNumber, url, "NOT_A_NUMBER");
		}
		if (!phoneUtil.isValidNumber(number)) {
			return invalid(normalizedNumber, url, "INVALID_LENGTH");
		}
		String region = phoneUtil.getRegionCodeForNumber(number);
		int seed = Math.floorMod(normalizedNumber.hashCode(), 1000);
		String carrier = CARRIERS.get(seed % CARRIERS.size());
		String type = lineType(number);
		boolean us = "US".equals(region);
		return new ProviderPhoneResponse(
				String.valueOf(number.getCountryCode()),
				region,
				normalizedNumber,
				phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.NATIONAL),
				true,
				List.of(),
				new ProviderPhoneResponse.LineTypeIntelligence(type, carrier, "310", "260", null),
				new ProviderPhoneResponse.LineStatus("active", null),
				us ? new ProviderPhoneResponse.CallerName("MOCK CALLER " + seed, "CONSUMER", null) : null,
				"mobile".equals(type) ? simSwap(seed, carrier) : null,
				"GB".equals(region) ? new ProviderPhoneResponse.CallForwarding(false, null) : null,
				us ? new ProviderPhoneResponse.ReassignedNumber("2025-01-01", "false", null) : null,
				new ProviderPhoneResponse.SmsPumpingRisk("low", false, null, false, 5 + seed % 20, null),
				Map.of("score", 70 + seed % 30),
				url);
	}

	/** Fixed base date plus a number-derived offset, so the answer never depends on the current time. */
	private ProviderPhoneResponse.SimSwap simSwap(int seed, String carrier) {
		Instant swapDate = Instant.parse("2025-01-01T00:00:00Z").plus(seed, ChronoUnit.DAYS);
		return new ProviderPhoneResponse.SimSwap(
				new ProviderPhoneResponse.LastSimSwap(swapDate, "PT24H", false), carrier, "310", "260", null);
	}

	private ProviderPhoneResponse invalid(String number, String url, String reason) {
		return new ProviderPhoneResponse(null, null, number, null, false, List.of(reason), null, url);
	}

	private String lineType(PhoneNumber number) {
		return switch (phoneUtil.getNumberType(number)) {
			case MOBILE, FIXED_LINE_OR_MOBILE -> "mobile";
			case FIXED_LINE -> "landline";
			case VOIP -> "nonFixedVoip";
			case TOLL_FREE -> "tollFree";
			default -> "unknown";
		};
	}
}
