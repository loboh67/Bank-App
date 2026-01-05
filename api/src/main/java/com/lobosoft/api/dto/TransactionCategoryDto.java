package com.lobosoft.api.dto;

import java.math.BigDecimal;

public record TransactionCategoryDto(
        Long id,
        Long transactionId,
        Long categoryId,
        String categoryKey,
        String categoryName,
        Long parentCategoryId,
        BigDecimal confidence,
        String source,
        boolean primary
) {
}
