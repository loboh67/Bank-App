package com.lobosoft.api.service;

import com.lobosoft.api.dto.MerchantDto;
import com.lobosoft.api.dto.TransactionDto;
import com.lobosoft.api.dto.TransactionsPage;
import com.lobosoft.api.model.BankTransaction;
import com.lobosoft.api.model.Merchant;
import com.lobosoft.api.repository.CategoryRepository;
import com.lobosoft.api.repository.BankTransactionRepository;
import com.lobosoft.api.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final BankTransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final MerchantRepository merchantRepository;

    public List<TransactionDto> getTransactionsForAccount(String userId, Long bankAccountId) {
        List<BankTransaction> txs =
                transactionRepository.findByUserIdAndBankAccountIdOrderByBookingDateDescIdDesc(userId, bankAccountId);

        return txs.stream()
                .map(this::toDto)
                .toList();
    }

    public TransactionsPage getTransactionsPageForAccount(String userId, Long bankAccountId, int page, int size) {
        Page<BankTransaction> txPage =
                transactionRepository.findByUserIdAndBankAccountIdOrderByBookingDateDescIdDesc(
                        userId, bankAccountId, PageRequest.of(page, size));

        String nextCursor = txPage.hasNext() ? String.valueOf(txPage.getNumber() + 1) : null;
        return new TransactionsPage(txPage.getContent(), nextCursor);
    }

    public List<TransactionDto> getAllTransactionsForUser(String userId) {
        log.info("Fetching ALL transactions for userId={}", userId);

        return transactionRepository.findByUserIdOrderByBookingDateDescIdDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    public List<TransactionDto> getTransactionsForCategory(String userId,
                                                           Long categoryId,
                                                           LocalDate fromDate,
                                                           LocalDate toDate) {
        assertCategoryExists(categoryId);

        List<BankTransaction> txs = transactionRepository.findByUserIdAndCategoryIdBetweenDates(
                userId, categoryId, fromDate, toDate);

        return txs.stream()
                .map(this::toDto)
                .toList();
    }

    public BigDecimal getTotalSpentForCategory(String userId, Long categoryId) {
        assertCategoryExists(categoryId);
        return transactionRepository.calculateTotalSpentForCategory(userId, categoryId);
    }

    public List<TransactionDto> getTransactionsForMerchant(String userId,
                                                           Long merchantId,
                                                           LocalDate fromDate,
                                                           LocalDate toDate) {
        assertMerchantExists(merchantId);

        List<BankTransaction> txs = transactionRepository.findByUserIdAndMerchantIdBetweenDates(
                userId, merchantId, fromDate, toDate);

        return txs.stream()
                .map(this::toDto)
                .toList();
    }

    public BigDecimal getTotalSpentForMerchant(String userId, Long merchantId) {
        assertMerchantExists(merchantId);
        return transactionRepository.calculateTotalSpentForMerchant(userId, merchantId);
    }

    public List<TransactionDto> getTransactionsForDescriptionRaw(String userId,
                                                                 String descriptionRaw,
                                                                 LocalDate fromDate,
                                                                 LocalDate toDate) {
        List<BankTransaction> txs = transactionRepository.findByUserIdAndDescriptionRawBetweenDates(
                userId, descriptionRaw, fromDate, toDate);

        return txs.stream()
                .map(this::toDto)
                .toList();
    }

    public BigDecimal getTotalSpentForDescriptionRaw(String userId, String descriptionRaw) {
        return transactionRepository.calculateTotalSpentForDescription(userId, descriptionRaw);
    }

    @Transactional
    public TransactionDto updateTransactionDescription(Long transactionId,
                                                       Long bankAccountId,
                                                       String userId,
                                                       String descriptionDisplay) {
        BankTransaction tx = transactionRepository
                .findByIdAndUserIdAndBankAccountId(transactionId, userId, bankAccountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found for user"));

        tx.setDescriptionDisplay(descriptionDisplay);
        BankTransaction saved = transactionRepository.save(tx);

        log.info("Updated description_display for transaction {} (accountId={}, userId={})",
                transactionId, bankAccountId, userId);

        return toDto(saved);
    }

    @Transactional
    public List<TransactionDto> updateDescriptionDisplayForDescriptionRaw(String userId,
                                                                          String descriptionRaw,
                                                                          String descriptionDisplay) {
        int updated = transactionRepository.updateDescriptionDisplayForDescriptionRaw(userId, descriptionRaw, descriptionDisplay);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No transactions found for description_raw");
        }

        log.info("Updated description_display for {} transactions (description_raw={}, userId={})",
                updated, descriptionRaw, userId);

        return transactionRepository.findByUserIdAndDescriptionRawBetweenDates(
                        userId, descriptionRaw, null, null)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private TransactionDto toDto(BankTransaction tx) {
        Merchant merchant = tx.getMerchant();
        MerchantDto merchantDto = merchant == null ? null : new MerchantDto(
                merchant.getId(),
                merchant.getKey(),
                merchant.getName(),
                merchant.getLogoUrl(),
                merchant.getWebsite()
        );

        return new TransactionDto(
                tx.getId(),
                tx.getBankAccountId(),
                tx.getProviderTransactionId(),
                tx.getAmount(),
                tx.getDirection(),
                tx.getCurrency(),
                tx.getBookingDate(),
                tx.getDescriptionRaw(),
                tx.getDescriptionDisplay(),
                merchantDto
        );
    }

    private void assertCategoryExists(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
    }

    private void assertMerchantExists(Long merchantId) {
        if (!merchantRepository.existsById(merchantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found");
        }
    }
}
