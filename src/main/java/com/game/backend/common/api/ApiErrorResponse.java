package com.game.backend.common.api;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Standard API error payload returned by global exception handling.
 */
public record ApiErrorResponse(
    String code,
    String message,
    OffsetDateTime timestamp,
    List<String> details
) {
    /**
     * Creates an error response with current timestamp.
     */
    public static ApiErrorResponse of(String code, String message, List<String> details) {
        return new ApiErrorResponse(code, message, OffsetDateTime.now(), details);
    }
}
