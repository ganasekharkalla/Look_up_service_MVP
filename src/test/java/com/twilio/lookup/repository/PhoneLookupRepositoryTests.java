package com.twilio.lookup.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.twilio.lookup.AbstractMongoIntegrationTest;
import com.twilio.lookup.model.PhoneLookup;
import com.twilio.lookup.model.PhoneResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
