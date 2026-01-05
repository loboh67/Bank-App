package com.lobosoft.api.dto.auth;

public record RegisterRequest(
        String email,
        String password
) {
}
