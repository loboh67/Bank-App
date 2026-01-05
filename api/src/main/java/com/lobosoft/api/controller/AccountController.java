package com.lobosoft.api.controller;

import com.lobosoft.api.dto.BankAccountResponse;
import com.lobosoft.api.service.BankAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountController {

    private final BankAccountService bankAccountService;

    @GetMapping("")
    public List<BankAccountResponse> getUserAccounts(Authentication authentication) {
        String userId = Objects.requireNonNull(authentication.getPrincipal()).toString();
        return bankAccountService.listUserAccounts(userId);
    }

    @PostMapping("/{accountId}/reset-sync")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetAccountSync(@PathVariable("accountId") Long accountId, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Unauthenticated");
        }

        String userId = authentication.getPrincipal().toString();

        log.info("User {} requested sync reset for account {}", userId, accountId);

        bankAccountService.resetAccountSync(userId, accountId);
    }

}
