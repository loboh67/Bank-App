package com.lobosoft.auth.api;

import com.lobosoft.auth.dto.AuthResponse;
import com.lobosoft.auth.dto.LoginRequest;
import com.lobosoft.auth.dto.RegisterRequest;
import com.lobosoft.auth.service.AuthService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<@NonNull AuthResponse> register(@RequestBody RegisterRequest body) {
        return ResponseEntity.ok(authService.register(body));
    }

    @PostMapping("/login")
    public ResponseEntity<@NonNull AuthResponse> login(@RequestBody LoginRequest body) {
        return ResponseEntity.ok(authService.login(body));
    }
}