package com.example.phonelookup.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.phonelookup.dto.PhoneBatchRequest;
import com.example.phonelookup.dto.PhoneBatchResponse;
import com.example.phonelookup.service.PhoneBatchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/lookup")
@RequiredArgsConstructor
@Tag(name = "Batch phone lookup")
public class PhoneBatchController {

    private final PhoneBatchService phoneBatchService;

    @PostMapping("/phones")
    @Operation(summary = "Look up 10 to 20 phone numbers")
    public PhoneBatchResponse lookupPhones(@Valid @RequestBody PhoneBatchRequest request) {
        return phoneBatchService.lookup(request);
    }

    @PostMapping(value = "/phones/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a .csv or .txt file. Each line is phone,region")
    public PhoneBatchResponse uploadPhones(@RequestParam("file") MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return phoneBatchService.lookupUpload(file.getOriginalFilename(), content);
    }
}
