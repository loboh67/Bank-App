package com.lobosoft.api.controller;

import com.lobosoft.api.dto.auth.LoginRequest;
import com.lobosoft.api.dto.auth.LoginResponse;
import com.lobosoft.api.dto.auth.RegisterRequest;
import com.lobosoft.api.dto.auth.RegisterResponse;
import com.lobosoft.api.service.AuthGatewayService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthGatewayService authGatewayService;

    @PostMapping("/register")
    public Mono<@NonNull RegisterResponse> register(@RequestBody RegisterRequest request) {
        log.info("API -> POST /api/auth/register email={}", request.email());
        return authGatewayService.register(request);
    }

    @PostMapping("/login")
    public Mono<@NonNull LoginResponse> login(@RequestBody LoginRequest request) {
        log.info("API -> POST /api/auth/login email={}", request.email());
        return authGatewayService.login(request);
    }
}
