package com.lobosoft.api.dto.auth;

public record LoginResponse(
        String accessToken,
        String tokenType
) {
}
