package com.twilio.lookup.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provider payload for a phone lookup, stored nested inside {@link PhoneLookup}.
 *
 * <p>Holds the basic Lookup v2 fields plus one optional object per data package. A package object is
 * {@code null} unless that package was requested from the provider. Not stored on purpose: {@code phone_number}
 * (it is the document id), {@code url}, {@code identity_match} and {@code pre_fill}.
 *
 * <p>Error codes follow the types in the Twilio documentation, so most are {@code Integer} but
 * {@code reassigned_number.error_code} is a {@code String}. Package flags are nullable {@code Boolean} because a
 * package can return no value when it reports an error code.
 */
public class PhoneResult {

	private String callingCountryCode;
	private String countryCode;
	private String nationalFormat;
	private List<String> validationErrors = new ArrayList<>();

	private LineTypeIntelligence lineTypeIntelligence;
	private LineStatus lineStatus;
	private CallerName callerName;
	private SimSwap simSwap;
	private CallForwarding callForwarding;
	private ReassignedNumber reassignedNumber;
	private SmsPumpingRisk smsPumpingRisk;
	/** Twilio documents no fields for this package, so the payload is kept as returned. */
	private Map<String, Object> phoneNumberQualityScore;

	public PhoneResult() {
	}

	public String getCallingCountryCode() {
		return callingCountryCode;
	}

