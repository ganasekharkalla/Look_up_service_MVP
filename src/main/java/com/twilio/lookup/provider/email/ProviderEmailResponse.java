package com.twilio.lookup.provider.email;

/** SendGrid email validation shaped response, in camelCase. {@code verdict} is {@code Valid}, {@code Risky} or {@code Invalid}. */
public record ProviderEmailResponse(
		String email,
		String verdict,
		double score,
		String local,
		String host,
		String suggestion,
		Checks checks,
		String ipAddress,
		String source) {

	/** Checks grouped by the part of the address they apply to. */
	public record Checks(Domain domain, LocalPart localPart, Additional additional) {
	}

	/** Checks on the domain. */
	public record Domain(boolean hasValidAddressSyntax, boolean hasMxOrARecord, boolean suspectedDisposableAddress) {
	}

	/** Checks on the part before the @. */
	public record LocalPart(boolean suspectedRoleAddress) {
	}

	/** Bounce history. */
	public record Additional(boolean hasKnownBounces, boolean hasSuspectedBounces) {
	}
}
