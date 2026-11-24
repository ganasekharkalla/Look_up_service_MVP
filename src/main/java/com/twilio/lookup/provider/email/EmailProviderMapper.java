package com.twilio.lookup.provider.email;

import com.twilio.lookup.model.EmailLookup;
import com.twilio.lookup.model.EmailResult;
import java.time.Clock;
import org.springframework.stereotype.Component;

/** Turns a {@link ProviderEmailResponse} into the stored {@link EmailLookup}. The provider's ip address and source are not stored. */
@Component
public class EmailProviderMapper {

	private final Clock clock;

	public EmailProviderMapper(Clock clock) {
		this.clock = clock;
	}

	/**
	 * @param normalizedEmail normalized address, used as the id
	 * @param response        provider answer
	 * @param existingOrNull  the stored record to update in place (keeps {@code createdAt} and {@code version}), or null
	 */
	public EmailLookup toEntity(String normalizedEmail, ProviderEmailResponse response, EmailLookup existingOrNull) {
		EmailLookup entity = existingOrNull != null ? existingOrNull : new EmailLookup();
		entity.setId(normalizedEmail);
		entity.setChannel("email");
		entity.setSource("email-proxy");
		entity.setCheckedAt(clock.instant());
		entity.setVerdict(response.verdict());
		entity.setScore(response.score());

		EmailResult result = new EmailResult();
		result.setLocal(response.local());
		result.setHost(response.host());
		result.setSuggestion(response.suggestion());
		ProviderEmailResponse.Checks checks = response.checks();
		if (checks != null) {
			EmailResult.Checks stored = new EmailResult.Checks();
			if (checks.domain() != null) {
				stored.getDomain().setHasValidAddressSyntax(checks.domain().hasValidAddressSyntax());
				stored.getDomain().setHasMxOrARecord(checks.domain().hasMxOrARecord());
				stored.getDomain().setSuspectedDisposableAddress(checks.domain().suspectedDisposableAddress());
			}
			if (checks.localPart() != null) {
				stored.getLocalPart().setSuspectedRoleAddress(checks.localPart().suspectedRoleAddress());
			}
			if (checks.additional() != null) {
				stored.getAdditional().setHasKnownBounces(checks.additional().hasKnownBounces());
				stored.getAdditional().setHasSuspectedBounces(checks.additional().hasSuspectedBounces());
			}
			result.setChecks(stored);
		}
		entity.setResult(result);
		return entity;
	}
}
