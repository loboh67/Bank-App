package com.lobosoft.api.dto.auth;

public record LoginRequest(
        String email,
        String password
) {
}
