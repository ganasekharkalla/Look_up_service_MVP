package com.example.phonelookup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedPhone {

    private String phoneE164;
    private String requestedRegion;
    private String appliedRegion;
    private String regionSource;
}
