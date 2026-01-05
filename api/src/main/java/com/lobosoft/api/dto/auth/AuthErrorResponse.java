package com.lobosoft.api.dto.auth;

public record AuthErrorResponse(
        String code,
        String message
) {
}
