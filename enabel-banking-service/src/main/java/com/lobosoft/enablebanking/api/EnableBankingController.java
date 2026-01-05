package com.lobosoft.enablebanking.api;

import com.lobosoft.enablebanking.dto.AspspDto;
import com.lobosoft.enablebanking.dto.StartAuthResponse;
import com.lobosoft.enablebanking.service.EnableBankingService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/enablebanking")
@RequiredArgsConstructor
@Slf4j
public class EnableBankingController {

    private final EnableBankingService service;

    @GetMapping(value = "/aspsps", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<@NonNull List<AspspDto>> getAspsps(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(required = false) String country,
            @RequestParam(required = false, name = "name") String bankName
    ) {
        if (userId == null || userId.isBlank()) {
            log.warn("Missing X-User-Id header for GET /aspsps");
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header"));
        }

        log.info("User {} -> GET /aspsps country={} bankName={}", userId, country, bankName);

        return service.listAspsps(country, bankName)
                .doOnSubscribe(sub -> log.debug("User {} fetching ASPSPs from provider...", userId))
                .doOnSuccess(r -> log.info("User {} got {} ASPSPs", userId, r.size()))
                .doOnError(e -> log.error("Error in /aspsps for user {}", userId, e));
    }


    @PostMapping(value = "/auth")
    public Mono<@NonNull StartAuthResponse> startAuth(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody AspspDto body
    ) {
        if (userId == null || userId.isBlank()) {
            log.warn("Missing X-User-Id header for POST /enablebanking/auth");
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header"));
        }

        log.info("User {} -> POST /enablebanking/auth bankCountry={}, bankName={}",
                userId, body.getCountry(), body.getName());

        return service.startAuthorizationForUser(body, userId)
                .doOnSubscribe(sub -> log.debug("User {} starting auth with bank {} ({})",
                        userId, body.getName(), body.getCountry()))
                .doOnError(e -> log.error("Error in /auth for user {}", userId, e));
    }


    @GetMapping(value = "/auth/callback")
    public Mono<@NonNull ResponseEntity<Void>> authCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription
    ) {
        return service
                .handleAuthCallBack(code, state, error, errorDescription)
                .map(body -> ResponseEntity
                        .status(HttpStatus.FOUND)
                        .location(buildAppRedirectUri(code, state, error, errorDescription, body))
                        .<Void>build()
                )
                .onErrorResume(ex -> {
                    log.error("Error handling /auth/callback", ex);
                    URI redirect = buildAppRedirectUri(null, state, "server_error", ex.getMessage(), null);
                    return Mono.just(ResponseEntity.status(HttpStatus.FOUND).location(redirect).<Void>build());
                });
    }

    @GetMapping("/accounts/{providerAccountId}/transactions")
    public Mono<@NonNull ResponseEntity<@NonNull JsonNode>> getAccountTransactions (
            @PathVariable String providerAccountId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        log.info("GET /banking/accounts/{}/transactions from={} to={}",
                providerAccountId, from, to);

        return service.getAccountTransactions(providerAccountId, from, to)
                .map(ResponseEntity::ok);
    }

    private URI buildAppRedirectUri(
            String code,
            String state,
            String error,
            String errorDescription,
            JsonNode body
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("bankapp://enablebanking/auth/callback");

        String resolvedState = firstNonBlank(state, body != null ? body.path("state").asString(null) : null);
        String resolvedCode = firstNonBlank(code, body != null ? body.path("code").asString(null) : null);
        String resolvedError = firstNonBlank(error, body != null ? body.path("error").asString(null) : null);
        String resolvedErrorDescription = firstNonBlank(
                errorDescription,
                body != null ? body.path("error_description").asString(null) : null
        );

        if (resolvedState != null && !resolvedState.isBlank()) builder.queryParam("state", resolvedState);
        if (resolvedCode != null && !resolvedCode.isBlank()) builder.queryParam("code", resolvedCode);
        if (resolvedError != null && !resolvedError.isBlank()) builder.queryParam("error", resolvedError);
        if (resolvedErrorDescription != null && !resolvedErrorDescription.isBlank()) {
            builder.queryParam("error_description", resolvedErrorDescription);
        }

        return builder.build(true).toUri();
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }
}
