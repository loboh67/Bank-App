package com.lobosoft.api.dto.auth;

public record RegisterResponse(
        String accessToken,
        String tokenType
) {
}
