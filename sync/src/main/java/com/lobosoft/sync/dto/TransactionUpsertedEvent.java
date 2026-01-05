package com.lobosoft.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionUpsertedEvent {
    private Long transactionId;
    private String userId;
    private Long accountId;
    private BigDecimal amount;
    private String currency;
    private String direction;
    private String bookingDate;
    private String descriptionRaw;
    private String descriptionDisplay;
}
