package com.littlepay.service;

import com.littlepay.model.FareRule;

import java.math.BigDecimal;
import java.util.List;

public class FareCalculator {

    public static final List<FareRule> DEFAULT_RULES = List.of(
            new FareRule("Stop1", "Stop2", new BigDecimal("3.25")),
            new FareRule("Stop2", "Stop1", new BigDecimal("3.25")),
            new FareRule("Stop2", "Stop3", new BigDecimal("5.50")),
            new FareRule("Stop3", "Stop2", new BigDecimal("5.50")),
            new FareRule("Stop1", "Stop3", new BigDecimal("7.30")),
            new FareRule("Stop3", "Stop1", new BigDecimal("7.30"))
    );

    private final List<FareRule> rules;

    public FareCalculator(List<FareRule> rules) {
        this.rules = rules;
    }

    public BigDecimal getFare(String fromStop, String toStop) {
        return rules.stream()
                .filter(r -> r.fromStop().equals(fromStop) && r.toStop().equals(toStop))
                .map(FareRule::amount)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No fare defined for stops: " + fromStop + ", " + toStop));
    }

    public BigDecimal getMaxFare(String fromStop) {
        return rules.stream()
                .filter(r -> r.fromStop().equals(fromStop))
                .map(FareRule::amount)
                .max(BigDecimal::compareTo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No fares defined for stop: " + fromStop));
    }
}
