package com.lobosoft.enablebanking.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private static final int MAX_LOG_BODY_CHARS = 4_096;
    private static final int MAX_RESPONSE_BODY_CHARS = 1_024;

    private final ObjectMapper objectMapper;

    @ExceptionHandler(WebClientResponseException.class)
    public Mono<ResponseEntity<ObjectNode>> handleWebClientResponseException(
            WebClientResponseException ex,
            ServerWebExchange exchange
    ) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String method = exchange.getRequest().getMethod() != null
                ? exchange.getRequest().getMethod().name()
                : "UNKNOWN";
        String path = exchange.getRequest().getURI().getPath();

        String providerBody = safeTruncate(ex.getResponseBodyAsString(), MAX_LOG_BODY_CHARS);
        log.warn(
                "User {} upstream error during {} {} -> status={} body={}",
                userId,
                method,
                path,
                ex.getStatusCode().value(),
                providerBody
        );

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("error", "enablebanking_upstream_error");
        payload.put("status", ex.getStatusCode().value());
        payload.put("message", "Enable Banking API request failed");

        String responseBody = safeTruncate(ex.getResponseBodyAsString(), MAX_RESPONSE_BODY_CHARS);
        if (responseBody != null && !responseBody.isBlank()) {
            payload.put("upstream_body", responseBody);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return Mono.just(new ResponseEntity<>(payload, headers, ex.getStatusCode()));
    }

    @ExceptionHandler(WebClientRequestException.class)
    public Mono<ResponseEntity<ObjectNode>> handleWebClientRequestException(
            WebClientRequestException ex,
            ServerWebExchange exchange
    ) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String method = exchange.getRequest().getMethod() != null
                ? exchange.getRequest().getMethod().name()
                : "UNKNOWN";
        String path = exchange.getRequest().getURI().getPath();

        log.warn("User {} upstream network error during {} {}: {}", userId, method, path, ex.getMessage());

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("error", "enablebanking_upstream_unavailable");
        payload.put("status", HttpStatus.BAD_GATEWAY.value());
        payload.put("message", "Enable Banking API is unreachable");

        return Mono.just(
                ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
        );
    }

    private static String safeTruncate(String value, int maxChars) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.length() <= maxChars) return trimmed;
        return trimmed.substring(0, maxChars) + "...(truncated)";
    }
}
