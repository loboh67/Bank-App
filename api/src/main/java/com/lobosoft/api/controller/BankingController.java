package com.lobosoft.api.controller;

import com.lobosoft.api.dto.StartAuthRequest;
import com.lobosoft.api.dto.StartAuthResponse;
import com.lobosoft.api.service.BankingLinkService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class BankingController {

    private final BankingLinkService bankingLinkService;

    @PostMapping("/auth")
    public Mono<@NonNull StartAuthResponse> startAuth(
            Authentication authentication,
            @RequestBody StartAuthRequest request
    ) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authentication");
        }

        String userId = authentication.getPrincipal().toString();
        log.info("BankingController.startAuth called, userId={}", userId);

        UUID userUuid = parseUserUuid(userId);

        if (request == null) {
            log.warn("Missing request body for api/auth");
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required"));
        }

        return bankingLinkService.startAuth(userUuid, request)
                .doOnSuccess(body ->
                        log.debug("Returning 200 with body: {}", body));
    }

    private UUID parseUserUuid(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            log.warn("Authenticated principal is not a UUID: {}", userId);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user id");
        }
    }
}
