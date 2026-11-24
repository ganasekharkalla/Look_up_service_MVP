package com.example.phonelookup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.example.phonelookup.dto.NormalizedPhone;
import com.example.phonelookup.exception.InvalidPhoneException;

class PhoneSanitizerServiceTest {

    private final PhoneSanitizerService service = new PhoneSanitizerService();

    @Test
    void nationalNumberUsesTheRegion() {
        NormalizedPhone normalized = service.sanitizeAndFormatE164("4155552671", "us");

        assertThat(normalized.getPhoneE164()).isEqualTo("+14155552671");
        assertThat(normalized.getRequestedRegion()).isEqualTo("US");
        assertThat(normalized.getAppliedRegion()).isEqualTo("US");
        assertThat(normalized.getRegionSource()).isEqualTo("REGION");
    }

    @Test
    void countryCodeInTheNumberWinsOverADifferentRegion() {
        NormalizedPhone normalized = service.sanitizeAndFormatE164("+91 98765 43210", "US");

        assertThat(normalized.getPhoneE164()).isEqualTo("+919876543210");
        assertThat(normalized.getRequestedRegion()).isEqualTo("US");
        assertThat(normalized.getAppliedRegion()).isEqualTo("IN");
        assertThat(normalized.getRegionSource()).isEqualTo("NUMBER");
    }

    @Test
    void leadingDoubleZeroIsTreatedAsACountryCode() {
        NormalizedPhone normalized = service.sanitizeAndFormatE164("00919876543210", "US");

        assertThat(normalized.getPhoneE164()).isEqualTo("+919876543210");
        assertThat(normalized.getAppliedRegion()).isEqualTo("IN");
        assertThat(normalized.getRegionSource()).isEqualTo("NUMBER");
    }

    @Test
    void countryCodeWithoutRegionUsesTheNumber() {
        NormalizedPhone normalized = service.sanitizeAndFormatE164("+919876543210", null);

        assertThat(normalized.getPhoneE164()).isEqualTo("+919876543210");
        assertThat(normalized.getRequestedRegion()).isNull();
        assertThat(normalized.getAppliedRegion()).isEqualTo("IN");
        assertThat(normalized.getRegionSource()).isEqualTo("NUMBER");
    }

    @Test
    void wordNullRegionIsIgnoredWhenTheNumberHasACountryCode() {
        NormalizedPhone normalized = service.sanitizeAndFormatE164("00919876543210", "null");

        assertThat(normalized.getPhoneE164()).isEqualTo("+919876543210");
        assertThat(normalized.getAppliedRegion()).isEqualTo("IN");
        assertThat(normalized.getRegionSource()).isEqualTo("NUMBER");
    }

    @Test
    void nationalNumberWithoutRegionAsksForACountryCode() {
        assertThatThrownBy(() -> service.sanitizeAndFormatE164("9876543210", null))
                .isInstanceOf(InvalidPhoneException.class)
                .hasMessageContaining("Provide a country code");
    }

    @Test
    void rejectsRegionNameThatIsNotAnIsoCode() {
        assertThatThrownBy(() -> service.sanitizeAndFormatE164("4155552671", "USA"))
                .isInstanceOf(InvalidPhoneException.class)
                .hasMessageContaining("2-letter ISO code");
    }

    @Test
    void rejectsIndianNationalNumberWhenRegionIsUs() {
        assertThatThrownBy(() -> service.sanitizeAndFormatE164("9876543210", "US"))
                .isInstanceOf(InvalidPhoneException.class)
                .hasMessageContaining("region US")
                .hasMessageContaining("no country code");
    }
}
