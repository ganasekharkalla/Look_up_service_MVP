package com.example.phonelookup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneBatchItem {

    private String input;
    private String outcome;
    private Integer status;
    private String message;
    private PhoneLookupResponse lookup;
}
