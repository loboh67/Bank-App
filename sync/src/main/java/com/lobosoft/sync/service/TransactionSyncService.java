package com.lobosoft.sync.service;

import com.lobosoft.enablebanking.grpc.EnableBankingGrpcServiceGrpc;
import com.lobosoft.enablebanking.grpc.GetAccountTransactionsRequest;
import com.lobosoft.enablebanking.grpc.GetAccountTransactionsResponse;
import com.lobosoft.enablebanking.grpc.Transaction;
import com.lobosoft.sync.domain.Account;
import com.lobosoft.sync.domain.AccountRepository;
import com.lobosoft.sync.domain.BankTransaction;
import com.lobosoft.sync.domain.TransactionRepository;
import com.lobosoft.sync.dto.TransactionUpsertedEvent;
import com.lobosoft.sync.dto.UpsertResult;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionSyncService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<@NonNull String, @NonNull TransactionUpsertedEvent> kafkaTemplate;

    private static final String TX_TOPIC = "transactions.upserted";

    @GrpcClient("enable-banking-service")
    private EnableBankingGrpcServiceGrpc.EnableBankingGrpcServiceBlockingStub ebStub;

    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void runPeriodicSync() {
        log.info("[SYNC] Starting transaction sync for all accounts...");

        List<Account> accounts = accountRepository.findActiveAndValid(Instant.now());
        log.info("[SYNC] Found {} active accounts to sync", accounts.size());

        for (Account account : accounts) {
            try {
               syncAccountTransactions(account);
            } catch (Exception e) {
                log.error("[SYNC] Error syncing account id={} providerAccountId={}",
                        account.getId(), account.getProviderAccountId(), e);
            }
        }

        log.info("[SYNC] BankTransaction sync finished");
    }

    public void syncAccountTransactions(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        syncAccountTransactions(account);
    }

    private void syncAccountTransactions(Account account) {
        String userId = account.getUserId();
        Long accountId = account.getId();
        String providerAccountId = account.getProviderAccountId();

        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = determineFromDate(accountId, toDate);

        log.info("[SYNC] Syncing account id={} providerAccountId={} from {} to {}",
                accountId, providerAccountId, fromDate, toDate);

        String continuationKey = null;
        int totalInserted = 0;
        int totalUpdated = 0;
        int page = 1;

        do {
            GetAccountTransactionsRequest.Builder builder = GetAccountTransactionsRequest.newBuilder()
                    .setProviderAccountId(providerAccountId)
                    .setFromDate(fromDate.toString())
                    .setToDate(toDate.toString());

            if (continuationKey != null) {
                builder.setContinuationKey(continuationKey);
            }

            GetAccountTransactionsResponse response = ebStub.getAccountTransactions(builder.build());
            List<Transaction> txs = response.getTransactionsList();

            log.info("[SYNC] Got {} transactions from provider for account {} (page {})",
                    txs.size(), providerAccountId, page);

            int inserted = 0;
            int updated = 0;

            for (Transaction t: txs) {
                UpsertResult result = upsertTransaction(userId, accountId, t);
                if (result.isNew()) {
                    inserted++;
                } else {
                    updated++;
                }

                String description = emptyToNull(t.getDescription());
                TransactionUpsertedEvent event = new TransactionUpsertedEvent(
                        result.transactionId(),
                        userId,
                        accountId,
                        parseAmount(t.getAmount()),
                        t.getCurrency(),
                        t.getDirection(),
                        emptyToNull(t.getBookingDate()),
                        description,
                        description
                );

                kafkaTemplate.send(
                        TX_TOPIC,
                        event.getTransactionId().toString(),
                        event
                );
            }

            totalInserted += inserted;
            totalUpdated += updated;
            continuationKey = normalizeContinuationKey(response.getContinuationKey());
            account.setEbContinuationKey(continuationKey);
            accountRepository.save(account);
            page++;
        } while (continuationKey != null);

        log.info("[SYNC] Account {} fully synced: {} inserted, {} updated",
                providerAccountId, totalInserted, totalUpdated);
    }

    private LocalDate determineFromDate(Long accountId, LocalDate fallbackTo) {
        Optional<BankTransaction> latestOpt =
                transactionRepository.findTopByBankAccountIdOrderByBookingDateDesc(accountId);

        return latestOpt
                .map(BankTransaction::getBookingDate)
                .map(d -> d.plusDays(1)) // start after last known booking date
                .orElse(fallbackTo.minusDays(30)); // or last 30 days if no history
    }



    private UpsertResult upsertTransaction(String userId, Long accountId,  Transaction t) {

        String providerTransactionId = t.getProviderTransactionId();
        Optional<BankTransaction> existingOpt =
                transactionRepository.findByBankAccountIdAndProviderTransactionId(
                        accountId,
                        providerTransactionId
                );

        String amountStr = t.getAmount();
        t.getAmount();
        BigDecimal amount = amountStr.isBlank()
            ? BigDecimal.ZERO
            : new BigDecimal(amountStr);

        BankTransaction entity = existingOpt.orElseGet(BankTransaction::new);
        boolean isNew = entity.getId() == null;

        entity.setUserId(userId);
        entity.setBankAccountId(accountId);
        entity.setProviderTransactionId(providerTransactionId);
        entity.setAmount(amount);
        entity.setCurrency(t.getCurrency());
        entity.setDirection(t.getDirection());

        String bookingDateStr = t.getBookingDate();
        if (!bookingDateStr.isBlank()) {
            entity.setBookingDate(LocalDate.parse(bookingDateStr));
        } else {
            entity.setBookingDate(null);
        }

        String valueDateStr = t.getValueDate();
        if (!valueDateStr.isBlank()) {
            entity.setValueDate(LocalDate.parse(valueDateStr));
        } else {
            entity.setValueDate(null);
        }

        String description = emptyToNull(t.getDescription());
        entity.setDescriptionRaw(description);
        entity.setDescriptionDisplay(null);
        entity.setRawJson(t.getRawJson());

        Instant now = Instant.now();
        if (isNew) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);

        entity = transactionRepository.save(entity);
        return new UpsertResult(entity.getId(), isNew);
    }

    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isBlank()) return BigDecimal.ZERO;
        return new BigDecimal(amountStr);
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private String normalizeContinuationKey(String key) {
        return (key == null || key.isBlank()) ? null : key;
    }
}
