package com.rickm.regex.handler;

import com.rickm.regex.dto.RegexResponse;
import com.rickm.regex.engine.exception.BacktrackLimitExceededException;
import com.rickm.regex.engine.exception.RegexMatchException;
import com.rickm.regex.engine.exception.RegexParseException;
import com.rickm.regex.engine.exception.RegexTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Regex Engine API.
 *
 * Converts exceptions to standardized API error responses.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles regex parse exceptions.
     */
    @ExceptionHandler(RegexParseException.class)
    public ResponseEntity<RegexResponse.ErrorResponse> handleParseException(RegexParseException ex) {
        log.warn("Parse error: {} at position {}", ex.getDetails(), ex.getPosition());

        return ResponseEntity.badRequest().body(RegexResponse.ErrorResponse.builder()
                .error("PARSE_ERROR")
                .message(ex.getDetails())
                .position(ex.getPosition())
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(Instant.now().toString())
                .build());
    }

    /**
     * Handles backtrack limit exceeded exceptions.
     */
    @ExceptionHandler(BacktrackLimitExceededException.class)
    public ResponseEntity<RegexResponse.ErrorResponse> handleBacktrackLimit(BacktrackLimitExceededException ex) {
        log.warn("Backtrack limit exceeded: {} > {}", ex.getActual(), ex.getLimit());

        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(RegexResponse.ErrorResponse.builder()
                .error("BACKTRACK_LIMIT_EXCEEDED")
                .message(String.format("Pattern caused excessive backtracking (%d steps, limit %d). " +
                        "Consider simplifying the pattern.", ex.getActual(), ex.getLimit()))
                .status(HttpStatus.REQUEST_TIMEOUT.value())
                .timestamp(Instant.now().toString())
                .build());
    }

    /**
     * Handles timeout exceptions.
     */
    @ExceptionHandler(RegexTimeoutException.class)
    public ResponseEntity<RegexResponse.ErrorResponse> handleTimeout(RegexTimeoutException ex) {
        log.warn("Regex operation timed out: {} ms > {} ms", ex.getElapsedMs(), ex.getTimeoutMs());

        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(RegexResponse.ErrorResponse.builder()
                .error("TIMEOUT")
                .message(String.format("Operation timed out after %d ms (limit %d ms). " +
                                "Consider simplifying the pattern or using smaller input.",
                        ex.getElapsedMs(), ex.getTimeoutMs()))
                .status(HttpStatus.REQUEST_TIMEOUT.value())
                .timestamp(Instant.now().toString())
                .build());
    }

    /**
     * Handles other regex match exceptions.
     */
    @ExceptionHandler(RegexMatchException.class)
    public ResponseEntity<RegexResponse.ErrorResponse> handleMatchException(RegexMatchException ex) {
        log.warn("Match error: {}", ex.getReason());

        return ResponseEntity.badRequest().body(RegexResponse.ErrorResponse.builder()
                .error("MATCH_ERROR")
                .message(ex.getReason())
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(Instant.now().toString())
                .build());
    }

    /**
     * Handles validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RegexResponse.ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("Validation error: {}", message);

        return ResponseEntity.badRequest().body(RegexResponse.ErrorResponse.builder()
                .error("VALIDATION_ERROR")
                .message(message)
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(Instant.now().toString())
                .build());
    }

    /**
     * Handles illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RegexResponse.ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());

        return ResponseEntity.badRequest().body(RegexResponse.ErrorResponse.builder()
                .error("INVALID_ARGUMENT")
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(Instant.now().toString())
                .build());
    }

    /**
     * Handles unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RegexResponse.ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RegexResponse.ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred. Please try again later.")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(Instant.now().toString())
                .build());
    }

    /**
     * Handles request json parse exceptions.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RegexResponse.ErrorResponse> handleParseException(HttpMessageNotReadableException ex) {
        log.warn("Request Json Parse error: {} with request {}", ex.getMessage(), ex.getHttpInputMessage());

        return ResponseEntity.badRequest().body(RegexResponse.ErrorResponse.builder()
                .error("REQUEST_JSON_PARSE_ERROR")
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(Instant.now().toString())
                .build());
    }
}
