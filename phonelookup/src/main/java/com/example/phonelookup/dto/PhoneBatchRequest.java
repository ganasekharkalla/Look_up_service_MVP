package com.example.phonelookup.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhoneBatchRequest {

    @NotNull
    @Size(min = 10, max = 20, message = "A batch must contain between 10 and 20 phone numbers")
    private List<@Valid PhoneLookupRequest> numbers;
}
