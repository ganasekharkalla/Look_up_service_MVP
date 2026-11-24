package com.twilio.lookup.service;

import com.twilio.lookup.dto.LookupSource;
import com.twilio.lookup.dto.phone.PhoneLookupRequest;
import com.twilio.lookup.dto.phone.PhoneLookupResponse;
import com.twilio.lookup.exception.InvalidLookupException;
import com.twilio.lookup.exception.ProviderException;
import com.twilio.lookup.model.PhoneLookup;
import com.twilio.lookup.provider.phone.PhoneLookupProvider;
import com.twilio.lookup.provider.phone.PhoneProviderMapper;
import com.twilio.lookup.provider.phone.ProviderPhoneResponse;
import com.twilio.lookup.repository.PhoneLookupRepository;
import com.twilio.lookup.util.PhoneNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Phone lookup flow: normalize, read the database, fall back to the provider, save provider results only.
 * A provider failure with an older record returns that record as stale; without one the failure propagates.
 */
@Service
public class PhoneLookupService {

	private static final Logger log = LoggerFactory.getLogger(PhoneLookupService.class);

	private final PhoneNormalizer normalizer;
	private final PhoneLookupRepository repository;
	private final StalenessPolicy stalenessPolicy;
	private final PhoneLookupProvider provider;
	private final PhoneProviderMapper mapper;
	private final KeyLock keyLock = new KeyLock();

	public PhoneLookupService(PhoneNormalizer normalizer, PhoneLookupRepository repository,
			StalenessPolicy stalenessPolicy, PhoneLookupProvider provider, PhoneProviderMapper mapper) {
		this.normalizer = normalizer;
		this.repository = repository;
		this.stalenessPolicy = stalenessPolicy;
		this.provider = provider;
		this.mapper = mapper;
	}

	public PhoneLookupResponse lookup(PhoneLookupRequest request) {
		String number = normalizer.normalize(request.phoneNumber(), request.countryCode())
				.orElseThrow(() -> new InvalidLookupException("Invalid phone number"));

		return keyLock.withLock(number, () -> lookupNormalized(number));
	}

	private PhoneLookupResponse lookupNormalized(String number) {
		PhoneLookup existing = repository.findById(number).orElse(null);
		if (existing != null && !stalenessPolicy.isStale(existing.getCheckedAt())) {
			return PhoneResponseAssembler.toResponse(existing, LookupSource.DATABASE, false);
		}
		try {
			ProviderPhoneResponse response = provider.lookup(number);
			PhoneLookup saved = repository.save(mapper.toEntity(number, response, existing));
			return PhoneResponseAssembler.toResponse(saved, LookupSource.PROVIDER, false);
		}
		catch (ProviderException ex) {
			if (existing == null) {
				throw ex;
			}
			log.warn("Provider failed for {}, returning stale record: {}", number, ex.getMessage());
			return PhoneResponseAssembler.toResponse(existing, LookupSource.DATABASE, true);
		}
	}
}
