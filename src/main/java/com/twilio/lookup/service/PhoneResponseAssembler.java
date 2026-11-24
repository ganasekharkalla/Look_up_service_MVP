package com.twilio.lookup.service;

import com.twilio.lookup.dto.LookupSource;
import com.twilio.lookup.dto.LookupStatus;
import com.twilio.lookup.dto.phone.PhoneLookupResponse;
import com.twilio.lookup.model.PhoneLookup;
import com.twilio.lookup.model.PhoneResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Builds the client-facing {@link PhoneLookupResponse} from a stored {@link PhoneLookup}. */
final class PhoneResponseAssembler {

	private PhoneResponseAssembler() {
	}

	static PhoneLookupResponse toResponse(PhoneLookup entity, LookupSource source, boolean stale) {
		PhoneResult result = entity.getResult() != null ? entity.getResult() : new PhoneResult();
		PhoneResult.LineTypeIntelligence line = result.getLineTypeIntelligence();
		return new PhoneLookupResponse(
				entity.getId(),
				entity.isValid() ? LookupStatus.VALID : LookupStatus.INVALID,
				source,
				entity.isValid(),
				result.getCallingCountryCode(),
				result.getCountryCode(),
				result.getNationalFormat(),
				result.getValidationErrors() != null ? result.getValidationErrors() : List.of(),
				line != null ? line.getType() : null,
				line != null ? line.getCarrierName() : null,
				entity.isMobile(),
				entity.getPackages() != null ? entity.getPackages() : new ArrayList<>(),
				lineType(line),
				lineStatus(result.getLineStatus()),
				callerName(result.getCallerName()),
				simSwap(result.getSimSwap()),
				callForwarding(result.getCallForwarding()),
				reassignedNumber(result.getReassignedNumber()),
				smsPumpingRisk(result.getSmsPumpingRisk()),
				result.getPhoneNumberQualityScore() != null ? new LinkedHashMap<>(result.getPhoneNumberQualityScore()) : null,
				stale,
				entity.getCheckedAt());
	}

	private static PhoneLookupResponse.LineTypeIntelligence lineType(PhoneResult.LineTypeIntelligence e) {
		return e == null ? null : new PhoneLookupResponse.LineTypeIntelligence(e.getType(), e.getCarrierName(),
				e.getMobileCountryCode(), e.getMobileNetworkCode(), e.getErrorCode());
	}

	private static PhoneLookupResponse.LineStatus lineStatus(PhoneResult.LineStatus e) {
		return e == null ? null : new PhoneLookupResponse.LineStatus(e.getStatus(), e.getErrorCode());
	}

	private static PhoneLookupResponse.CallerName callerName(PhoneResult.CallerName e) {
		return e == null ? null : new PhoneLookupResponse.CallerName(e.getCallerName(), e.getCallerType(), e.getErrorCode());
	}

	private static PhoneLookupResponse.SimSwap simSwap(PhoneResult.SimSwap e) {
		if (e == null) return null;
		PhoneLookupResponse.LastSimSwap last = e.getLastSimSwap() == null ? null
				: new PhoneLookupResponse.LastSimSwap(e.getLastSimSwap().getLastSimSwapDate(),
						e.getLastSimSwap().getSwappedPeriod(), e.getLastSimSwap().getSwappedInPeriod());
		return new PhoneLookupResponse.SimSwap(last, e.getCarrierName(), e.getMobileCountryCode(),
				e.getMobileNetworkCode(), e.getErrorCode());
	}

	private static PhoneLookupResponse.CallForwarding callForwarding(PhoneResult.CallForwarding e) {
		return e == null ? null : new PhoneLookupResponse.CallForwarding(e.getCallForwardingEnabled(), e.getErrorCode());
	}

	private static PhoneLookupResponse.ReassignedNumber reassignedNumber(PhoneResult.ReassignedNumber e) {
		return e == null ? null : new PhoneLookupResponse.ReassignedNumber(e.getLastVerifiedDate(),
				e.getIsNumberReassigned(), e.getErrorCode());
	}

	private static PhoneLookupResponse.SmsPumpingRisk smsPumpingRisk(PhoneResult.SmsPumpingRisk e) {
		return e == null ? null : new PhoneLookupResponse.SmsPumpingRisk(e.getCarrierRiskCategory(), e.getNumberBlocked(),
				e.getNumberBlockedDate(), e.getNumberBlockedLast3Months(), e.getSmsPumpingRiskScore(), e.getErrorCode());
	}
}
