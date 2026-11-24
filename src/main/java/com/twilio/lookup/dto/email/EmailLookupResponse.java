package com.twilio.lookup.dto.email;

import com.twilio.lookup.dto.LookupSource;
import com.twilio.lookup.dto.LookupStatus;
import java.time.Instant;

/** Result of an email lookup. {@code Valid} and {@code Risky} verdicts both have status VALID. */
public record EmailLookupResponse(
		String normalizedEmail,
		LookupStatus status,
		LookupSource source,
		String verdict,
		double score,
		String local,
		String host,
		String suggestion,
		Checks checks,
		boolean stale,
		Instant checkedAt) {

	/** The six provider checks, flattened. */
	public record Checks(
			boolean hasValidAddressSyntax,
			boolean hasMxOrARecord,
			boolean suspectedDisposableAddress,
			boolean suspectedRoleAddress,
			boolean hasKnownBounces,
			boolean hasSuspectedBounces) {
	}
}
