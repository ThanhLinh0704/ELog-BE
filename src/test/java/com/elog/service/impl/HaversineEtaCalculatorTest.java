package com.elog.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class HaversineEtaCalculatorTest {

    private final HaversineEtaCalculator calculator = new HaversineEtaCalculator(null, null, null);

    @Test
    @DisplayName("[L1-HV-01] same coordinates have zero distance")
    void l1Hv01_sameCoordinatesReturnZero() {
        assertThat(calculator.haversine(21.0, 105.8, 21.0, 105.8)).isZero();
    }

    @Test
    @DisplayName("[L1-HV-02] Hanoi to Ho Chi Minh City has known distance")
    void l1Hv02_hanoiToHoChiMinhIsApproximatelyKnownDistance() {
        assertThat(calculator.haversine(21.0285, 105.8542, 10.8231, 106.6297))
                .isBetween(1_133.0, 1_143.0);
    }

    @Test
    @DisplayName("[L1-HV-03] antipodal poles have half-earth circumference")
    void l1Hv03_antipodalPolesReturnHalfEarthCircumference() {
        assertThat(calculator.haversine(-90.0, 0.0, 90.0, 0.0))
                .isBetween(20_010.0, 20_020.0);
    }

    @Test
    @DisplayName("[L1-HV-04] short urban points have local positive distance")
    void l1Hv04_shortUrbanPairReturnsPositiveLocalDistance() {
        assertThat(calculator.haversine(21.02, 105.83, 21.05, 105.86))
                .isBetween(4.4, 4.8);
    }
}
