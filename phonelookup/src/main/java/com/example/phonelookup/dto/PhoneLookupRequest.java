package com.example.phonelookup.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhoneLookupRequest {

    @NotBlank
    private String phoneNumber;

    private String defaultRegion;
}
