package com.littlepay.io;

import com.littlepay.model.Trip;
import com.littlepay.model.TripStatus;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TripCsvWriterTest {

    private static final LocalDateTime T1 = LocalDateTime.of(2023, 1, 22, 13, 0, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2023, 1, 22, 13, 5, 0);
    private static final String PAN = "5454545454545454";

    private final TripCsvWriter writer = new TripCsvWriter();

    @Test
    void shouldWriteCompletedTripRow() throws Exception {
        Trip trip = new Trip(T1, T2, 300, "Stop1", "Stop2",
                new BigDecimal("3.25"), "Company1", "Bus37", PAN, TripStatus.COMPLETED);

        StringWriter out = new StringWriter();
        writer.write(out, List.of(trip));

        String content = out.toString();
        assertThat(content).contains("22-01-2023 13:00:00");
        assertThat(content).contains("22-01-2023 13:05:00");
        assertThat(content).contains("300");
        assertThat(content).contains("Stop1");
        assertThat(content).contains("Stop2");
        assertThat(content).contains("$3.25");
        assertThat(content).contains("COMPLETED");
    }

    @Test
    void shouldWriteIncompleteTripRowWithEmptyFields() throws Exception {
        Trip trip = new Trip(T1, null, 0, "Stop3", null,
                new BigDecimal("7.30"), "Company1", "Bus37", PAN, TripStatus.INCOMPLETE);

        StringWriter out = new StringWriter();
        writer.write(out, List.of(trip));

        String[] lines = out.toString().split("\n");
        String dataLine = lines[1];
        String[] fields = dataLine.split(",");
        assertThat(fields[1].trim()).isEmpty();   // Finished
        assertThat(fields[2].trim()).isEqualTo("0"); // DurationSecs
        assertThat(fields[4].trim()).isEmpty();   // ToStopId
        assertThat(fields[9].trim()).isEqualTo("INCOMPLETE");
    }

    @Test
    void shouldWriteCancelledTripRowWithZeroCharge() throws Exception {
        Trip trip = new Trip(T1, T2, 120, "Stop1", "Stop1",
                BigDecimal.ZERO, "Company1", "Bus37", PAN, TripStatus.CANCELLED);

        StringWriter out = new StringWriter();
        writer.write(out, List.of(trip));

        assertThat(out.toString()).contains("$0");
        assertThat(out.toString()).contains("CANCELLED");
    }
}
