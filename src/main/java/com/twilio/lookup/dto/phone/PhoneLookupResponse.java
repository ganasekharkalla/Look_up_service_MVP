package com.twilio.lookup.dto.phone;

import com.twilio.lookup.dto.LookupSource;
import com.twilio.lookup.dto.LookupStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Result of a phone lookup, shaped like Twilio Lookup v2. Each data package is a nested object that is null when it was
 * not returned. {@code lineType}, {@code carrier} and {@code mobile} are convenience copies of the line type package.
 */
public record PhoneLookupResponse(
		String normalizedNumber,
		LookupStatus status,
		LookupSource source,
		boolean valid,
		String callingCountryCode,
		String countryCode,
		String nationalFormat,
		List<String> validationErrors,
		String lineType,
		String carrier,
		boolean mobile,
		List<String> packages,
		LineTypeIntelligence lineTypeIntelligence,
		LineStatus lineStatus,
		CallerName callerName,
		SimSwap simSwap,
		CallForwarding callForwarding,
		ReassignedNumber reassignedNumber,
		SmsPumpingRisk smsPumpingRisk,
		Map<String, Object> phoneNumberQualityScore,
		boolean stale,
		Instant checkedAt) {

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

	/** Reassigned number package, US only. */
	public record ReassignedNumber(String lastVerifiedDate, String isNumberReassigned, String errorCode) {
	}

	/** SMS pumping risk package. */
	public record SmsPumpingRisk(String carrierRiskCategory, Boolean numberBlocked, Instant numberBlockedDate,
			Boolean numberBlockedLast3Months, Integer smsPumpingRiskScore, Integer errorCode) {
	}
}
