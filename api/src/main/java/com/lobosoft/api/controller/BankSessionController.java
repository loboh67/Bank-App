package com.lobosoft.api.controller;

import com.lobosoft.api.dto.BankSessionStatusResponse;
import com.lobosoft.api.service.BankSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/bank-session")
@RequiredArgsConstructor
@Slf4j
public class BankSessionController {

    private final BankSessionService bankSessionService;

    @GetMapping("/status")
    public BankSessionStatusResponse getSessionStatus(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authentication");
        }

        String userId = authentication.getPrincipal().toString();
        if ("anonymousUser".equals(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Anonymous user");
        }

        log.info("User {} -> GET /api/bank-session/status", userId);

        return bankSessionService.getLatestSessionStatus(userId);
    }
}
