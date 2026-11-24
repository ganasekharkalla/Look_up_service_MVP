package com.example.phonelookup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.phonelookup.dto.PhoneBatchResponse;
import com.example.phonelookup.dto.PhoneLookupRequest;
import com.example.phonelookup.dto.PhoneLookupResponse;
import com.example.phonelookup.exception.InvalidPhoneException;
import com.example.phonelookup.exception.UpstreamProviderException;

@ExtendWith(MockitoExtension.class)
class PhoneBatchServiceTest {

    @Mock
    private PhoneLookupService phoneLookupService;

    @Test
    void rejectsFewerThanTenBeforeLookup() {
        PhoneBatchService service = new PhoneBatchService(phoneLookupService, new PhoneSanitizerService());

        assertThatThrownBy(() -> service.lookupAll(requests(9)))
                .isInstanceOf(InvalidPhoneException.class)
                .hasMessageContaining("between 10 and 20");
        verify(phoneLookupService, never()).lookup(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsMoreThanTwentyBeforeLookup() {
        PhoneBatchService service = new PhoneBatchService(phoneLookupService, new PhoneSanitizerService());

        assertThatThrownBy(() -> service.lookupAll(requests(21)))
                .isInstanceOf(InvalidPhoneException.class)
                .hasMessageContaining("Received: 21");
    }

    @Test
    void keepsGoingWhenOneNumberFails() {
        PhoneBatchService service = new PhoneBatchService(phoneLookupService, new PhoneSanitizerService());
        List<PhoneLookupRequest> requests = requests(10);
        when(phoneLookupService.lookup(requests.get(0)))
                .thenThrow(new InvalidPhoneException("Invalid phone number: bad"));
        when(phoneLookupService.lookup(requests.get(1)))
                .thenThrow(new UpstreamProviderException("invalid number", 400, "{\"message\":\"invalid number\"}"));
        for (int i = 2; i < requests.size(); i++) {
            when(phoneLookupService.lookup(requests.get(i))).thenReturn(PhoneLookupResponse.builder()
                    .phoneE164("+91987654321" + i)
                    .source("VERIPHONE")
                    .build());
        }

        PhoneBatchResponse response = service.lookupAll(requests);

        assertThat(response.getCount()).isEqualTo(10);
        assertThat(response.getResults().get(0).getOutcome()).isEqualTo("ERROR");
        assertThat(response.getResults().get(0).getStatus()).isEqualTo(400);
        assertThat(response.getResults().get(1).getMessage()).contains("invalid number");
        assertThat(response.getResults().get(2).getOutcome()).isEqualTo("SUCCESS");
        assertThat(response.getResults().get(2).getLookup().getSource()).isEqualTo("VERIPHONE");
    }

    @Test
    void parsesCsvHeaderAndPerLineRegion() {
        PhoneBatchService service = new PhoneBatchService(phoneLookupService, new PhoneSanitizerService());
        String csv = """
                phone,region
                9876543210,IN
                +14155552671,US

                # comment
                2025550123
                """;

        List<PhoneLookupRequest> parsed = service.parseUpload(csv);

        assertThat(parsed).hasSize(3);
        assertThat(parsed.get(0).getDefaultRegion()).isEqualTo("IN");
        assertThat(parsed.get(1).getPhoneNumber()).isEqualTo("+14155552671");
        assertThat(parsed.get(1).getDefaultRegion()).isEqualTo("US");
        assertThat(parsed.get(2).getDefaultRegion()).isNull();
    }

    @Test
    void nationalNumberWithoutRegionFailsWithoutLookup() {
        PhoneBatchService service = new PhoneBatchService(phoneLookupService, new PhoneSanitizerService());
        List<PhoneLookupRequest> requests = requests(9);
        requests.add(new PhoneLookupRequest("2025550123", null));

        PhoneBatchResponse response = service.lookupAll(requests);

        assertThat(response.getResults().get(9).getOutcome()).isEqualTo("ERROR");
        assertThat(response.getResults().get(9).getMessage()).contains("Provide a country code");
        verify(phoneLookupService, never()).lookup(requests.get(9));
    }

    @Test
    void countryCodeWithoutRegionStillLooksUp() {
        PhoneBatchService service = new PhoneBatchService(phoneLookupService, new PhoneSanitizerService());
        List<PhoneLookupRequest> requests = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            requests.add(new PhoneLookupRequest("+1415555267" + i, null));
        }
        when(phoneLookupService.lookup(any())).thenReturn(PhoneLookupResponse.builder()
                .phoneE164("+14155552670")
                .appliedRegion("US")
                .regionSource("NUMBER")
                .source("VERIPHONE")
                .build());

        PhoneBatchResponse response = service.lookupAll(requests);

        assertThat(response.getResults()).allMatch(item -> "SUCCESS".equals(item.getOutcome()));
        verify(phoneLookupService, times(10)).lookup(any());
    }

    @Test
    void acceptsCsvAndTxtOnly() {
        PhoneBatchService service = new PhoneBatchService(phoneLookupService, new PhoneSanitizerService());

        service.requireSupportedFile("numbers.CSV");
        service.requireSupportedFile("numbers.txt");
        assertThatThrownBy(() -> service.requireSupportedFile("numbers.xlsx"))
                .isInstanceOf(InvalidPhoneException.class)
                .hasMessageContaining(".csv and .txt");
    }

    private List<PhoneLookupRequest> requests(int count) {
        List<PhoneLookupRequest> requests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            requests.add(new PhoneLookupRequest("900000000" + i, "IN"));
        }
        return requests;
    }
}
