package com.cropplanner.model;

/** Implemented by every record that is saved to a CSV file. */
public interface CsvSerializable {

    String toCsv();

    /** Commas and line breaks would corrupt a CSV row, so they are replaced by spaces. */
    static String clean(String value) {
        return value.replace(',', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
