package com.twilio.lookup.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.twilio.lookup.AbstractMongoIntegrationTest;
import com.twilio.lookup.model.PhoneLookup;
import com.twilio.lookup.model.PhoneResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PhoneLookupRepositoryTests extends AbstractMongoIntegrationTest {

	private static final String NUMBER = "+14159929960";

	@Autowired
	private PhoneLookupRepository repository;

	@BeforeEach
	void clean() {
		repository.deleteAll();
	}

	@Test
	void savesAndFindsById() {
		Instant checkedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		repository.save(newLookup(checkedAt));

		PhoneLookup found = repository.findById(NUMBER).orElseThrow();

		assertThat(found.getId()).isEqualTo(NUMBER);
		assertThat(found.getChannel()).isEqualTo("phone");
		assertThat(found.getSource()).isEqualTo("phone-proxy");
		assertThat(found.getCheckedAt()).isEqualTo(checkedAt);
		assertThat(found.getPackages()).containsExactly("line_type_intelligence");
		assertThat(found.isValid()).isTrue();
		assertThat(found.isMobile()).isFalse();
		assertThat(found.getResult().getCallingCountryCode()).isEqualTo("1");
		assertThat(found.getResult().getCountryCode()).isEqualTo("US");
		assertThat(found.getResult().getNationalFormat()).isEqualTo("(415) 992-9960");
		assertThat(found.getResult().getValidationErrors()).isEmpty();

		PhoneResult.LineTypeIntelligence line = found.getResult().getLineTypeIntelligence();
		assertThat(line.getType()).isEqualTo("nonFixedVoip");
		assertThat(line.getCarrierName()).isEqualTo("Twilio - SMS/MMS-SVR");
		assertThat(line.getMobileCountryCode()).isEqualTo("240");
		assertThat(line.getMobileNetworkCode()).isEqualTo("38");
		assertThat(line.getErrorCode()).isNull();

		// packages that were not requested stay null
		assertThat(found.getResult().getLineStatus()).isNull();
		assertThat(found.getResult().getCallerName()).isNull();
		assertThat(found.getResult().getSimSwap()).isNull();
		assertThat(found.getResult().getCallForwarding()).isNull();
		assertThat(found.getResult().getReassignedNumber()).isNull();
		assertThat(found.getResult().getSmsPumpingRisk()).isNull();
		assertThat(found.getResult().getPhoneNumberQualityScore()).isNull();
	}

	@Test
	void storesAllOptionalPackages() {
		Instant swapDate = Instant.parse("2026-11-01T10:15:30Z");
		Instant blockedDate = Instant.parse("2026-10-05T08:00:00Z");
		PhoneLookup lookup = newLookup(Instant.now());
		PhoneResult result = lookup.getResult();

		PhoneResult.LineStatus lineStatus = new PhoneResult.LineStatus();
		lineStatus.setStatus("active");
		result.setLineStatus(lineStatus);

		PhoneResult.CallerName callerName = new PhoneResult.CallerName();
		callerName.setCallerName("Delicious Cheese Cake");
		callerName.setCallerType("CONSUMER");
		result.setCallerName(callerName);

		PhoneResult.LastSimSwap lastSimSwap = new PhoneResult.LastSimSwap();
		lastSimSwap.setLastSimSwapDate(swapDate);
		lastSimSwap.setSwappedPeriod("PT24H");
		lastSimSwap.setSwappedInPeriod(true);
		PhoneResult.SimSwap simSwap = new PhoneResult.SimSwap();
		simSwap.setLastSimSwap(lastSimSwap);
		simSwap.setCarrierName("Vodafone");
		simSwap.setMobileCountryCode("234");
		simSwap.setMobileNetworkCode("15");
		result.setSimSwap(simSwap);

		PhoneResult.CallForwarding callForwarding = new PhoneResult.CallForwarding();
		callForwarding.setCallForwardingEnabled(false);
		result.setCallForwarding(callForwarding);

		PhoneResult.ReassignedNumber reassigned = new PhoneResult.ReassignedNumber();
		reassigned.setLastVerifiedDate("20201227");
		reassigned.setIsNumberReassigned("yes");
		result.setReassignedNumber(reassigned);

		PhoneResult.SmsPumpingRisk risk = new PhoneResult.SmsPumpingRisk();
		risk.setCarrierRiskCategory("low");
		risk.setNumberBlocked(true);
		risk.setNumberBlockedDate(blockedDate);
		risk.setNumberBlockedLast3Months(true);
		risk.setSmsPumpingRiskScore(42);
		result.setSmsPumpingRisk(risk);

		result.setPhoneNumberQualityScore(Map.of("score", 85));

		repository.save(lookup);
		PhoneResult found = repository.findById(NUMBER).orElseThrow().getResult();

		assertThat(found.getLineStatus().getStatus()).isEqualTo("active");
		assertThat(found.getLineStatus().getErrorCode()).isNull();
		assertThat(found.getCallerName().getCallerName()).isEqualTo("Delicious Cheese Cake");
		assertThat(found.getCallerName().getCallerType()).isEqualTo("CONSUMER");
		assertThat(found.getSimSwap().getLastSimSwap().getLastSimSwapDate()).isEqualTo(swapDate);
		assertThat(found.getSimSwap().getLastSimSwap().getSwappedPeriod()).isEqualTo("PT24H");
		assertThat(found.getSimSwap().getLastSimSwap().getSwappedInPeriod()).isTrue();
		assertThat(found.getSimSwap().getCarrierName()).isEqualTo("Vodafone");
		assertThat(found.getSimSwap().getMobileCountryCode()).isEqualTo("234");
		assertThat(found.getSimSwap().getMobileNetworkCode()).isEqualTo("15");
		assertThat(found.getCallForwarding().getCallForwardingEnabled()).isFalse();
		assertThat(found.getReassignedNumber().getLastVerifiedDate()).isEqualTo("20201227");
		assertThat(found.getReassignedNumber().getIsNumberReassigned()).isEqualTo("yes");
		assertThat(found.getSmsPumpingRisk().getCarrierRiskCategory()).isEqualTo("low");
		assertThat(found.getSmsPumpingRisk().getNumberBlocked()).isTrue();
		assertThat(found.getSmsPumpingRisk().getNumberBlockedDate()).isEqualTo(blockedDate);
		assertThat(found.getSmsPumpingRisk().getNumberBlockedLast3Months()).isTrue();
		assertThat(found.getSmsPumpingRisk().getSmsPumpingRiskScore()).isEqualTo(42);
		assertThat(found.getPhoneNumberQualityScore()).containsEntry("score", 85);
	}

	@Test
	void findByIdReturnsEmptyWhenMissing() {
		assertThat(repository.findById("+10000000000")).isEmpty();
	}

	@Test
	void versionIncrementsOnEachSave() {
		PhoneLookup saved = repository.save(newLookup(Instant.now()));
		assertThat(saved.getVersion()).isEqualTo(0L);

		saved.setValid(false);
		PhoneLookup updated = repository.save(saved);

		assertThat(updated.getVersion()).isEqualTo(1L);
		assertThat(repository.findById(NUMBER).orElseThrow().getVersion()).isEqualTo(1L);
	}

	@Test
	void auditFieldsAreSetAndCreatedAtIsKeptOnUpdate() {
		repository.save(newLookup(Instant.now()));
		PhoneLookup first = repository.findById(NUMBER).orElseThrow();

		assertThat(first.getCreatedAt()).isNotNull();
		assertThat(first.getUpdatedAt()).isNotNull();

		// save() updates the entity in place, so keep the stored values before saving again
		Instant createdAt = first.getCreatedAt();
		Instant updatedAt = first.getUpdatedAt();

		first.setMobile(true);
		repository.save(first);
		PhoneLookup second = repository.findById(NUMBER).orElseThrow();

		assertThat(second.getCreatedAt()).isEqualTo(createdAt);
		assertThat(second.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
	}

	private static PhoneLookup newLookup(Instant checkedAt) {
		PhoneResult.LineTypeIntelligence line = new PhoneResult.LineTypeIntelligence();
		line.setType("nonFixedVoip");
		line.setCarrierName("Twilio - SMS/MMS-SVR");
		line.setMobileCountryCode("240");
		line.setMobileNetworkCode("38");

		PhoneResult result = new PhoneResult();
		result.setCallingCountryCode("1");
		result.setCountryCode("US");
		result.setNationalFormat("(415) 992-9960");
		result.setLineTypeIntelligence(line);

		PhoneLookup lookup = new PhoneLookup();
		lookup.setId(NUMBER);
		lookup.setChannel("phone");
		lookup.setSource("phone-proxy");
		lookup.setCheckedAt(checkedAt);
		lookup.setPackages(List.of("line_type_intelligence"));
		lookup.setValid(true);
		lookup.setMobile(false);
		lookup.setResult(result);
		return lookup;
	}

}
