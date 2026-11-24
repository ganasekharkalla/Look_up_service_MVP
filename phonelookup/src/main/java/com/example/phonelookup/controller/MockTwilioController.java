package com.example.phonelookup.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/v2/PhoneNumbers")
@Profile("!prod")
public class  MockTwilioController {

    private static final List<String> CARRIERS = List.of("Jio", "Airtel", "Vi India", "BSNL");
    private static final List<String> LINE_TYPES = List.of("mobile", "landline", "voip");

    @GetMapping("/{phoneNumber}")
    public Map<String, Object> mockTwilioLookup(
            @PathVariable String phoneNumber,
            @RequestParam(name = "Fields", required = false) String fields) {

        String randomCarrier = CARRIERS.get(ThreadLocalRandom.current().nextInt(CARRIERS.size()));
        String randomLineType = LINE_TYPES.get(ThreadLocalRandom.current().nextInt(LINE_TYPES.size()));

        return Map.of(
                "phone_number", phoneNumber,
                "valid", true,
                "country_code", "IN",
                "calling_country_code", "91",
                "national_format", phoneNumber,
                "line_type_intelligence", Map.of(
                        "type", randomLineType,
                        "carrier_name", randomCarrier
                )
        );
    }
}
