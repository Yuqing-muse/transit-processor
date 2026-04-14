package com.littlepay.model;

import java.math.BigDecimal;

public record FareRule(String fromStop, String toStop, BigDecimal amount) {}
