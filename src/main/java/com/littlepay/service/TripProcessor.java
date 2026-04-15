package com.littlepay.service;

import com.littlepay.model.TapEvent;
import com.littlepay.model.TapType;
import com.littlepay.model.Trip;
import com.littlepay.model.TripStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TripProcessor {

    private final FareCalculator fareCalculator;

    public TripProcessor(FareCalculator fareCalculator) {
        this.fareCalculator = fareCalculator;
    }

    public List<Trip> process(List<TapEvent> tapEvents) {
        List<TapEvent> sortedTapEvents = tapEvents.stream()
                .sorted(Comparator.comparing(TapEvent::dateTimeUTC))
                .toList();

        Map<String, TapEvent> unpairedOnEvents = new HashMap<>();
        List<Trip> trips = new ArrayList<>();

        for (TapEvent tap : sortedTapEvents) {
            String key = tap.pan() + "|" + tap.busId();

            if (tap.tapType() == TapType.ON) {
                if (unpairedOnEvents.containsKey(key)) {
                    // Double ON: close the first as incompleted, start fresh
                    trips.add(resolveIncompleteTrip(unpairedOnEvents.get(key)));
                }
                unpairedOnEvents.put(key, tap);
            } else {
                // Current tap is an OFF; fetch ON event from map
                TapEvent openTapEvent = unpairedOnEvents.remove(key);
                if (openTapEvent == null) {
                    // Ignore Orphaned OFF
                    continue;
                }
                trips.add(resolveTrip(openTapEvent, tap));
            }
        }

        // Mark any remaining open ONs as INCOMPLETE
        unpairedOnEvents.values().forEach(openTap -> trips.add(resolveIncompleteTrip(openTap)));

        return trips;
    }

    private Trip resolveTrip(TapEvent on, TapEvent off) {
        long durationSecs = Duration.between(on.dateTimeUTC(), off.dateTimeUTC()).getSeconds();

        if (on.stopId().equals(off.stopId())) {
            return new Trip(
                    on.dateTimeUTC(), off.dateTimeUTC(), durationSecs,
                    on.stopId(), off.stopId(), BigDecimal.ZERO,
                    on.companyId(), on.busId(), on.pan(),
                    TripStatus.CANCELLED
            );
        }

        return new Trip(
                on.dateTimeUTC(), off.dateTimeUTC(), durationSecs,
                on.stopId(), off.stopId(), fareCalculator.getFare(on.stopId(), off.stopId()),
                on.companyId(), on.busId(), on.pan(),
                TripStatus.COMPLETED
        );
    }

    private Trip resolveIncompleteTrip(TapEvent on) {
        return new Trip(
                on.dateTimeUTC(), null, 0,
                on.stopId(), null, fareCalculator.getMaxFare(on.stopId()),
                on.companyId(), on.busId(), on.pan(),
                TripStatus.INCOMPLETE
        );
    }
}
