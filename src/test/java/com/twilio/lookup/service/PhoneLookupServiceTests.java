package com.twilio.lookup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.twilio.lookup.dto.LookupSource;
import com.twilio.lookup.dto.LookupStatus;
import com.twilio.lookup.dto.phone.PhoneLookupRequest;
import com.twilio.lookup.dto.phone.PhoneLookupResponse;
import com.twilio.lookup.exception.InvalidLookupException;
import com.twilio.lookup.exception.PermanentProviderException;
import com.twilio.lookup.exception.TransientProviderException;
import com.twilio.lookup.model.PhoneLookup;
import com.twilio.lookup.provider.phone.PhoneLookupProvider;
import com.twilio.lookup.provider.phone.PhoneProviderMapper;
import com.twilio.lookup.provider.phone.ProviderPhoneResponse;
import com.twilio.lookup.repository.PhoneLookupRepository;
import com.twilio.lookup.util.PhoneNormalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PhoneLookupServiceTests {

	private static final String NUMBER = "+14159929960";
	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
	private static final PhoneLookupRequest REQUEST = new PhoneLookupRequest("(415) 992-9960", "US");

	@Mock PhoneLookupRepository repository;
	@Mock StalenessPolicy stalenessPolicy;
	@Mock PhoneLookupProvider provider;

	private PhoneLookupService service;

	@BeforeEach
	void setUp() {
		service = new PhoneLookupService(new PhoneNormalizer(), repository, stalenessPolicy, provider,
				new PhoneProviderMapper(Clock.fixed(NOW, ZoneOffset.UTC)));
	}

	private static ProviderPhoneResponse providerResponse(boolean valid) {
		return new ProviderPhoneResponse("1", "US", NUMBER, "(415) 992-9960", valid,
				valid ? List.of() : List.of("INVALID_LENGTH"),
				valid ? new ProviderPhoneResponse.LineTypeIntelligence("mobile", "Mock Wireless", "310", "260", null) : null,
				null);
	}

	private PhoneLookup stored() {
		PhoneLookup entity = new PhoneLookup();
		entity.setId(NUMBER);
		entity.setValid(true);
		entity.setCheckedAt(Instant.parse("2025-01-01T00:00:00Z"));
		return entity;
	}

	@Test
	void invalidInputNeverTouchesRepositoryOrProvider() {
		assertThatThrownBy(() -> service.lookup(new PhoneLookupRequest("12", "US")))
				.isInstanceOf(InvalidLookupException.class);
		verifyNoInteractions(repository, provider, stalenessPolicy);
	}

	@Test
	void freshRecordIsServedFromDatabase() {
		PhoneLookup entity = stored();
		when(repository.findById(NUMBER)).thenReturn(Optional.of(entity));
		when(stalenessPolicy.isStale(entity.getCheckedAt())).thenReturn(false);

		PhoneLookupResponse response = service.lookup(REQUEST);

		assertThat(response.source()).isEqualTo(LookupSource.DATABASE);
		assertThat(response.stale()).isFalse();
		assertThat(response.status()).isEqualTo(LookupStatus.VALID);
		verifyNoInteractions(provider);
		verify(repository, never()).save(any());
	}

	@Test
	void missCallsProviderAndSaves() {
		when(repository.findById(NUMBER)).thenReturn(Optional.empty());
		when(provider.lookup(NUMBER)).thenReturn(providerResponse(true));
		when(repository.save(any(PhoneLookup.class))).thenAnswer(inv -> inv.getArgument(0));

		PhoneLookupResponse response = service.lookup(REQUEST);

		assertThat(response.source()).isEqualTo(LookupSource.PROVIDER);
		assertThat(response.stale()).isFalse();
		assertThat(response.normalizedNumber()).isEqualTo(NUMBER);
		assertThat(response.lineType()).isEqualTo("mobile");
		assertThat(response.carrier()).isEqualTo("Mock Wireless");
		assertThat(response.mobile()).isTrue();
		assertThat(response.packages()).containsExactly("line_type_intelligence");
		assertThat(response.checkedAt()).isEqualTo(NOW);
		verify(repository).save(any(PhoneLookup.class));
	}

	@Test
	void staleRecordIsRefreshedInPlace() {
		PhoneLookup entity = stored();
		when(repository.findById(NUMBER)).thenReturn(Optional.of(entity));
		when(stalenessPolicy.isStale(any())).thenReturn(true);
		when(provider.lookup(NUMBER)).thenReturn(providerResponse(true));
		when(repository.save(any(PhoneLookup.class))).thenAnswer(inv -> inv.getArgument(0));

		PhoneLookupResponse response = service.lookup(REQUEST);

		assertThat(response.source()).isEqualTo(LookupSource.PROVIDER);
		assertThat(response.checkedAt()).isEqualTo(NOW);
		verify(repository).save(entity);
	}

	@Test
	void providerFailureWithStaleRecordReturnsStale() {
		PhoneLookup entity = stored();
		when(repository.findById(NUMBER)).thenReturn(Optional.of(entity));
		when(stalenessPolicy.isStale(any())).thenReturn(true);
		when(provider.lookup(NUMBER)).thenThrow(new TransientProviderException("down"));

		PhoneLookupResponse response = service.lookup(REQUEST);

		assertThat(response.source()).isEqualTo(LookupSource.DATABASE);
		assertThat(response.stale()).isTrue();
		verify(repository, never()).save(any());
	}

	@Test
	void providerFailureWithoutRecordRethrowsAndSavesNothing() {
		when(repository.findById(NUMBER)).thenReturn(Optional.empty());
		when(provider.lookup(NUMBER)).thenThrow(new PermanentProviderException("rejected"));

		assertThatThrownBy(() -> service.lookup(REQUEST)).isInstanceOf(PermanentProviderException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void providerInvalidVerdictIsStoredAsInvalid() {
		when(repository.findById(NUMBER)).thenReturn(Optional.empty());
		when(provider.lookup(NUMBER)).thenReturn(providerResponse(false));
		when(repository.save(any(PhoneLookup.class))).thenAnswer(inv -> inv.getArgument(0));

		PhoneLookupResponse response = service.lookup(REQUEST);

		assertThat(response.status()).isEqualTo(LookupStatus.INVALID);
		assertThat(response.valid()).isFalse();
		assertThat(response.lineType()).isNull();
		assertThat(response.validationErrors()).containsExactly("INVALID_LENGTH");
		verify(repository).save(any(PhoneLookup.class));
	}

	@Test
	void concurrentRequestsForSameNumberCallProviderOnce() throws Exception {
		AtomicReference<PhoneLookup> store = new AtomicReference<>();
		when(repository.findById(NUMBER)).thenAnswer(inv -> Optional.ofNullable(store.get()));
		when(stalenessPolicy.isStale(NOW)).thenReturn(false);
		when(provider.lookup(NUMBER)).thenAnswer(inv -> {
			Thread.sleep(25);
			return providerResponse(true);
		});
		when(repository.save(any(PhoneLookup.class))).thenAnswer(inv -> {
			PhoneLookup saved = inv.getArgument(0);
			store.set(saved);
			return saved;
		});

		int threads = 8;
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);
		List<PhoneLookupResponse> responses = new ArrayList<>();

		ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			List<Future<PhoneLookupResponse>> futures = new ArrayList<>();
			for (int i = 0; i < threads; i++) {
				PhoneLookupRequest req = (i % 2 == 0)
						? REQUEST
						: new PhoneLookupRequest("+14159929960", null);
				futures.add(pool.submit(() -> {
					ready.countDown();
					assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
					return service.lookup(req);
				}));
			}
			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			for (Future<PhoneLookupResponse> future : futures) {
				responses.add(future.get(5, TimeUnit.SECONDS));
			}
		}
		finally {
			pool.shutdownNow();
		}

		verify(provider, times(1)).lookup(NUMBER);
		verify(repository, times(1)).save(any(PhoneLookup.class));
		assertThat(responses).filteredOn(r -> r.source() == LookupSource.PROVIDER).hasSize(1);
		assertThat(responses).filteredOn(r -> r.source() == LookupSource.DATABASE).hasSize(threads - 1);
	}
}
