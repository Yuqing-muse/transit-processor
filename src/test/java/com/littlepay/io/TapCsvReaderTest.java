package com.littlepay.io;

import com.littlepay.model.TapEvent;
import com.littlepay.model.TapType;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TapCsvReaderTest {

    private final TapCsvReader reader = new TapCsvReader();

    @Test
    void shouldParseValidRow() throws Exception {
        String csv = """
                ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN
                1, 22-01-2023 13:00:00, ON, Stop1, Company1, Bus37, 5454545454545454
                """;

        List<TapEvent> taps = reader.read(new StringReader(csv));

        assertThat(taps).hasSize(1);
        TapEvent tap = taps.get(0);
        assertThat(tap.id()).isEqualTo(1);
        assertThat(tap.dateTimeUTC()).isEqualTo(LocalDateTime.of(2023, 1, 22, 13, 0, 0));
        assertThat(tap.tapType()).isEqualTo(TapType.ON);
        assertThat(tap.stopId()).isEqualTo("Stop1");
        assertThat(tap.companyId()).isEqualTo("Company1");
        assertThat(tap.busId()).isEqualTo("Bus37");
        assertThat(tap.pan()).isEqualTo("5454545454545454");
    }

    @Test
    void shouldTrimWhitespaceFromAllFields() throws Exception {
        String csv = """
                ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN
                 2 ,  22-01-2023 13:05:00 ,  OFF ,  Stop2 ,  Company1 ,  Bus37 ,  4444333322221111\s
                """;

        List<TapEvent> taps = reader.read(new StringReader(csv));

        assertThat(taps).hasSize(1);
        TapEvent tap = taps.get(0);
        assertThat(tap.stopId()).isEqualTo("Stop2");
        assertThat(tap.tapType()).isEqualTo(TapType.OFF);
        assertThat(tap.pan()).isEqualTo("4444333322221111");
    }

    @Test
    void shouldSkipHeaderRow() throws Exception {
        String csv = """
                ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN
                1, 22-01-2023 13:00:00, ON, Stop1, Company1, Bus37, 5454545454545454
                2, 22-01-2023 13:05:00, OFF, Stop2, Company1, Bus37, 5454545454545454
                """;

        List<TapEvent> taps = reader.read(new StringReader(csv));

        assertThat(taps).hasSize(2);
    }
}
