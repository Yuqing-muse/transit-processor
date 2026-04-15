package com.littlepay.service;

import com.littlepay.model.TapEvent;
import com.littlepay.model.TapType;
import com.littlepay.model.Trip;
import com.littlepay.model.TripStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TripProcessorTest {

    private static final String PAN_A = "5454545454545454";
    private static final String PAN_B = "4444333322221111";
    private static final String COMPANY = "Company1";
    private static final String BUS = "Bus37";

    private final TripProcessor processor = new TripProcessor(new FareCalculator(FareCalculator.DEFAULT_RULES));

    private TapEvent createTapEvent(int id, String datetime, TapType type, String stop, String pan) {
        return new TapEvent(id, LocalDateTime.parse(datetime.replace(" ", "T")),
                type, stop, COMPANY, BUS, pan);
    }

    @Test
    void shouldProcessCompletedTrip() {
        List<TapEvent> taps = List.of(
                createTapEvent(1, "2023-01-22 13:00:00", TapType.ON, "Stop1", PAN_A),
                createTapEvent(2, "2023-01-22 13:05:00", TapType.OFF, "Stop2", PAN_A)
        );

        List<Trip> trips = processor.process(taps);

        assertThat(trips).hasSize(1);

        Trip trip = trips.get(0);
        assertThat(trip.status()).isEqualTo(TripStatus.COMPLETED);
        assertThat(trip.fromStopId()).isEqualTo("Stop1");
        assertThat(trip.toStopId()).isEqualTo("Stop2");
        assertThat(trip.chargeAmount()).isEqualByComparingTo(new BigDecimal("3.25"));
        assertThat(trip.durationSecs()).isEqualTo(300);
    }

    @Test
    void shouldCancelTripWhenTapOnAndOffAtSameStop() {
        List<TapEvent> taps = List.of(
                createTapEvent(1, "2023-01-23 08:00:00", TapType.ON, "Stop1", PAN_A),
                createTapEvent(2, "2023-01-23 08:02:00", TapType.OFF, "Stop1", PAN_A)
        );

        List<Trip> trips = processor.process(taps);

        assertThat(trips).hasSize(1);

        Trip trip = trips.get(0);
        assertThat(trip.status()).isEqualTo(TripStatus.CANCELLED);
        assertThat(trip.chargeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(trip.durationSecs()).isEqualTo(120);
    }

    @Test
    void shouldMarkTripIncompleteWithoutTapOff() {
        List<TapEvent> taps = List.of(
                createTapEvent(1, "2023-01-22 09:20:00", TapType.ON, "Stop3", PAN_A)
        );

        List<Trip> trips = processor.process(taps);

        assertThat(trips).hasSize(1);

        Trip trip = trips.get(0);
        assertThat(trip.status()).isEqualTo(TripStatus.INCOMPLETE);
        assertThat(trip.chargeAmount()).isEqualByComparingTo(new BigDecimal("7.30"));
        assertThat(trip.finished()).isNull();
        assertThat(trip.toStopId()).isNull();
        assertThat(trip.durationSecs()).isZero();
    }

    @Test
    void shouldCloseFirstTripAsIncompleteWhenDoubleTapOn() {
        List<TapEvent> taps = List.of(
                createTapEvent(1, "2023-01-22 09:00:00", TapType.ON, "Stop1", PAN_A),
                createTapEvent(2, "2023-01-22 09:10:00", TapType.ON, "Stop2", PAN_A),
                createTapEvent(3, "2023-01-22 09:20:00", TapType.OFF, "Stop3", PAN_A)
        );

        List<Trip> trips = processor.process(taps);

        assertThat(trips).hasSize(2);

        Trip incompleteTrip = trips.stream().filter(t -> t.status() == TripStatus.INCOMPLETE).findFirst().orElseThrow();
        assertThat(incompleteTrip.fromStopId()).isEqualTo("Stop1");
        assertThat(incompleteTrip.chargeAmount()).isEqualByComparingTo(new BigDecimal("7.30"));

        Trip completedTrip = trips.stream().filter(t -> t.status() == TripStatus.COMPLETED).findFirst().orElseThrow();
        assertThat(completedTrip.fromStopId()).isEqualTo("Stop2");
        assertThat(completedTrip.toStopId()).isEqualTo("Stop3");
        assertThat(completedTrip.chargeAmount()).isEqualByComparingTo(new BigDecimal("5.50"));
    }

    @Test
    void shouldIgnoreOrphanTapOff() {
        List<TapEvent> taps = List.of(
                createTapEvent(1, "2023-01-24 16:30:00", TapType.OFF, "Stop2", PAN_A)
        );

        List<Trip> trips = processor.process(taps);

        assertThat(trips).isEmpty();
    }

    @Test
    void shouldProcessMultipleEventsBasedOnPan() {
        List<TapEvent> taps = List.of(
                createTapEvent(1, "2023-01-22 13:00:00", TapType.ON, "Stop1", PAN_A),
                createTapEvent(2, "2023-01-22 09:20:00", TapType.ON, "Stop3", PAN_B),
                createTapEvent(3, "2023-01-22 13:05:00", TapType.OFF, "Stop2", PAN_A)
        );

        List<Trip> trips = processor.process(taps);

        assertThat(trips).hasSize(2);
        assertThat(trips).anySatisfy(t -> {
            assertThat(t.pan()).isEqualTo(PAN_A);
            assertThat(t.status()).isEqualTo(TripStatus.COMPLETED);
        });
        assertThat(trips).anySatisfy(t -> {
            assertThat(t.pan()).isEqualTo(PAN_B);
            assertThat(t.status()).isEqualTo(TripStatus.INCOMPLETE);
        });
    }
}