	public void setCallingCountryCode(String callingCountryCode) {
		this.callingCountryCode = callingCountryCode;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getNationalFormat() {
		return nationalFormat;
	}

	public void setNationalFormat(String nationalFormat) {
		this.nationalFormat = nationalFormat;
	}

	public List<String> getValidationErrors() {
		return validationErrors;
	}

	public void setValidationErrors(List<String> validationErrors) {
		this.validationErrors = validationErrors;
	}

	public LineTypeIntelligence getLineTypeIntelligence() {
		return lineTypeIntelligence;
	}

	public void setLineTypeIntelligence(LineTypeIntelligence lineTypeIntelligence) {
		this.lineTypeIntelligence = lineTypeIntelligence;
	}

	public LineStatus getLineStatus() {
		return lineStatus;
	}

	public void setLineStatus(LineStatus lineStatus) {
		this.lineStatus = lineStatus;
	}

	public CallerName getCallerName() {
		return callerName;
	}

	public void setCallerName(CallerName callerName) {
		this.callerName = callerName;
	}

	public SimSwap getSimSwap() {
		return simSwap;
	}

	public void setSimSwap(SimSwap simSwap) {
		this.simSwap = simSwap;
	}

	public CallForwarding getCallForwarding() {
		return callForwarding;
	}

	public void setCallForwarding(CallForwarding callForwarding) {
		this.callForwarding = callForwarding;
	}

	public ReassignedNumber getReassignedNumber() {
		return reassignedNumber;
	}

	public void setReassignedNumber(ReassignedNumber reassignedNumber) {
		this.reassignedNumber = reassignedNumber;
	}

	public SmsPumpingRisk getSmsPumpingRisk() {
		return smsPumpingRisk;
	}

	public void setSmsPumpingRisk(SmsPumpingRisk smsPumpingRisk) {
		this.smsPumpingRisk = smsPumpingRisk;
	}

	public Map<String, Object> getPhoneNumberQualityScore() {
		return phoneNumberQualityScore;
	}

	public void setPhoneNumberQualityScore(Map<String, Object> phoneNumberQualityScore) {
		this.phoneNumberQualityScore = phoneNumberQualityScore;
	}

	/** Line type package, present only when requested from the provider. */
	public static class LineTypeIntelligence {

		private String type;
		private String carrierName;
		private String mobileCountryCode;
		private String mobileNetworkCode;
		private String errorCode;

		public LineTypeIntelligence() {
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getCarrierName() {
			return carrierName;
		}

		public void setCarrierName(String carrierName) {
			this.carrierName = carrierName;
		}

		public String getMobileCountryCode() {
			return mobileCountryCode;
		}

		public void setMobileCountryCode(String mobileCountryCode) {
			this.mobileCountryCode = mobileCountryCode;
		}

		public String getMobileNetworkCode() {
			return mobileNetworkCode;
		}

		public void setMobileNetworkCode(String mobileNetworkCode) {
			this.mobileNetworkCode = mobileNetworkCode;
		}

		public String getErrorCode() {
			return errorCode;
		}

		public void setErrorCode(String errorCode) {
			this.errorCode = errorCode;
		}
	}

	/** Line status package: whether the line is active, inactive, unreachable or unknown. */
	public static class LineStatus {

		private String status;
		private Integer errorCode;

		public LineStatus() {
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public Integer getErrorCode() {
			return errorCode;
		}

		public void setErrorCode(Integer errorCode) {
			this.errorCode = errorCode;
		}
	}

	/** Caller name (CNAM) package, US carriers only. */
	public static class CallerName {

		private String callerName;
		private String callerType;
		private Integer errorCode;

		public CallerName() {
		}

		public String getCallerName() {
			return callerName;
		}

		public void setCallerName(String callerName) {
			this.callerName = callerName;
		}

		public String getCallerType() {
			return callerType;
		}

		public void setCallerType(String callerType) {
			this.callerType = callerType;
		}

		public Integer getErrorCode() {
			return errorCode;
		}

		public void setErrorCode(Integer errorCode) {
			this.errorCode = errorCode;
		}
	}

	/** SIM swap package: when the SIM for this number last changed. */
	public static class SimSwap {

		private LastSimSwap lastSimSwap;
		private String carrierName;
		private String mobileCountryCode;
		private String mobileNetworkCode;
		private Integer errorCode;

		public SimSwap() {
		}

		public LastSimSwap getLastSimSwap() {
			return lastSimSwap;
		}

		public void setLastSimSwap(LastSimSwap lastSimSwap) {
			this.lastSimSwap = lastSimSwap;
		}

		public String getCarrierName() {
			return carrierName;
		}

		public void setCarrierName(String carrierName) {
			this.carrierName = carrierName;
		}

		public String getMobileCountryCode() {
			return mobileCountryCode;
		}

		public void setMobileCountryCode(String mobileCountryCode) {
			this.mobileCountryCode = mobileCountryCode;
		}

		public String getMobileNetworkCode() {
			return mobileNetworkCode;
		}

		public void setMobileNetworkCode(String mobileNetworkCode) {
			this.mobileNetworkCode = mobileNetworkCode;
		}

		public Integer getErrorCode() {
			return errorCode;
		}

		public void setErrorCode(Integer errorCode) {
			this.errorCode = errorCode;
		}
	}

	/** Details of the most recent SIM swap, nested in {@link SimSwap}. */
	public static class LastSimSwap {

		private Instant lastSimSwapDate;
		private String swappedPeriod;
		private Boolean swappedInPeriod;

		public LastSimSwap() {
		}

		public Instant getLastSimSwapDate() {
			return lastSimSwapDate;
		}

		public void setLastSimSwapDate(Instant lastSimSwapDate) {
			this.lastSimSwapDate = lastSimSwapDate;
		}

		public String getSwappedPeriod() {
			return swappedPeriod;
		}

		public void setSwappedPeriod(String swappedPeriod) {
			this.swappedPeriod = swappedPeriod;
		}

		public Boolean getSwappedInPeriod() {
			return swappedInPeriod;
		}

		public void setSwappedInPeriod(Boolean swappedInPeriod) {
			this.swappedInPeriod = swappedInPeriod;
		}
	}

	/** Call forwarding package, major UK carriers only. */
	public static class CallForwarding {

		private Boolean callForwardingEnabled;
		private Integer errorCode;

		public CallForwarding() {
		}

		public Boolean getCallForwardingEnabled() {
			return callForwardingEnabled;
		}

		public void setCallForwardingEnabled(Boolean callForwardingEnabled) {
			this.callForwardingEnabled = callForwardingEnabled;
		}

		public Integer getErrorCode() {
			return errorCode;
		}

		public void setErrorCode(Integer errorCode) {
			this.errorCode = errorCode;
		}
	}

	/** Reassigned number package, US only. The provider returns all three values as strings. */
	public static class ReassignedNumber {

		private String lastVerifiedDate;
		private String isNumberReassigned;
		private String errorCode;

		public ReassignedNumber() {
		}

		public String getLastVerifiedDate() {
			return lastVerifiedDate;
		}

		public void setLastVerifiedDate(String lastVerifiedDate) {
			this.lastVerifiedDate = lastVerifiedDate;
		}

		public String getIsNumberReassigned() {
			return isNumberReassigned;
		}

		public void setIsNumberReassigned(String isNumberReassigned) {
			this.isNumberReassigned = isNumberReassigned;
		}

		public String getErrorCode() {
			return errorCode;
		}

		public void setErrorCode(String errorCode) {
			this.errorCode = errorCode;
		}
	}

	/** SMS pumping risk package. */
	public static class SmsPumpingRisk {

		private String carrierRiskCategory;
		private Boolean numberBlocked;
		private Instant numberBlockedDate;
		private Boolean numberBlockedLast3Months;
		private Integer smsPumpingRiskScore;
		private Integer errorCode;

		public SmsPumpingRisk() {
		}

		public String getCarrierRiskCategory() {
			return carrierRiskCategory;
		}

		public void setCarrierRiskCategory(String carrierRiskCategory) {
			this.carrierRiskCategory = carrierRiskCategory;
		}

		public Boolean getNumberBlocked() {
			return numberBlocked;
		}

		public void setNumberBlocked(Boolean numberBlocked) {
			this.numberBlocked = numberBlocked;
		}

		public Instant getNumberBlockedDate() {
			return numberBlockedDate;
		}

		public void setNumberBlockedDate(Instant numberBlockedDate) {
			this.numberBlockedDate = numberBlockedDate;
		}

		public Boolean getNumberBlockedLast3Months() {
			return numberBlockedLast3Months;
		}

		public void setNumberBlockedLast3Months(Boolean numberBlockedLast3Months) {
			this.numberBlockedLast3Months = numberBlockedLast3Months;
		}

		public Integer getSmsPumpingRiskScore() {
			return smsPumpingRiskScore;
		}

		public void setSmsPumpingRiskScore(Integer smsPumpingRiskScore) {
			this.smsPumpingRiskScore = smsPumpingRiskScore;
		}

		public Integer getErrorCode() {
			return errorCode;
		}

		public void setErrorCode(Integer errorCode) {
			this.errorCode = errorCode;
		}
	}
}
