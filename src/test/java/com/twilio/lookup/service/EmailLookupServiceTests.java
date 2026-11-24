package com.twilio.lookup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.twilio.lookup.dto.email.EmailLookupRequest;
import com.twilio.lookup.dto.email.EmailLookupResponse;
import com.twilio.lookup.dto.LookupSource;
import com.twilio.lookup.dto.LookupStatus;
import com.twilio.lookup.exception.InvalidLookupException;
import com.twilio.lookup.exception.PermanentProviderException;
import com.twilio.lookup.exception.TransientProviderException;
import com.twilio.lookup.model.EmailLookup;
import com.twilio.lookup.provider.email.EmailLookupProvider;
import com.twilio.lookup.provider.email.EmailProviderMapper;
import com.twilio.lookup.provider.email.ProviderEmailResponse;
import com.twilio.lookup.repository.EmailLookupRepository;
import com.twilio.lookup.util.EmailNormalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailLookupServiceTests {

	private static final String EMAIL = "jane@example.com";
	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
	private static final EmailLookupRequest REQUEST = new EmailLookupRequest("  Jane@Example.com ");

	@Mock EmailLookupRepository repository;
	@Mock StalenessPolicy stalenessPolicy;
	@Mock EmailLookupProvider provider;

	private EmailLookupService service;

	@BeforeEach
	void setUp() {
		service = new EmailLookupService(new EmailNormalizer(), repository, stalenessPolicy, provider,
				new EmailProviderMapper(Clock.fixed(NOW, ZoneOffset.UTC)));
	}

	private static ProviderEmailResponse providerResponse(String verdict) {
		return new ProviderEmailResponse(EMAIL, verdict, 0.5, "jane", "example.com", null,
				new ProviderEmailResponse.Checks(
						new ProviderEmailResponse.Domain(true, true, false),
						new ProviderEmailResponse.LocalPart(false),
						new ProviderEmailResponse.Additional(false, false)),
				"127.0.0.1", "MOCK");
	}

	private EmailLookup stored() {
		EmailLookup entity = new EmailLookup();
		entity.setId(EMAIL);
		entity.setVerdict("Valid");
		entity.setCheckedAt(Instant.parse("2025-01-01T00:00:00Z"));
		return entity;
	}

	@Test
	void invalidInputNeverTouchesRepositoryOrProvider() {
		assertThatThrownBy(() -> service.lookup(new EmailLookupRequest("a@b@c")))
				.isInstanceOf(InvalidLookupException.class);
		verifyNoInteractions(repository, provider, stalenessPolicy);
	}

	@Test
	void freshRecordIsServedFromDatabase() {
		EmailLookup entity = stored();
		when(repository.findById(EMAIL)).thenReturn(Optional.of(entity));
		when(stalenessPolicy.isStale(entity.getCheckedAt())).thenReturn(false);

		EmailLookupResponse response = service.lookup(REQUEST);

		assertThat(response.source()).isEqualTo(LookupSource.DATABASE);
		assertThat(response.stale()).isFalse();
		verifyNoInteractions(provider);
	}

	@Test
	void missCallsProviderAndSaves() {
		when(repository.findById(EMAIL)).thenReturn(Optional.empty());
		when(provider.lookup(EMAIL)).thenReturn(providerResponse("Valid"));
		when(repository.save(any(EmailLookup.class))).thenAnswer(inv -> inv.getArgument(0));

		EmailLookupResponse response = service.lookup(REQUEST);

		assertThat(response.source()).isEqualTo(LookupSource.PROVIDER);
		assertThat(response.normalizedEmail()).isEqualTo(EMAIL);
		assertThat(response.status()).isEqualTo(LookupStatus.VALID);
		assertThat(response.verdict()).isEqualTo("Valid");
		assertThat(response.score()).isEqualTo(0.5);
		assertThat(response.checks().hasMxOrARecord()).isTrue();
		verify(repository).save(any(EmailLookup.class));
	}

	@Test
	void riskyVerdictIsValidStatusAndInvalidVerdictIsInvalidStatus() {
		when(repository.findById(EMAIL)).thenReturn(Optional.empty());
		when(repository.save(any(EmailLookup.class))).thenAnswer(inv -> inv.getArgument(0));

		when(provider.lookup(EMAIL)).thenReturn(providerResponse("Risky"));
		assertThat(service.lookup(REQUEST).status()).isEqualTo(LookupStatus.VALID);

		when(provider.lookup(EMAIL)).thenReturn(providerResponse("Invalid"));
		assertThat(service.lookup(REQUEST).status()).isEqualTo(LookupStatus.INVALID);
	}

	@Test
	void staleRecordIsRefreshed() {
		EmailLookup entity = stored();
		when(repository.findById(EMAIL)).thenReturn(Optional.of(entity));
		when(stalenessPolicy.isStale(any())).thenReturn(true);
		when(provider.lookup(EMAIL)).thenReturn(providerResponse("Valid"));
		when(repository.save(any(EmailLookup.class))).thenAnswer(inv -> inv.getArgument(0));

		EmailLookupResponse response = service.lookup(REQUEST);

		assertThat(response.source()).isEqualTo(LookupSource.PROVIDER);
		verify(repository).save(entity);
	}

	@Test
	void providerFailureWithStaleRecordReturnsStale() {
		when(repository.findById(EMAIL)).thenReturn(Optional.of(stored()));
		when(stalenessPolicy.isStale(any())).thenReturn(true);
		when(provider.lookup(EMAIL)).thenThrow(new TransientProviderException("down"));

		EmailLookupResponse response = service.lookup(REQUEST);

		assertThat(response.source()).isEqualTo(LookupSource.DATABASE);
		assertThat(response.stale()).isTrue();
		verify(repository, never()).save(any());
	}

	@Test
	void providerFailureWithoutRecordRethrowsAndSavesNothing() {
		when(repository.findById(EMAIL)).thenReturn(Optional.empty());
		when(provider.lookup(EMAIL)).thenThrow(new PermanentProviderException("rejected"));

		assertThatThrownBy(() -> service.lookup(REQUEST)).isInstanceOf(PermanentProviderException.class);
		verify(repository, never()).save(any());
	}
}
