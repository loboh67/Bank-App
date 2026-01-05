package com.lobosoft.api.controller;

import com.lobosoft.api.client.SyncServiceClient;
import com.lobosoft.api.dto.TransactionCategoryDto;
import com.lobosoft.api.dto.TransactionDto;
import com.lobosoft.api.dto.TransactionsPage;
import com.lobosoft.api.dto.UpdateTransactionCategoriesRequest;
import com.lobosoft.api.dto.UpdateTransactionDescriptionRequest;
import com.lobosoft.api.dto.UpdateTransactionsDescriptionRequest;
import com.lobosoft.api.dto.TotalSpentResponse;
import com.lobosoft.api.repository.BankAccountRepository;
import com.lobosoft.api.service.BankAccountService;
import com.lobosoft.api.service.TransactionCategoryService;
import com.lobosoft.api.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;
    private final SyncServiceClient syncServiceClient;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionCategoryService transactionCategoryService;
    private final BankAccountService bankAccountService;

    @GetMapping("/accounts/{accountId}/transactions")
    public List<TransactionDto> getAccountTransactions(
            @PathVariable("accountId") Long accountId,
            Authentication authentication
    ) {
        String userId = getUserId(authentication);
        assertAccountOwnership(accountId, userId);
        log.info("User {} -> GET /accounts/{}/transactions", userId, accountId);
        return transactionService.getTransactionsForAccount(userId, accountId);
    }

    @GetMapping("/transactions")
    public List<TransactionDto> getAllUserTransactions(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authentication");
        }

        Object principal = auth.getPrincipal();
        String userId = principal.toString();
        if ("anonymousUser".equals(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Anonymous user");
        }

        log.info("User {} -> GET /transactions", userId);
        return transactionService.getAllTransactionsForUser(userId);
    }

    @GetMapping("/categories/{categoryId}/transactions")
    public List<TransactionDto> getTransactionsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication auth
    ) {
        String userId = getUserId(auth);
        validateDateRange(fromDate, toDate);
        log.info("User {} -> GET /categories/{}/transactions (from={}, to={})", userId, categoryId, fromDate, toDate);

        return transactionService.getTransactionsForCategory(userId, categoryId, fromDate, toDate);
    }

    @GetMapping("/categories/{categoryId}/transactions/total-spent")
    public TotalSpentResponse getTotalSpentByCategory(
            @PathVariable Long categoryId,
            Authentication auth
    ) {
        String userId = getUserId(auth);
        log.info("User {} -> GET /categories/{}/transactions/total-spent", userId, categoryId);
        return new TotalSpentResponse(categoryId, null, null,
                transactionService.getTotalSpentForCategory(userId, categoryId));
    }

    @GetMapping("/merchants/{merchantId}/transactions")
    public List<TransactionDto> getTransactionsByMerchant(
            @PathVariable Long merchantId,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication auth
    ) {
        String userId = getUserId(auth);
        validateDateRange(fromDate, toDate);
        log.info("User {} -> GET /merchants/{}/transactions (from={}, to={})", userId, merchantId, fromDate, toDate);

        return transactionService.getTransactionsForMerchant(userId, merchantId, fromDate, toDate);
    }

    @GetMapping("/merchants/{merchantId}/transactions/total-spent")
    public TotalSpentResponse getTotalSpentByMerchant(
            @PathVariable Long merchantId,
            Authentication auth
    ) {
        String userId = getUserId(auth);
        log.info("User {} -> GET /merchants/{}/transactions/total-spent", userId, merchantId);
        return new TotalSpentResponse(null, merchantId, null,
                transactionService.getTotalSpentForMerchant(userId, merchantId));
    }

    @GetMapping("/transactions/description")
    public List<TransactionDto> getTransactionsByDescriptionRaw(
            @RequestParam("q") String descriptionRaw,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication auth
    ) {
        String userId = getUserId(auth);
        validateDateRange(fromDate, toDate);
        if (descriptionRaw == null || descriptionRaw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "q (description_raw) is required");
        }

        log.info("User {} -> GET /transactions/description (q={}, from={}, to={})",
                userId, descriptionRaw, fromDate, toDate);

        return transactionService.getTransactionsForDescriptionRaw(userId, descriptionRaw, fromDate, toDate);
    }

    @GetMapping("/transactions/description/total-spent")
    public TotalSpentResponse getTotalSpentByDescriptionRaw(
            @RequestParam("q") String descriptionRaw,
            Authentication auth
    ) {
        String userId = getUserId(auth);
        if (descriptionRaw == null || descriptionRaw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "q (description_raw) is required");
        }

        log.info("User {} -> GET /transactions/description/total-spent (q={})", userId, descriptionRaw);
        return new TotalSpentResponse(null, null, descriptionRaw,
                transactionService.getTotalSpentForDescriptionRaw(userId, descriptionRaw));
    }

    @PatchMapping("/transactions/description")
    public List<TransactionDto> updateDescriptionDisplayForDescriptionRaw(
            @RequestBody UpdateTransactionsDescriptionRequest request,
            Authentication auth
    ) {
        String userId = getUserId(auth);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (request.descriptionRaw() == null || request.descriptionRaw().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "descriptionRaw is required");
        }
        if (request.descriptionDisplay() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "descriptionDisplay is required");
        }

        log.info("User {} -> PATCH /transactions/description (description_raw={})", userId, request.descriptionRaw());
        return transactionService.updateDescriptionDisplayForDescriptionRaw(
                userId, request.descriptionRaw(), request.descriptionDisplay());
    }

    @GetMapping("/transactions/{transactionId}/categories")
    public List<TransactionCategoryDto> getTransactionCategories(
            @PathVariable Long transactionId,
            Authentication auth
    ) {
        String userId = getUserId(auth);
        log.info("User {} -> GET /transactions/{}/categories", userId, transactionId);

        return transactionCategoryService.getCategoriesForTransaction(transactionId, userId);
    }

    @PutMapping("/transactions/{transactionId}/categories")
    public List<TransactionCategoryDto> replaceTransactionCategories(
            @PathVariable Long transactionId,
            @RequestBody UpdateTransactionCategoriesRequest request,
            Authentication auth
    ) {
        String userId = getUserId(auth);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        log.info("User {} -> PUT /transactions/{}/categories ({} categories)", userId, transactionId,
                request.categories() == null ? 0 : request.categories().size());

        return transactionCategoryService.replaceCategoriesForTransaction(
                transactionId,
                userId,
                request.categories()
        );
    }

    @PostMapping("/accounts/{accountId}/transactions/fetch-all")
    public List<TransactionDto> fetchAll(
            @PathVariable Long accountId,
            Authentication auth
    ) {
        String userId = getUserId(auth);
        assertAccountOwnership(accountId, userId);

        log.info("User {} -> POST /accounts/{}/transactions/fetch-all (reset continuation and full fetch)", userId, accountId);

        bankAccountService.resetAccountSync(userId, accountId);

        TransactionsPage page;
        do {
            String continuationKey = bankAccountService.getContinuationKey(userId, accountId);
            log.info("User {} -> fetching next page for account {} (continuationKey={})", userId, accountId, continuationKey);

            page = syncServiceClient.requestNextPage(accountId, userId);

            if (page == null) {
                log.warn("User {} -> sync returned null page for account {}", userId, accountId);
                break;
            }

            log.info("User {} -> received {} transactions, nextCursor={}", userId,
                    page.getTransactions() == null ? 0 : page.getTransactions().size(),
                    page.getNextCursor());
        } while (page.getNextCursor() != null);

        return transactionService.getTransactionsForAccount(userId, accountId);
    }

    @PostMapping("/accounts/{accountId}/transactions/full-sync")
    public ResponseEntity<Void> triggerFullSync(
            @PathVariable Long accountId,
            Authentication auth
    ) {
        String userId = getUserId(auth);
        assertAccountOwnership(accountId, userId);

        log.info("User {} -> POST /accounts/{}/transactions/full-sync (manual trigger)", userId, accountId);
        try {
            syncServiceClient.triggerFullSync(accountId, userId);
            log.info("Full-sync accepted for user {} account {}", userId, accountId);
        } catch (Exception ex) {
            log.error("Full-sync failed for user {} account {}: {}", userId, accountId, ex.toString(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Full-sync failed");
        }

        return ResponseEntity.accepted().build();
    }

    @PatchMapping("/accounts/{accountId}/transactions/{transactionId}/description")
    public TransactionDto updateTransactionDescription(
            @PathVariable Long accountId,
            @PathVariable Long transactionId,
            @RequestBody UpdateTransactionDescriptionRequest request,
            Authentication auth
    ) {
        String userId = getUserId(auth);
        assertAccountOwnership(accountId, userId);

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        log.info("User {} -> PATCH /accounts/{}/transactions/{}/description", userId, accountId, transactionId);
        return transactionService.updateTransactionDescription(
                transactionId,
                accountId,
                userId,
                request.descriptionDisplay()
        );
    }

    private String getUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authentication");
        }
        String userId = auth.getPrincipal().toString();
        if ("anonymousUser".equals(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Anonymous user");
        }
        return userId;
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "`from` must be before or equal to `to`");
        }
    }

    private void assertAccountOwnership(Long accountId, String userId) {
        if (!bankAccountRepository.existsByIdAndUserId(accountId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found for user");
        }
    }
}
