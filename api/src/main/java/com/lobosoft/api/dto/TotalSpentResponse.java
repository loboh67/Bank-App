package com.lobosoft.api.dto;

import java.math.BigDecimal;

public record TotalSpentResponse(
        Long categoryId,
        Long merchantId,
        String descriptionRaw,
        BigDecimal totalSpent
) {}
