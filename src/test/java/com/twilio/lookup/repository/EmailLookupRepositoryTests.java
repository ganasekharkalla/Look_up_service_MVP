package com.twilio.lookup.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.twilio.lookup.AbstractMongoIntegrationTest;
import com.twilio.lookup.model.EmailLookup;
import com.twilio.lookup.model.EmailResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EmailLookupRepositoryTests extends AbstractMongoIntegrationTest {

	private static final String EMAIL = "cedric@fogowl.com";

	@Autowired
	private EmailLookupRepository repository;

	@BeforeEach
	void clean() {
		repository.deleteAll();
	}

	@Test
	void savesAndFindsById() {
		Instant checkedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		repository.save(newLookup(checkedAt));

		EmailLookup found = repository.findById(EMAIL).orElseThrow();

		assertThat(found.getId()).isEqualTo(EMAIL);
		assertThat(found.getChannel()).isEqualTo("email");
		assertThat(found.getSource()).isEqualTo("email-proxy");
		assertThat(found.getCheckedAt()).isEqualTo(checkedAt);
		assertThat(found.getVerdict()).isEqualTo("Valid");
		assertThat(found.getScore()).isEqualTo(0.85021);
		assertThat(found.getResult().getLocal()).isEqualTo("cedric");
		assertThat(found.getResult().getHost()).isEqualTo("fogowl.com");
		assertThat(found.getResult().getSuggestion()).isNull();

		EmailResult.Checks checks = found.getResult().getChecks();
		assertThat(checks.getDomain().isHasValidAddressSyntax()).isTrue();
		assertThat(checks.getDomain().isHasMxOrARecord()).isTrue();
		assertThat(checks.getDomain().isSuspectedDisposableAddress()).isFalse();
		assertThat(checks.getLocalPart().isSuspectedRoleAddress()).isFalse();
		assertThat(checks.getAdditional().isHasKnownBounces()).isFalse();
		assertThat(checks.getAdditional().isHasSuspectedBounces()).isFalse();
	}

	@Test
	void findByIdReturnsEmptyWhenMissing() {
		assertThat(repository.findById("nobody@example.com")).isEmpty();
	}

	@Test
	void versionIncrementsOnEachSave() {
		EmailLookup saved = repository.save(newLookup(Instant.now()));
		assertThat(saved.getVersion()).isEqualTo(0L);

		saved.setVerdict("Risky");
		EmailLookup updated = repository.save(saved);

		assertThat(updated.getVersion()).isEqualTo(1L);
		assertThat(repository.findById(EMAIL).orElseThrow().getVersion()).isEqualTo(1L);
	}

	@Test
	void auditFieldsAreSetAndCreatedAtIsKeptOnUpdate() {
		repository.save(newLookup(Instant.now()));
		EmailLookup first = repository.findById(EMAIL).orElseThrow();

		assertThat(first.getCreatedAt()).isNotNull();
		assertThat(first.getUpdatedAt()).isNotNull();

		// save() updates the entity in place, so keep the stored values before saving again
		Instant createdAt = first.getCreatedAt();
		Instant updatedAt = first.getUpdatedAt();

		first.setScore(0.5);
		repository.save(first);
		EmailLookup second = repository.findById(EMAIL).orElseThrow();

		assertThat(second.getCreatedAt()).isEqualTo(createdAt);
		assertThat(second.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
	}

	private static EmailLookup newLookup(Instant checkedAt) {
		EmailResult.Domain domain = new EmailResult.Domain();
		domain.setHasValidAddressSyntax(true);
		domain.setHasMxOrARecord(true);
		domain.setSuspectedDisposableAddress(false);

		EmailResult.LocalPart localPart = new EmailResult.LocalPart();
		localPart.setSuspectedRoleAddress(false);

		EmailResult.Additional additional = new EmailResult.Additional();
		additional.setHasKnownBounces(false);
		additional.setHasSuspectedBounces(false);

		EmailResult.Checks checks = new EmailResult.Checks();
		checks.setDomain(domain);
		checks.setLocalPart(localPart);
		checks.setAdditional(additional);

		EmailResult result = new EmailResult();
		result.setLocal("cedric");
		result.setHost("fogowl.com");
		result.setChecks(checks);

		EmailLookup lookup = new EmailLookup();
		lookup.setId(EMAIL);
		lookup.setChannel("email");
		lookup.setSource("email-proxy");
		lookup.setCheckedAt(checkedAt);
		lookup.setVerdict("Valid");
		lookup.setScore(0.85021);
		lookup.setResult(result);
		return lookup;
	}

}
