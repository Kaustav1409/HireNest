package com.hirenest.backend.util;

import java.sql.SQLException;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Builds human-readable messages from Spring/JDBC integrity failures (logs should still include full stack traces).
 */
public final class DataIntegrityMessageMapper {

    private DataIntegrityMessageMapper() {
    }

    public static String toClientMessage(DataIntegrityViolationException ex, String operationLabel) {
        String blob = collectAllMessages(ex);
        String lower = blob.toLowerCase(Locale.ROOT);

        if (lower.contains("data too long") || lower.contains("too long for column")) {
            return "Value too long for a database column (often company name is still VARCHAR(255)). "
                    + "Shorten the text or run the DB migration to widen columns. Raw: "
                    + truncate(blob, 400);
        }
        if (lower.contains("duplicate") || lower.contains("unique")) {
            if (lower.contains("user_id") || blob.contains("user_id")) {
                return "A recruiter profile already exists for this user (duplicate user_id). "
                        + "The app will update the first matching row; remove duplicate recruiter_profile rows if the error persists. Raw: "
                        + truncate(blob, 400);
            }
            return "Duplicate or unique key violation. Raw: " + truncate(blob, 400);
        }
        if (lower.contains("foreign key") || lower.contains("cannot add or update a child row")) {
            return "Foreign key violation (user reference could not be resolved). Raw: " + truncate(blob, 400);
        }
        if (lower.contains("cannot be null") || lower.contains("not null")
                || (lower.contains("column") && lower.contains("null"))) {
            return "Required column is null in the database. Raw: " + truncate(blob, 400);
        }

        if (blob.isBlank()) {
            return (operationLabel != null ? operationLabel + ": " : "")
                    + "Database constraint failed; JDBC cause chain had no text. Exception types: "
                    + exceptionTypeChain(ex)
                    + ". See server logs (SQLState on SQLException causes).";
        }
        return (operationLabel != null ? operationLabel + ": " : "") + "Database constraint — " + truncate(blob, 500);
    }

    private static String exceptionTypeChain(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        Throwable t = ex;
        int depth = 0;
        while (t != null && depth < 10) {
            if (sb.length() > 0) {
                sb.append(" <- ");
            }
            sb.append(t.getClass().getSimpleName());
            t = t.getCause();
            depth++;
        }
        return sb.toString();
    }

    /** Concatenates non-null messages along the cause chain for pattern matching and logging snippets. */
    public static String collectAllMessages(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        Throwable t = ex;
        int depth = 0;
        while (t != null && depth < 16) {
            if (t.getMessage() != null && !t.getMessage().isBlank()) {
                if (sb.length() > 0) {
                    sb.append(" | ");
                }
                sb.append(t.getMessage());
            }
            if (t instanceof SQLException se) {
                sb.append(" [SQLState=").append(se.getSQLState()).append(", code=").append(se.getErrorCode()).append("]");
            }
            t = t.getCause();
            depth++;
        }
        return sb.toString();
    }

    public static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s == null ? "" : s;
        }
        return s.substring(0, max).trim() + "...";
    }
}
