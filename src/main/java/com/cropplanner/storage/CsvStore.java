package com.cropplanner.storage;

import com.cropplanner.model.CsvSerializable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Reads and writes one CSV file of any record type.
 *
 * @param <T> record type; it knows how to turn itself into a CSV row
 */
public class CsvStore<T extends CsvSerializable> {

    private final String path;
    private final String header;
    private final Function<String[], T> parser;

    public CsvStore(String path, String header, Function<String[], T> parser) {
        this.path = path;
        this.header = header;
        this.parser = parser;
    }

    /** Bad rows are reported and skipped so one typo cannot block the whole file. */
    public List<T> load() throws IOException {
        List<T> items = new ArrayList<>();
        File file = new File(path);
        if (!file.exists()) {
            return items;
        }
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            in.readLine();                                  // skip header
            String line;
            int lineNo = 1;
            while ((line = in.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    items.add(parser.apply(line.split(",", -1)));
                } catch (IllegalArgumentException | DateTimeException e) {
                    System.out.println("Skipping line " + lineNo + " of " + path + ": " + e.getMessage());
                }
            }
        }
        return items;
    }

    public void save(Collection<T> items) throws IOException {
        File parent = new File(path).getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (BufferedWriter out = new BufferedWriter(new FileWriter(path))) {
            out.write(header);
            out.newLine();
            for (T item : items) {
                out.write(item.toCsv());
                out.newLine();
            }
        }
    }
}
