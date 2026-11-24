package com.twilio.lookup.provider.phone;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Twilio Lookup v2 shaped response, in camelCase. Each data package is null unless it was requested from the provider
 * (or is not available for the number). {@code url} is returned by Twilio but never stored.
 */
public record ProviderPhoneResponse(
		String callingCountryCode,
		String countryCode,
		String phoneNumber,
		String nationalFormat,
		boolean valid,
		List<String> validationErrors,
		LineTypeIntelligence lineTypeIntelligence,
		LineStatus lineStatus,
		CallerName callerName,
		SimSwap simSwap,
		CallForwarding callForwarding,
		ReassignedNumber reassignedNumber,
		SmsPumpingRisk smsPumpingRisk,
		Map<String, Object> phoneNumberQualityScore,
		String url) {

	/** Basic lookup with only the line type package (or none). */
	public ProviderPhoneResponse(String callingCountryCode, String countryCode, String phoneNumber, String nationalFormat,
			boolean valid, List<String> validationErrors, LineTypeIntelligence lineTypeIntelligence, String url) {
		this(callingCountryCode, countryCode, phoneNumber, nationalFormat, valid, validationErrors, lineTypeIntelligence,
				null, null, null, null, null, null, null, url);
	}

	/** Line type package. */
	public record LineTypeIntelligence(String type, String carrierName, String mobileCountryCode,
			String mobileNetworkCode, String errorCode) {
	}

	/** Line status package: active, inactive, unreachable or unknown. */
	public record LineStatus(String status, Integer errorCode) {
	}

	/** Caller name (CNAM) package, US carriers only. */
	public record CallerName(String callerName, String callerType, Integer errorCode) {
	}

	/** SIM swap package. */
	public record SimSwap(LastSimSwap lastSimSwap, String carrierName, String mobileCountryCode,
			String mobileNetworkCode, Integer errorCode) {
	}

	/** Details of the most recent SIM swap. */
	public record LastSimSwap(Instant lastSimSwapDate, String swappedPeriod, Boolean swappedInPeriod) {
	}

	/** Call forwarding package, major UK carriers only. */
	public record CallForwarding(Boolean callForwardingEnabled, Integer errorCode) {
	}

	/** Reassigned number package, US only. Twilio returns all three values as strings. */
	public record ReassignedNumber(String lastVerifiedDate, String isNumberReassigned, String errorCode) {
	}

	/** SMS pumping risk package. */
	public record SmsPumpingRisk(String carrierRiskCategory, Boolean numberBlocked, Instant numberBlockedDate,
			Boolean numberBlockedLast3Months, Integer smsPumpingRiskScore, Integer errorCode) {
	}
}
