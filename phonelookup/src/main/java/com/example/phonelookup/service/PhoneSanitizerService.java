package com.example.phonelookup.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.phonelookup.exception.InvalidPhoneException;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

@Service
public class PhoneSanitizerService {

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();
    private static final String DEFAULT_REGION = "IN";

    public String sanitizeAndFormatE164(String rawPhone, String defaultRegion) {
        if (!StringUtils.hasText(rawPhone)) {
            throw new InvalidPhoneException("Phone number must not be null or blank");
        }

        String region = StringUtils.hasText(defaultRegion) ? defaultRegion.trim() : DEFAULT_REGION;
        String cleaned = cleanRawPhone(rawPhone);

        try {
            PhoneNumber parsed = PHONE_UTIL.parse(cleaned, region);
            if (!PHONE_UTIL.isValidNumber(parsed)) {
                throw new InvalidPhoneException("Invalid phone number: " + rawPhone);
            }
            return PHONE_UTIL.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException ex) {
            throw new InvalidPhoneException("Unparseable phone number: " + rawPhone, ex);
        }
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
