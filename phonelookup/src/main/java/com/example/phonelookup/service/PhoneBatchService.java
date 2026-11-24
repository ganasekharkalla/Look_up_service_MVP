package com.example.phonelookup.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.phonelookup.dto.PhoneBatchItem;
import com.example.phonelookup.dto.PhoneBatchRequest;
import com.example.phonelookup.dto.PhoneBatchResponse;
import com.example.phonelookup.dto.PhoneLookupRequest;
import com.example.phonelookup.dto.PhoneLookupResponse;
import com.example.phonelookup.exception.InvalidPhoneException;
import com.example.phonelookup.exception.UpstreamProviderException;

@Service
public class PhoneBatchService {

    public static final int MIN_BATCH_SIZE = 10;
    public static final int MAX_BATCH_SIZE = 20;

    private final PhoneLookupService phoneLookupService;
    private final PhoneSanitizerService phoneSanitizerService;

    public PhoneBatchService(
            PhoneLookupService phoneLookupService,
            PhoneSanitizerService phoneSanitizerService) {
        this.phoneLookupService = phoneLookupService;
        this.phoneSanitizerService = phoneSanitizerService;
    }

    public PhoneBatchResponse lookup(PhoneBatchRequest request) {
        return lookupAll(request.getNumbers());
    }

    public PhoneBatchResponse lookupUpload(String filename, String content) {
        requireSupportedFile(filename);
        return lookupAll(parseUpload(content));
    }

    public void requireSupportedFile(String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new InvalidPhoneException("Upload a .csv or .txt file.");
        }
        String lower = filename.trim().toLowerCase();
        if (!lower.endsWith(".csv") && !lower.endsWith(".txt")) {
            throw new InvalidPhoneException(
                    "Only .csv and .txt files are accepted. Received: " + filename);
        }
    }

    public List<PhoneLookupRequest> parseUpload(String content) {
        List<PhoneLookupRequest> requests = new ArrayList<>();
        if (content == null) {
            return requests;
        }
        boolean headerStillPossible = true;
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (!StringUtils.hasText(trimmed) || trimmed.startsWith("#")) {
                continue;
            }
            if (headerStillPossible && isHeader(trimmed)) {
                headerStillPossible = false;
                continue;
            }
            headerStillPossible = false;
            String[] parts = trimmed.split(",", -1);
            String phone = parts[0].trim();
            String region = parts.length > 1 ? parts[1].trim() : "";
            requests.add(new PhoneLookupRequest(phone, phoneSanitizerService.isRegionMissing(region) ? null : region));
        }
        return requests;
    }

    public PhoneBatchResponse lookupAll(List<PhoneLookupRequest> requests) {
        int size = requests == null ? 0 : requests.size();
        if (size < MIN_BATCH_SIZE || size > MAX_BATCH_SIZE) {
            throw new InvalidPhoneException(
                    "A batch must contain between 10 and 20 phone numbers. Received: " + size);
        }

        List<PhoneBatchItem> results = new ArrayList<>();
        for (PhoneLookupRequest request : requests) {
            results.add(lookupOne(request));
        }
        return PhoneBatchResponse.builder().count(size).results(results).build();
    }

    private PhoneBatchItem lookupOne(PhoneLookupRequest request) {
        if (phoneSanitizerService.isRegionMissing(request.getDefaultRegion())
                && !phoneSanitizerService.hasExplicitCountryCode(request.getPhoneNumber())) {
            return errorItem(request.getPhoneNumber(), 400, PhoneSanitizerService.MISSING_COUNTRY_CODE);
        }
        try {
            PhoneLookupResponse response = phoneLookupService.lookup(request);
            return PhoneBatchItem.builder()
                    .input(request.getPhoneNumber())
                    .outcome("SUCCESS")
                    .status(200)
                    .lookup(response)
                    .build();
        } catch (InvalidPhoneException ex) {
            return errorItem(request.getPhoneNumber(), 400, ex.getMessage());
        } catch (UpstreamProviderException ex) {
            return errorItem(request.getPhoneNumber(), ex.getStatusCode(), ex.getMessage());
        }
    }

    private PhoneBatchItem errorItem(String input, int status, String message) {
        return PhoneBatchItem.builder()
                .input(input)
                .outcome("ERROR")
                .status(status)
                .message(message)
                .build();
    }

    private boolean isHeader(String line) {
        String normalized = line.toLowerCase().replace(" ", "");
        return normalized.equals("phone")
                || normalized.equals("phonenumber")
                || normalized.equals("number")
                || normalized.startsWith("phone,")
                || normalized.startsWith("phonenumber,")
                || normalized.startsWith("number,");
    }
}
