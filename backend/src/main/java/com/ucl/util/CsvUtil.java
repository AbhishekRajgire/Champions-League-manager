package com.ucl.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal, dependency-free CSV reader/writer covering the common cases:
 * comma separators, double-quoted fields, escaped quotes ("") and quoted
 * fields containing commas or newlines. Sufficient for the import/export
 * templates this app produces and consumes.
 */
public final class CsvUtil {

    private CsvUtil() {
    }

    /** Parse CSV text into rows of trimmed field values. Blank lines are skipped. */
    public static List<String[]> parse(String content) {
        List<String[]> rows = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return rows;
        }
        // Strip a leading UTF-8 BOM (Excel / Windows often prepend one) so the
        // first field — typically a header like "name" — is recognised cleanly.
        if (!content.isEmpty() && content.charAt(0) == 0xFEFF) {
            content = content.substring(1);
        }
        // Normalise newlines.
        String text = content.replace("\r\n", "\n").replace('\r', '\n');

        List<String> field = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++; // skip escaped quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                field.add(cur.toString());
                cur.setLength(0);
            } else if (c == '\n') {
                field.add(cur.toString());
                cur.setLength(0);
                rows.add(toTrimmedArray(field));
                field.clear();
            } else {
                cur.append(c);
            }
        }
        // Last field / row (no trailing newline).
        if (cur.length() > 0 || !field.isEmpty()) {
            field.add(cur.toString());
            rows.add(toTrimmedArray(field));
        }

        rows.removeIf(CsvUtil::isBlankRow);
        return rows;
    }

    private static String[] toTrimmedArray(List<String> field) {
        return field.stream().map(String::trim).toArray(String[]::new);
    }

    private static boolean isBlankRow(String[] row) {
        for (String s : row) {
            if (s != null && !s.isBlank()) {
                return false;
            }
        }
        return true;
    }

    /** Quote a single field if it contains a comma, quote or newline. */
    public static String escape(Object value) {
        String s = value == null ? "" : value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /** Build one CSV line from the given values, each escaped as needed. */
    public static String line(Object... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(values[i]));
        }
        return sb.append('\n').toString();
    }
}
