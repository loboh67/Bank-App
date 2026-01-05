package com.lobosoft.api.service;

import com.lobosoft.api.dto.auth.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthGatewayService {

    private final WebClient authWebClient;

    public Mono<@NonNull RegisterResponse> register(RegisterRequest request) {
        log.info("AuthGatewayService.register -> calling Auth Service /auth/register email={}", request.email());

        return authWebClient.post()
                .uri("/auth/register")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.value() == HttpStatus.CONFLICT.value(),
                        response -> response.bodyToMono(AuthErrorResponse.class)
                                .defaultIfEmpty(new AuthErrorResponse("UNKNOWN", "Unknown error"))
                                .flatMap(err -> {
                                    log.warn("Auth Service returned 409: code={} message={}",
                                            err.code(), err.message());
                                    return Mono.error(new EmailAlreadyRegisteredApiException(err.message()));
                                })
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("Auth service error")
                                .flatMap(body -> {
                                    log.error("Auth Service 5xx: {}", body);
                                    return Mono.error(new RuntimeException("Auth service unavailable"));
                                })
                )
                .bodyToMono(RegisterResponse.class)
                .doOnError(ex -> log.error("Auth Service /auth/register failed for email {}: {}",
                        request.email(), ex.getMessage(), ex));
    }

    public Mono<@NonNull LoginResponse> login(LoginRequest request) {
        return authWebClient.post()
                .uri("/auth/login")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, res ->
                        res.bodyToMono(String.class).defaultIfEmpty("Bad credentials")
                                .flatMap(body -> {
                                    log.warn("AuthService /auth/login 4xx: {}", body);
                                    return Mono.error(new WebClientResponseException(
                                            "Login failed: " + body,
                                            res.statusCode().value(), res.statusCode().toString(),
                                            null, null, null
                                    ));
                                }))
                .onStatus(HttpStatusCode::is5xxServerError, res ->
                        res.bodyToMono(String.class).defaultIfEmpty("Server error")
                                .flatMap(body -> {
                                    log.error("AuthService /auth/login 5xx: {}", body);
                                    return Mono.error(new WebClientResponseException(
                                            "Auth service error: " + body,
                                            res.statusCode().value(), res.statusCode().toString(),
                                            null, null, null
                                    ));
                                }))
                .bodyToMono(LoginResponse.class)
                .doOnError(ex -> log.error("Auth Service /auth/login failed for email {}: {}",
                        request.email(), ex.getMessage(), ex));
    }
}
