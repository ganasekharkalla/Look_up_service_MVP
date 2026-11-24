package com.twilio.lookup.service;

import com.twilio.lookup.dto.email.EmailLookupRequest;
import com.twilio.lookup.dto.email.EmailLookupResponse;
import com.twilio.lookup.dto.LookupSource;
import com.twilio.lookup.dto.LookupStatus;
import com.twilio.lookup.exception.InvalidLookupException;
import com.twilio.lookup.exception.ProviderException;
import com.twilio.lookup.model.EmailLookup;
import com.twilio.lookup.model.EmailResult;
import com.twilio.lookup.provider.email.EmailLookupProvider;
import com.twilio.lookup.provider.email.EmailProviderMapper;
import com.twilio.lookup.provider.email.ProviderEmailResponse;
import com.twilio.lookup.repository.EmailLookupRepository;
import com.twilio.lookup.util.EmailNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Email lookup flow, identical in shape to {@link PhoneLookupService}. {@code Valid} and {@code Risky} verdicts
 * give status VALID, {@code Invalid} gives INVALID.
 */
@Service
public class EmailLookupService {

	private static final Logger log = LoggerFactory.getLogger(EmailLookupService.class);

	private final EmailNormalizer normalizer;
	private final EmailLookupRepository repository;
	private final StalenessPolicy stalenessPolicy;
	private final EmailLookupProvider provider;
	private final EmailProviderMapper mapper;

	public EmailLookupService(EmailNormalizer normalizer, EmailLookupRepository repository,
			StalenessPolicy stalenessPolicy, EmailLookupProvider provider, EmailProviderMapper mapper) {
		this.normalizer = normalizer;
		this.repository = repository;
		this.stalenessPolicy = stalenessPolicy;
		this.provider = provider;
		this.mapper = mapper;
	}

	public EmailLookupResponse lookup(EmailLookupRequest request) {
		String email = normalizer.normalize(request.email())
				.orElseThrow(() -> new InvalidLookupException("Invalid email address"));

		EmailLookup existing = repository.findById(email).orElse(null);
		if (existing != null && !stalenessPolicy.isStale(existing.getCheckedAt())) {
			return toResponse(existing, LookupSource.DATABASE, false);
		}
		try {
			ProviderEmailResponse response = provider.lookup(email);
			EmailLookup saved = repository.save(mapper.toEntity(email, response, existing));
			return toResponse(saved, LookupSource.PROVIDER, false);
		}
		catch (ProviderException ex) {
			if (existing == null) {
				throw ex;
			}
			log.warn("Provider failed for {}, returning stale record: {}", email, ex.getMessage());
			return toResponse(existing, LookupSource.DATABASE, true);
		}
	}

	private EmailLookupResponse toResponse(EmailLookup entity, LookupSource source, boolean stale) {
		EmailResult result = entity.getResult() != null ? entity.getResult() : new EmailResult();
		EmailResult.Checks checks = result.getChecks() != null ? result.getChecks() : new EmailResult.Checks();
		LookupStatus status = "Invalid".equalsIgnoreCase(entity.getVerdict()) ? LookupStatus.INVALID : LookupStatus.VALID;
		return new EmailLookupResponse(
				entity.getId(),
				status,
				source,
				entity.getVerdict(),
				entity.getScore(),
				result.getLocal(),
				result.getHost(),
				result.getSuggestion(),
				new EmailLookupResponse.Checks(
						checks.getDomain().isHasValidAddressSyntax(),
						checks.getDomain().isHasMxOrARecord(),
						checks.getDomain().isSuspectedDisposableAddress(),
						checks.getLocalPart().isSuspectedRoleAddress(),
						checks.getAdditional().isHasKnownBounces(),
						checks.getAdditional().isHasSuspectedBounces()),
				stale,
				entity.getCheckedAt());
	}
}
