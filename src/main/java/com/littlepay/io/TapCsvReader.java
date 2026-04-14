package com.littlepay.io;

import com.littlepay.model.TapEvent;
import com.littlepay.model.TapType;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TapCsvReader {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public List<TapEvent> read(Reader reader) throws IOException, CsvException {
        try (CSVReader csv = new CSVReader(reader)) {
            List<String[]> rows = csv.readAll();
            // skip header row
            return rows.stream()
                    .skip(1)
                    .map(this::toTapEvent)
                    .toList();
        }
    }

    private TapEvent toTapEvent(String[] row) {
        return new TapEvent(
                Integer.parseInt(trim(row[0])),
                LocalDateTime.parse(trim(row[1]), FORMATTER),
                TapType.valueOf(trim(row[2])),
                trim(row[3]),
                trim(row[4]),
                trim(row[5]),
                trim(row[6])
        );
    }

    private String trim(String value) {
        return value.trim();
    }
}
