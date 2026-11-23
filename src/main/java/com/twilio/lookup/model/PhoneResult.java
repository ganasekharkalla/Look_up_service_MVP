package com.twilio.lookup.model;

import java.util.ArrayList;
import java.util.List;

/** Provider payload for a phone lookup, stored nested inside {@link PhoneLookup}. */
public class PhoneResult {

	private String callingCountryCode;
	private String countryCode;
	private String nationalFormat;
	private List<String> validationErrors = new ArrayList<>();
	private LineTypeIntelligence lineTypeIntelligence;

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
}
