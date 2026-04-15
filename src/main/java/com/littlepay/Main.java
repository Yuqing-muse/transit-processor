package com.littlepay;

import com.littlepay.io.TapCsvReader;
import com.littlepay.io.TripCsvWriter;
import com.littlepay.model.TapEvent;
import com.littlepay.model.Trip;
import com.littlepay.service.FareCalculator;
import com.littlepay.service.TripProcessor;

import com.opencsv.exceptions.CsvException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        String inputPath  = args.length > 0 ? args[0] : "tap-events.csv";
        String outputPath = args.length > 1 ? args[1] : "trip-results.csv";

        try {
            TripProcessor tripProcessor = new TripProcessor(new FareCalculator(FareCalculator.DEFAULT_RULES));

            List<TapEvent> taps  = new TapCsvReader().read(new FileReader(inputPath));
            List<Trip> trips = tripProcessor.process(taps);

            try (FileWriter out = new FileWriter(outputPath)) {
                new TripCsvWriter().write(out, trips);
            }

            System.out.printf("Processed %d taps, %d trips written to %s%n", taps.size(), trips.size(), outputPath);
        } catch (FileNotFoundException e) {
            System.err.println("Error: input file not found: " + inputPath);

            System.exit(1);
        } catch (IOException | CsvException e) {
            System.err.println("Error: " + e.getMessage());

            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: illegal argument: " + e.getMessage());

            System.exit(1);
        }
    }
}
