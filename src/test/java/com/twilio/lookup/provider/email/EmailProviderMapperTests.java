package com.twilio.lookup.provider.email;

import static org.assertj.core.api.Assertions.assertThat;

import com.twilio.lookup.model.EmailLookup;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class EmailProviderMapperTests {

	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
	private final EmailProviderMapper mapper = new EmailProviderMapper(Clock.fixed(NOW, ZoneOffset.UTC));

	private static ProviderEmailResponse response() {
		return new ProviderEmailResponse("a@b.com", "Risky", 0.45, "a", "b.com", "a@b.co",
				new ProviderEmailResponse.Checks(
						new ProviderEmailResponse.Domain(true, true, true),
						new ProviderEmailResponse.LocalPart(true),
						new ProviderEmailResponse.Additional(true, false)),
				"127.0.0.1", "MOCK");
	}

	@Test
	void mapsNewEntity() {
		EmailLookup entity = mapper.toEntity("a@b.com", response(), null);

		assertThat(entity.getId()).isEqualTo("a@b.com");
		assertThat(entity.getChannel()).isEqualTo("email");
		assertThat(entity.getSource()).isEqualTo("email-proxy");
		assertThat(entity.getCheckedAt()).isEqualTo(NOW);
		assertThat(entity.getVerdict()).isEqualTo("Risky");
		assertThat(entity.getScore()).isEqualTo(0.45);
		assertThat(entity.getResult().getLocal()).isEqualTo("a");
		assertThat(entity.getResult().getHost()).isEqualTo("b.com");
		assertThat(entity.getResult().getSuggestion()).isEqualTo("a@b.co");
		assertThat(entity.getResult().getChecks().getDomain().isSuspectedDisposableAddress()).isTrue();
		assertThat(entity.getResult().getChecks().getLocalPart().isSuspectedRoleAddress()).isTrue();
		assertThat(entity.getResult().getChecks().getAdditional().isHasKnownBounces()).isTrue();
		assertThat(entity.getResult().getChecks().getAdditional().isHasSuspectedBounces()).isFalse();
	}

	@Test
	void updatesExistingInPlaceKeepingCreatedAtAndVersion() {
		EmailLookup existing = new EmailLookup();
		Instant created = Instant.parse("2025-01-01T00:00:00Z");
		existing.setCreatedAt(created);
		existing.setVersion(2L);

		EmailLookup entity = mapper.toEntity("a@b.com", response(), existing);

		assertThat(entity).isSameAs(existing);
		assertThat(entity.getCreatedAt()).isEqualTo(created);
		assertThat(entity.getVersion()).isEqualTo(2L);
	}
}
