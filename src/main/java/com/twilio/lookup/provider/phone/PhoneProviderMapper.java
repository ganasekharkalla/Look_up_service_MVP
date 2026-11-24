package com.twilio.lookup.provider.phone;

import com.twilio.lookup.model.PhoneLookup;
import com.twilio.lookup.model.PhoneResult;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Component;

/** Turns a {@link ProviderPhoneResponse} into the stored {@link PhoneLookup}. The provider's {@code url} is not stored. */
@Component
public class PhoneProviderMapper {

	private final Clock clock;

	public PhoneProviderMapper(Clock clock) {
		this.clock = clock;
	}

	/**
	 * @param normalizedNumber E.164 number, used as the id
	 * @param response         provider answer
	 * @param existingOrNull   the stored record to update in place (keeps {@code createdAt} and {@code version}), or null
	 */
	public PhoneLookup toEntity(String normalizedNumber, ProviderPhoneResponse response, PhoneLookup existingOrNull) {
		PhoneLookup entity = existingOrNull != null ? existingOrNull : new PhoneLookup();
		ProviderPhoneResponse.LineTypeIntelligence line = response.lineTypeIntelligence();

		PhoneResult result = new PhoneResult();
		result.setCallingCountryCode(response.callingCountryCode());
		result.setCountryCode(response.countryCode());
		result.setNationalFormat(response.nationalFormat());
		result.setValidationErrors(response.validationErrors() != null ? new ArrayList<>(response.validationErrors()) : new ArrayList<>());
		result.setLineTypeIntelligence(lineType(line));
		result.setLineStatus(lineStatus(response.lineStatus()));
		result.setCallerName(callerName(response.callerName()));
		result.setSimSwap(simSwap(response.simSwap()));
		result.setCallForwarding(callForwarding(response.callForwarding()));
		result.setReassignedNumber(reassignedNumber(response.reassignedNumber()));
		result.setSmsPumpingRisk(smsPumpingRisk(response.smsPumpingRisk()));
		result.setPhoneNumberQualityScore(response.phoneNumberQualityScore() != null
				? new LinkedHashMap<>(response.phoneNumberQualityScore()) : null);

		entity.setId(normalizedNumber);
		entity.setChannel("phone");
		entity.setSource("phone-proxy");
		entity.setCheckedAt(clock.instant());
		entity.setValid(response.valid());
		entity.setMobile(line != null && "mobile".equals(line.type()));
		entity.setPackages(packages(result));
		entity.setResult(result);
		return entity;
	}

	/** Names of the packages that came back, in Twilio's snake_case. Empty when only basic data was returned. */
	private static List<String> packages(PhoneResult result) {
		List<String> packages = new ArrayList<>();
		if (result.getLineTypeIntelligence() != null) packages.add("line_type_intelligence");
		if (result.getLineStatus() != null) packages.add("line_status");
		if (result.getCallerName() != null) packages.add("caller_name");
		if (result.getSimSwap() != null) packages.add("sim_swap");
		if (result.getCallForwarding() != null) packages.add("call_forwarding");
		if (result.getReassignedNumber() != null) packages.add("reassigned_number");
		if (result.getSmsPumpingRisk() != null) packages.add("sms_pumping_risk");
		if (result.getPhoneNumberQualityScore() != null) packages.add("phone_number_quality_score");
		return packages;
	}

	private static PhoneResult.LineTypeIntelligence lineType(ProviderPhoneResponse.LineTypeIntelligence p) {
		if (p == null) return null;
		PhoneResult.LineTypeIntelligence e = new PhoneResult.LineTypeIntelligence();
		e.setType(p.type());
		e.setCarrierName(p.carrierName());
		e.setMobileCountryCode(p.mobileCountryCode());
		e.setMobileNetworkCode(p.mobileNetworkCode());
		e.setErrorCode(p.errorCode());
		return e;
	}

	private static PhoneResult.LineStatus lineStatus(ProviderPhoneResponse.LineStatus p) {
		if (p == null) return null;
		PhoneResult.LineStatus e = new PhoneResult.LineStatus();
		e.setStatus(p.status());
		e.setErrorCode(p.errorCode());
		return e;
	}

	private static PhoneResult.CallerName callerName(ProviderPhoneResponse.CallerName p) {
		if (p == null) return null;
		PhoneResult.CallerName e = new PhoneResult.CallerName();
		e.setCallerName(p.callerName());
		e.setCallerType(p.callerType());
		e.setErrorCode(p.errorCode());
		return e;
	}

	private static PhoneResult.SimSwap simSwap(ProviderPhoneResponse.SimSwap p) {
		if (p == null) return null;
		PhoneResult.SimSwap e = new PhoneResult.SimSwap();
		if (p.lastSimSwap() != null) {
			PhoneResult.LastSimSwap last = new PhoneResult.LastSimSwap();
			last.setLastSimSwapDate(p.lastSimSwap().lastSimSwapDate());
			last.setSwappedPeriod(p.lastSimSwap().swappedPeriod());
			last.setSwappedInPeriod(p.lastSimSwap().swappedInPeriod());
			e.setLastSimSwap(last);
		}
		e.setCarrierName(p.carrierName());
		e.setMobileCountryCode(p.mobileCountryCode());
		e.setMobileNetworkCode(p.mobileNetworkCode());
		e.setErrorCode(p.errorCode());
		return e;
	}

	private static PhoneResult.CallForwarding callForwarding(ProviderPhoneResponse.CallForwarding p) {
		if (p == null) return null;
		PhoneResult.CallForwarding e = new PhoneResult.CallForwarding();
		e.setCallForwardingEnabled(p.callForwardingEnabled());
		e.setErrorCode(p.errorCode());
		return e;
	}

	private static PhoneResult.ReassignedNumber reassignedNumber(ProviderPhoneResponse.ReassignedNumber p) {
		if (p == null) return null;
		PhoneResult.ReassignedNumber e = new PhoneResult.ReassignedNumber();
		e.setLastVerifiedDate(p.lastVerifiedDate());
		e.setIsNumberReassigned(p.isNumberReassigned());
		e.setErrorCode(p.errorCode());
		return e;
	}

	private static PhoneResult.SmsPumpingRisk smsPumpingRisk(ProviderPhoneResponse.SmsPumpingRisk p) {
		if (p == null) return null;
		PhoneResult.SmsPumpingRisk e = new PhoneResult.SmsPumpingRisk();
		e.setCarrierRiskCategory(p.carrierRiskCategory());
		e.setNumberBlocked(p.numberBlocked());
		e.setNumberBlockedDate(p.numberBlockedDate());
		e.setNumberBlockedLast3Months(p.numberBlockedLast3Months());
		e.setSmsPumpingRiskScore(p.smsPumpingRiskScore());
		e.setErrorCode(p.errorCode());
		return e;
	}
}
