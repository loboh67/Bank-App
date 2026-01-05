package com.lobosoft.api.service;

import com.lobosoft.api.dto.TransactionCategoryDto;
import com.lobosoft.api.dto.UpdateTransactionCategoriesRequest;
import com.lobosoft.api.model.Category;
import com.lobosoft.api.model.TransactionCategory;
import com.lobosoft.api.repository.BankTransactionRepository;
import com.lobosoft.api.repository.CategoryRepository;
import com.lobosoft.api.repository.TransactionCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionCategoryService {

    private final TransactionCategoryRepository transactionCategoryRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<TransactionCategoryDto> getCategoriesForTransaction(Long transactionId, String userId) {
        if (!bankTransactionRepository.existsByIdAndUserId(transactionId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found for user");
        }

        List<TransactionCategory> categories =
                transactionCategoryRepository.findByTransactionIdAndUserId(transactionId, userId);

        return categories.stream()
                .map(tc -> new TransactionCategoryDto(
                        tc.getId(),
                        tc.getTransactionId(),
                        tc.getCategoryId(),
                        tc.getCategory() != null ? tc.getCategory().getCategoryKey() : null,
                        tc.getCategory() != null ? tc.getCategory().getName() : null,
                        tc.getCategory() != null ? tc.getCategory().getParentId() : null,
                        tc.getConfidence(),
                        tc.getSource(),
                        Boolean.TRUE.equals(tc.getPrimaryCategory())
                ))
                .toList();
    }

    @Transactional
    public List<TransactionCategoryDto> replaceCategoriesForTransaction(
            Long transactionId,
            String userId,
            List<UpdateTransactionCategoriesRequest.CategorySelection> categories
    ) {
        if (!bankTransactionRepository.existsByIdAndUserId(transactionId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found for user");
        }

        if (categories == null || categories.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one category is required");
        }

        Set<Long> categoryIds = new LinkedHashSet<>();
        Long requestedPrimaryId = null;

        for (UpdateTransactionCategoriesRequest.CategorySelection selection : categories) {
            if (selection == null || selection.categoryId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryId is required for each category");
            }
            if (!categoryIds.add(selection.categoryId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Duplicate categoryId: " + selection.categoryId());
            }
            if (Boolean.TRUE.equals(selection.primary())) {
                if (requestedPrimaryId != null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only one category can be primary");
                }
                requestedPrimaryId = selection.categoryId();
            }
        }

        List<Category> foundCategories = categoryRepository.findAllById(categoryIds);
        Set<Long> foundIds = foundCategories.stream().map(Category::getId).collect(Collectors.toSet());
        Set<Long> missingIds = categoryIds.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toSet());
        if (!missingIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown category ids: " + missingIds);
        }

        Long primaryCategoryId = requestedPrimaryId != null
                ? requestedPrimaryId
                : categories.get(0).categoryId();

        transactionCategoryRepository.deleteByTransactionId(transactionId);

        LocalDateTime now = LocalDateTime.now();
        List<TransactionCategory> newCategories = categories.stream()
                .map(selection -> {
                    TransactionCategory tc = new TransactionCategory();
                    tc.setTransactionId(transactionId);
                    tc.setCategoryId(selection.categoryId());
                    tc.setConfidence(BigDecimal.ONE);
                    tc.setSource("manual");
                    tc.setPrimaryCategory(selection.categoryId().equals(primaryCategoryId));
                    tc.setCreatedAt(now);
                    tc.setUpdatedAt(now);
                    return tc;
                })
                .toList();

        transactionCategoryRepository.saveAll(newCategories);

        log.info("User {} replaced categories for transaction {} -> {} entries (primary={})",
                userId, transactionId, newCategories.size(), primaryCategoryId);

        return getCategoriesForTransaction(transactionId, userId);
    }
}
