package com.littlepay.io;

import com.littlepay.model.Trip;
import com.littlepay.model.TripStatus;
import com.opencsv.CSVWriter;

import java.io.IOException;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TripCsvWriter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private static final String[] HEADER = {
            "Started", "Finished", "DurationSecs", "FromStopId", "ToStopId",
            "ChargeAmount", "CompanyId", "BusID", "PAN", "Status"
    };

    public void write(Writer writer, List<Trip> trips) throws IOException {
        try (CSVWriter csv = new CSVWriter(writer,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {
            csv.writeNext(HEADER);
            for (Trip trip : trips) {
                csv.writeNext(toRow(trip));
            }
        }
    }

    private String[] toRow(Trip trip) {
        return new String[]{
                trip.started().format(FORMATTER),
                trip.finished() != null ? trip.finished().format(FORMATTER) : "",
                String.valueOf(trip.durationSecs()),
                trip.fromStopId(),
                trip.toStopId() != null ? trip.toStopId() : "",
                "$" + trip.chargeAmount().setScale(2),
                trip.companyId(),
                trip.busId(),
                trip.pan(),
                trip.status().name()
        };
    }
}
