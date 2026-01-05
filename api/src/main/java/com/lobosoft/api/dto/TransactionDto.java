package com.lobosoft.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionDto(
        Long id,
        Long bankAccountId,
        String providerTransactionId,
        BigDecimal amount,
        String direction,
        String currency,
        LocalDate bookingDate,
        String descriptionRaw,
        String descriptionDisplay,
        MerchantDto merchant
) {}
