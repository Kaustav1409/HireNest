package com.hirenest.backend.config;

import com.hirenest.backend.exception.BadRequestException;
import com.hirenest.backend.exception.NotFoundException;
import com.hirenest.backend.util.DataIntegrityMessageMapper;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return body(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        logDataIntegrityCauseChain(ex);
        log.error("Database constraint violation — aggregated: {}", DataIntegrityMessageMapper.collectAllMessages(ex), ex);
        String clientMessage = DataIntegrityMessageMapper.toClientMessage(ex, "Database");
        return body(HttpStatus.BAD_REQUEST, clientMessage);
    }

    @ExceptionHandler(IncorrectResultSizeDataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleIncorrectResultSize(IncorrectResultSizeDataAccessException ex) {
        log.error("Incorrect result size (expected single row)", ex);
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = "Expected a single database row but multiple (or zero) matched — check for duplicate profile rows.";
        }
        return body(HttpStatus.BAD_REQUEST, msg);
    }

    /**
     * Commit-time failures often surface as TransactionSystemException with a nested
     * {@link DataIntegrityViolationException} (e.g. if something flushed only at boundary).
     */
    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionSystem(TransactionSystemException ex) {
        log.error("TransactionSystemException (often commit/flush integrity failure)", ex);
        DataIntegrityViolationException dive = findFirstCauseOfType(ex, DataIntegrityViolationException.class);
        if (dive != null) {
            logDataIntegrityCauseChain(dive);
            String msg = DataIntegrityMessageMapper.toClientMessage(dive, "Database transaction");
            return body(HttpStatus.BAD_REQUEST, msg);
        }
        Throwable root = ex.getMostSpecificCause();
        String msg = root != null && root.getMessage() != null && !root.getMessage().isBlank()
                ? root.getMessage()
                : ex.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = "Transaction failed (see server logs for root cause).";
        }
        return body(HttpStatus.BAD_REQUEST, msg);
    }

    private static <T extends Throwable> T findFirstCauseOfType(Throwable ex, Class<T> type) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return type.cast(t);
            }
        }
        return null;
    }

    /**
     * Logs the full cause chain so the real JDBC / MySQL error (SQLState, errorCode, message) is visible in logs.
     */
    private void logDataIntegrityCauseChain(DataIntegrityViolationException ex) {
        Throwable t = ex;
        for (int depth = 0; t != null && depth < 12; depth++) {
            String msg = t.getMessage() != null ? t.getMessage() : "";
            log.error("DataIntegrityViolation cause[{}] {}: {}", depth, t.getClass().getName(), msg);
            if (t instanceof SQLException se) {
                log.error("  SQLException SQLState={}, errorCode={}", se.getSQLState(), se.getErrorCode());
            }
            t = t.getCause();
        }
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        log.error("Unhandled runtime exception", ex);
        return body(HttpStatus.BAD_REQUEST, ex.getMessage() == null ? "Unexpected server error" : ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAnyException(Exception ex) {
        log.error("Unhandled exception", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("message", message);
        body.put("error", status.getReasonPhrase());
        return ResponseEntity.status(status).body(body);
    }
}

