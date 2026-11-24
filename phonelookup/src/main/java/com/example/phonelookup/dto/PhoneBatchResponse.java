package com.example.phonelookup.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneBatchResponse {

    private int count;
    private List<PhoneBatchItem> results;
}
