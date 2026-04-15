package com.littlepay;

import com.littlepay.io.TapCsvReader;
import com.littlepay.io.TripCsvWriter;
import com.littlepay.model.Trip;
import com.littlepay.model.TripStatus;
import com.littlepay.service.FareCalculator;
import com.littlepay.service.TripProcessor;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransitProcessorIntegrationTest {

    private static final String EXAMPLE_TAPS = """
            ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN
            1, 22-01-2023 13:00:00, ON, Stop1, Company1, Bus37, 5454545454545454
            2, 22-01-2023 13:05:00, OFF, Stop2, Company1, Bus37, 5454545454545454
            3, 22-01-2023 09:20:00, ON, Stop3, Company1, Bus36, 4444333322221111
            4, 23-01-2023 08:00:00, ON, Stop1, Company1, Bus37, 4444333322221111
            5, 23-01-2023 08:02:00, OFF, Stop1, Company1, Bus37, 4444333322221111
            6, 24-01-2023 16:30:00, OFF, Stop2, Company1, Bus37, 5454545454545454
            """;

    @Test
    void shouldBuildThreeTripsFromExampleInput() throws Exception {
        TapCsvReader reader = new TapCsvReader();
        TripProcessor processor = new TripProcessor(new FareCalculator(FareCalculator.DEFAULT_RULES));

        List<Trip> trips = processor.process(reader.read(new StringReader(EXAMPLE_TAPS)));

        // tap 6 is an orphaned OFF - ignored
        assertThat(trips).hasSize(3);
    }

    @Test
    void shouldProcessTap1And2AsCompletedTrip() throws Exception {
        List<Trip> trips = process();

        Trip completed = trips.stream()
                .filter(t -> t.pan().equals("5454545454545454") && t.status() == TripStatus.COMPLETED)
                .findFirst().orElseThrow();

        assertThat(completed.fromStopId()).isEqualTo("Stop1");
        assertThat(completed.toStopId()).isEqualTo("Stop2");
        assertThat(completed.chargeAmount()).isEqualByComparingTo(new BigDecimal("3.25"));
        assertThat(completed.durationSecs()).isEqualTo(300);
    }

    @Test
    void shouldProcessTap3AsIncompleteTrip() throws Exception {
        List<Trip> trips = process();

        Trip incomplete = trips.stream()
                .filter(t -> t.pan().equals("4444333322221111") && t.status() == TripStatus.INCOMPLETE)
                .findFirst().orElseThrow();

        assertThat(incomplete.fromStopId()).isEqualTo("Stop3");
        assertThat(incomplete.chargeAmount()).isEqualByComparingTo(new BigDecimal("7.30"));
        assertThat(incomplete.finished()).isNull();
    }

    @Test
    void shouldProcessTap4And5AsCancelledTrip() throws Exception {
        List<Trip> trips = process();

        Trip cancelled = trips.stream()
                .filter(t -> t.pan().equals("4444333322221111") && t.status() == TripStatus.CANCELLED)
                .findFirst().orElseThrow();

        assertThat(cancelled.fromStopId()).isEqualTo("Stop1");
        assertThat(cancelled.chargeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cancelled.durationSecs()).isEqualTo(120);
    }

    @Test
    void shouldIgnoreOrphanedTap6() throws Exception {
        List<Trip> trips = process();

        // PAN 5454545454545454 has only one trip (COMPLETED from taps 1&2); tap 6 is ignored
        long countForPan = trips.stream()
                .filter(t -> t.pan().equals("5454545454545454"))
                .count();
        assertThat(countForPan).isEqualTo(1);
    }

    @Test
    void shouldWriteOutputCsvWithCorrectHeader() throws Exception {
        List<Trip> trips = process();
        StringWriter out = new StringWriter();
        new TripCsvWriter().write(out, trips);

        assertThat(out.toString()).startsWith(
                "Started,Finished,DurationSecs,FromStopId,ToStopId,ChargeAmount,CompanyId,BusID,PAN,Status");
    }

    private List<Trip> process() throws Exception {
        return new TripProcessor(new FareCalculator(FareCalculator.DEFAULT_RULES))
                .process(new TapCsvReader().read(new StringReader(EXAMPLE_TAPS)));
    }
}
