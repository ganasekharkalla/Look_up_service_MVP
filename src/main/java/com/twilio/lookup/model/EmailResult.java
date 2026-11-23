package com.twilio.lookup.model;

/** Provider payload for an email lookup, stored nested inside {@link EmailLookup}. */
public class EmailResult {

	private String local;
	private String host;
	private String suggestion;
	private Checks checks = new Checks();

	public EmailResult() {
	}

	public String getLocal() {
		return local;
	}

	public void setLocal(String local) {
		this.local = local;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getSuggestion() {
		return suggestion;
	}

	public void setSuggestion(String suggestion) {
		this.suggestion = suggestion;
	}

	public Checks getChecks() {
		return checks;
	}

	public void setChecks(Checks checks) {
		this.checks = checks;
	}

	/** Groups the provider's checks by the part of the address they apply to. */
	public static class Checks {

		private Domain domain = new Domain();
		private LocalPart localPart = new LocalPart();
		private Additional additional = new Additional();

		public Checks() {
		}

		public Domain getDomain() {
			return domain;
		}

		public void setDomain(Domain domain) {
			this.domain = domain;
		}

		public LocalPart getLocalPart() {
			return localPart;
		}

		public void setLocalPart(LocalPart localPart) {
			this.localPart = localPart;
		}

		public Additional getAdditional() {
			return additional;
		}

		public void setAdditional(Additional additional) {
			this.additional = additional;
		}
	}

	/** Checks on the domain part of the address. */
	public static class Domain {

		private boolean hasValidAddressSyntax;
		private boolean hasMxOrARecord;
		private boolean suspectedDisposableAddress;

		public Domain() {
		}

		public boolean isHasValidAddressSyntax() {
			return hasValidAddressSyntax;
		}

		public void setHasValidAddressSyntax(boolean hasValidAddressSyntax) {
			this.hasValidAddressSyntax = hasValidAddressSyntax;
		}

		public boolean isHasMxOrARecord() {
			return hasMxOrARecord;
		}

		public void setHasMxOrARecord(boolean hasMxOrARecord) {
			this.hasMxOrARecord = hasMxOrARecord;
		}

		public boolean isSuspectedDisposableAddress() {
			return suspectedDisposableAddress;
		}

		public void setSuspectedDisposableAddress(boolean suspectedDisposableAddress) {
			this.suspectedDisposableAddress = suspectedDisposableAddress;
		}
	}

	/** Checks on the local part of the address, before the @. */
	public static class LocalPart {

		private boolean suspectedRoleAddress;

		public LocalPart() {
		}

		public boolean isSuspectedRoleAddress() {
			return suspectedRoleAddress;
		}

		public void setSuspectedRoleAddress(boolean suspectedRoleAddress) {
			this.suspectedRoleAddress = suspectedRoleAddress;
		}
	}

	/** Bounce history from the provider. */
	public static class Additional {

		private boolean hasKnownBounces;
		private boolean hasSuspectedBounces;

		public Additional() {
		}

		public boolean isHasKnownBounces() {
			return hasKnownBounces;
		}

		public void setHasKnownBounces(boolean hasKnownBounces) {
			this.hasKnownBounces = hasKnownBounces;
		}

		public boolean isHasSuspectedBounces() {
			return hasSuspectedBounces;
		}

		public void setHasSuspectedBounces(boolean hasSuspectedBounces) {
			this.hasSuspectedBounces = hasSuspectedBounces;
		}
	}
}
