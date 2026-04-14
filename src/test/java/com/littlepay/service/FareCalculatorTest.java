package com.littlepay.service;

import com.littlepay.model.FareRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FareCalculatorTest {

    private final FareCalculator calculator = new FareCalculator(FareCalculator.DEFAULT_RULES);

    @ParameterizedTest(name = "{0} -> {1} = ${2}")
    @CsvSource({
            "Stop1, Stop2, 3.25",
            "Stop2, Stop1, 3.25",
            "Stop2, Stop3, 5.50",
            "Stop3, Stop2, 5.50",
            "Stop1, Stop3, 7.30",
            "Stop3, Stop1, 7.30"
    })
    void shouldReturnCorrectFareForBothDirections(String from, String to, String expected) {
        assertThat(calculator.getFare(from, to))
                .isEqualByComparingTo(new BigDecimal(expected));
    }

    @ParameterizedTest(name = "max fare from {0} = ${1}")
    @CsvSource({
            "Stop1, 7.30",
            "Stop2, 5.50",
            "Stop3, 7.30"
    })
    void shouldReturnMaxFarePerStop(String stop, String expected) {
        assertThat(calculator.getMaxFare(stop))
                .isEqualByComparingTo(new BigDecimal(expected));
    }

    @Test
    void shouldUseDefaultRules() {
        FareCalculator custom = new FareCalculator(List.of(
                new FareRule("StopA", "StopB", new BigDecimal("9.99"))
        ));

        assertThat(custom.getFare("StopA", "StopB"))
                .isEqualByComparingTo(new BigDecimal("9.99"));

        assertThatThrownBy(() -> custom.getFare("Stop1", "Stop2"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
