# Transit Processor

A Java application that processes transit tap events (ON/OFF) from a CSV file and produces a trip report.

## Assumptions

1. **Matching key is PAN + BusID.** A tap OFF is matched to the most recent unmatched tap ON for the same PAN on the same bus.
2. **Taps are sorted by timestamp before processing.** Input order is not assumed to be chronological.
3. **Orphaned tap OFFs are ignored.** A tap OFF with no preceding ON for the same PAN+BusID produces no trip.
4. **Double tap ON closes the first as INCOMPLETE.** A second ON for the same PAN+BusID always supersedes the first.
5. **StopId, CompanyId, and BusId are strings.** The model accepts both human-readable labels and opaque numeric identifiers.
6. **PAN is stored as a string.** Card numbers may have leading zeros, no arithmetic is performed on them, and 16-digit values can overflow `Long`.
7. **Input file is assumed well-formed.** No validation required.

## Features

- Build `COMPLETED`, `INCOMPLETE`, and `CANCELLED` trip records
- Configurable fare table via injectable `List<FareRule>` - supports asymmetric fares and future extension
- PAN+BusID matching key
- Input-order independent - taps are sorted by timestamp before processing
- Ignore orphaned tap OFFs; double tap ONs resolved to max-fare INCOMPLETE
- Use Worldpay standard test PANs in test suite

## Requirements

- Java 17+
- Gradle (or use the included `./gradlew` wrapper)

## Project Structure

```
src/
  main/java/com/littlepay/
    model/         : TapEvent, TapType, Trip, TripStatus, FareRule
    service/       : FareCalculator, TripProcessor (business logic)
    io/            : TapCsvReader, TripCsvWriter (I/O)
    Main.java      : entry point
  main/resources/
    tap-events.csv : example input file
  test/java/com/littlepay/
    service/       : FareCalculatorTest, TripProcessorTest
    io/            : TapCsvReaderTest, TripCsvWriterTest
    TransitProcessorIntegrationTest.java
```

## How to Build

```bash
./gradlew build
```

## How to Run

```bash
./gradlew run --args="<input-file> <output-file>"
```

**Example** (uses the provided sample input):

```bash
./gradlew run --args="src/main/resources/tap-events.csv trip-results.csv"
```

If no arguments are provided, defaults to `tap-events.csv`, `trip-results.csv`.

## How to Test

```bash
./gradlew test
```

## Input Format (`tap-events.csv`)

```
ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN
1, 22-01-2023 13:00:00, ON, Stop1, Company1, Bus37, 5454545454545454
2, 22-01-2023 13:05:00, OFF, Stop2, Company1, Bus37, 5454545454545454
```

- Timestamps use format `dd-MM-yyyy HH:mm:ss`
- `TapType` is `ON` or `OFF`
- Leading/trailing whitespace in fields is trimmed

## Output Format (`trip-results.csv`)

```
Started,Finished,DurationSecs,FromStopId,ToStopId,ChargeAmount,CompanyId,BusID,PAN,Status
22-01-2023 13:00:00,22-01-2023 13:05:00,300,Stop1,Stop2,$3.25,Company1,Bus37,5454545454545454,COMPLETED
```

- `Status` is one of: `COMPLETED`, `INCOMPLETE`, `CANCELLED`
- `INCOMPLETE` trips have empty `Finished` and `ToStopId`, and `DurationSecs` of `0`
- `CANCELLED` trips (tap ON and OFF at same stop) have `ChargeAmount` of `$0.00`

## Error Handling

The program exits with code 1 and prints a message for the following failures:

| Condition                  | Message |
|----------------------------|---|
| Input file not found       | `Error: input file not found: <path>` |
| I/O or CSV parse failure   | `Error: <detail>` |
| Unknown data in fare table | `Error: illegal argument: <detail>` |

Malformed CSV rows are not validated (assumption).
