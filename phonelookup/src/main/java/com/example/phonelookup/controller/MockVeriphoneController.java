package com.example.phonelookup.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Profile;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2")
@Profile("!prod")
@Hidden
public class MockVeriphoneController {

    @RequestMapping(value = "/verify", method = {RequestMethod.GET, RequestMethod.POST})
    public Map<String, Object> verify(@RequestParam("phone") String phone) {
        String e164 = phone == null ? "" : phone.trim();
        CountryInfo country = countryFor(e164);
        String national = country.nationalNumber();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "success");
        body.put("phone", e164);
        body.put("phone_valid", true);
        body.put("phone_type", phoneType(e164));
        body.put("phone_region", country.region());
        body.put("country", country.name());
        body.put("country_code", country.code());
        body.put("country_prefix", country.prefix());
        body.put("international_number", internationalFormat(country, national, e164));
        body.put("local_number", localFormat(country, national, e164));
        body.put("e164", e164);
        body.put("carrier", "mock");
        return body;
    }

    private String phoneType(String phone) {
        if (phone.isEmpty()) {
            return "unknown";
        }
        char last = phone.charAt(phone.length() - 1);
        int digit = Character.isDigit(last) ? last - '0' : 0;
        return switch (digit % 3) {
            case 0 -> "mobile";
            case 1 -> "fixed_line";
            default -> "voip";
        };
    }

    private CountryInfo countryFor(String phone) {
        if (phone.startsWith("+91")) {
            return new CountryInfo("India", "IN", "91", "India", phone.substring(3));
        }
        if (phone.startsWith("+44")) {
            return new CountryInfo("United Kingdom", "GB", "44", "United Kingdom", phone.substring(3));
        }
        if (phone.startsWith("+1")) {
            return new CountryInfo("United States", "US", "1", "United States", phone.substring(2));
        }
        String national = phone.startsWith("+") ? phone.substring(1) : phone;
        return new CountryInfo("", "", "", "", national);
    }

    private String internationalFormat(CountryInfo country, String national, String e164) {
        if ("IN".equals(country.code()) && national.length() == 10) {
            return "+91 " + national.substring(0, 5) + " " + national.substring(5);
        }
        if ("US".equals(country.code()) && national.length() == 10) {
            return "+1 " + national.substring(0, 3) + "-" + national.substring(3, 6) + "-" + national.substring(6);
        }
        return e164;
    }

    private String localFormat(CountryInfo country, String national, String e164) {
        if ("IN".equals(country.code()) && national.length() == 10) {
            return "0" + national.substring(0, 5) + " " + national.substring(5);
        }
        if ("US".equals(country.code()) && national.length() == 10) {
            return "(" + national.substring(0, 3) + ") " + national.substring(3, 6) + "-" + national.substring(6);
        }
        return national.isEmpty() ? e164 : national;
    }

    private record CountryInfo(String name, String code, String prefix, String region, String nationalNumber) {
    }
}
