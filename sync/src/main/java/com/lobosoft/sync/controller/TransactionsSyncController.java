package com.lobosoft.sync.controller;

import com.lobosoft.sync.dto.TransactionsPage;
import com.lobosoft.sync.service.TransactionSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/sync/accounts")
@RequiredArgsConstructor
public class TransactionsSyncController {

    private final TransactionSyncService transactionSyncService;

    @PostMapping("/{accountId}/transactions/full")
    public void syncFullAccount(@PathVariable Long accountId) {
        transactionSyncService.syncAccountTransactions(accountId);
    }
}
