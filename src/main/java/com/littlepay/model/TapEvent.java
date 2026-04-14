package com.littlepay.model;

import java.time.LocalDateTime;

public record TapEvent(
        int id,
        LocalDateTime dateTimeUTC,
        TapType tapType,
        String stopId,
        String companyId,
        String busId,
        String pan
) {}
