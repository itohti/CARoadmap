package com.caroadmap.data;

import java.util.ArrayList;

public class CsvUtils {

    /**
     * Parses a CSV line into fields, supporting quoted fields and escaped quotes.
     * @param line CSV line
     * @return Array of parsed fields
     */
    public static String[] parseCsvLine(String line) {
        ArrayList<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        // Double quote -> add literal quote
                        current.append('"');
                        i++;
                    } else {
                        // End of quoted field
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }

        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    /**
     * Escapes a string for CSV writing.
     * If the string contains comma, quote, or newline, surrounds it in quotes and escapes quotes.
     * @param input The string to escape
     * @return Escaped CSV field string
     */
    public static String escapeCsv(String input) {
        if (input.contains(",") || input.contains("\"") || input.contains("\n")) {
            input = input.replace("\"", "\"\"");
            return "\"" + input + "\"";
        }
        return input;
    }
}
