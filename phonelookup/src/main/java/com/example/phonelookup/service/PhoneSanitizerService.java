package com.example.phonelookup.service;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.phonelookup.dto.NormalizedPhone;
import com.example.phonelookup.exception.InvalidPhoneException;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

@Service
public class PhoneSanitizerService {

    static final String SOURCE_NUMBER = "NUMBER";
    static final String SOURCE_REGION = "REGION";

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();
    static final String MISSING_COUNTRY_CODE =
            "Provide a country code. The number has no country code and no region, so the lookup service was not called.";

    public boolean hasExplicitCountryCode(String rawPhone) {
        return StringUtils.hasText(rawPhone) && cleanRawPhone(rawPhone).startsWith("+");
    }

    public boolean isRegionMissing(String region) {
        if (!StringUtils.hasText(region)) {
            return true;
        }
        String trimmed = region.trim();
        return trimmed.equalsIgnoreCase("null") || trimmed.equalsIgnoreCase("none");
    }

    public NormalizedPhone sanitizeAndFormatE164(String rawPhone, String defaultRegion) {
        if (!StringUtils.hasText(rawPhone)) {
            throw new InvalidPhoneException("Phone number must not be null or blank");
        }

        String cleaned = cleanRawPhone(rawPhone);
        boolean numberHasCountryCode = cleaned.startsWith("+");
        String requestedRegion = requestedRegion(defaultRegion, numberHasCountryCode);

        try {
            PhoneNumber parsed = PHONE_UTIL.parse(cleaned, requestedRegion == null ? "ZZ" : requestedRegion);
            if (!PHONE_UTIL.isValidNumber(parsed)) {
                throw new InvalidPhoneException(invalidMessage(rawPhone, requestedRegion, numberHasCountryCode));
            }
            String appliedRegion = PHONE_UTIL.getRegionCodeForNumber(parsed);
            return NormalizedPhone.builder()
                    .phoneE164(PHONE_UTIL.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164))
                    .requestedRegion(requestedRegion)
                    .appliedRegion(appliedRegion)
                    .regionSource(numberHasCountryCode ? SOURCE_NUMBER : SOURCE_REGION)
                    .build();
        } catch (NumberParseException ex) {
            throw new InvalidPhoneException(
                    "Unparseable phone number: " + rawPhone + ". "
                            + ruleSentence(requestedRegion, numberHasCountryCode),
                    ex);
        }
    }

    private String requestedRegion(String defaultRegion, boolean numberHasCountryCode) {
        if (isRegionMissing(defaultRegion)) {
            if (numberHasCountryCode) {
                return null;
            }
            throw new InvalidPhoneException(MISSING_COUNTRY_CODE);
        }
        String region = defaultRegion.trim().toUpperCase(Locale.ROOT);
        if (!region.matches("[A-Z]{2}")) {
            throw new InvalidPhoneException(
                    "Region must be a 2-letter ISO code such as IN or US. Received: " + defaultRegion);
        }
        return region;
    }

    private String invalidMessage(String rawPhone, String requestedRegion, boolean numberHasCountryCode) {
        return "Invalid phone number: " + rawPhone + ". " + ruleSentence(requestedRegion, numberHasCountryCode);
    }

    private String ruleSentence(String requestedRegion, boolean numberHasCountryCode) {
        if (numberHasCountryCode) {
            return "Rejected by local validation. The country code in the number was used"
                    + (requestedRegion == null ? "." : ", and region " + requestedRegion + " was not used.");
        }
        return "Rejected by local validation for region " + requestedRegion
                + " before any lookup service call. The number has no country code, so the region decides the country.";
    }

    private String cleanRawPhone(String rawPhone) {
        String cleaned = rawPhone.trim();
        cleaned = cleaned.replaceAll("(?i)\\s*(ext\\.?|extension|x)\\s*\\d+\\s*$", "");
        cleaned = cleaned.replaceAll("[\\s\\-().]", "");
        if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2);
        }
        return cleaned;
    }
}
