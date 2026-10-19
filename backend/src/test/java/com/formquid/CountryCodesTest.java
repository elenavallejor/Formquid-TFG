package com.formquid;

import com.formquid.common.CountryCodes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CountryCodesTest {

    @Test
    void matchingCodes_findsCountryBySpanishName() {
        assertThat(CountryCodes.matchingCodes("españa")).contains("ES");
    }

    @Test
    void normalizeConfigured_convertsKnownNameWithoutRejectingLegacyValue() {
        assertThat(CountryCodes.normalizeConfigured(" España ")).isEqualTo("ES");
        assertThat(CountryCodes.normalizeConfigured("País anterior")).isEqualTo("País anterior");
    }
}
