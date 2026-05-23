package com.game.backend.common.api;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
    String code,
    String message,
    OffsetDateTime timestamp,
    List<String> details
) {
    public static ApiErrorResponse of(String code, String message, List<String> details) {
        return new ApiErrorResponse(code, message, OffsetDateTime.now(), details);
    }
}
