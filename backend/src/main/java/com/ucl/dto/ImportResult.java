package com.ucl.dto;

import java.util.List;

/**
 * Summary of a CSV import: how many data rows were seen, how many were applied,
 * how many were skipped, plus per-row messages (errors / skips) for feedback.
 */
public record ImportResult(
        int rows,
        int succeeded,
        int skipped,
        List<String> messages
) {
}
