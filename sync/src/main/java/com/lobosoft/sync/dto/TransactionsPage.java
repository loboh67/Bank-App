package com.lobosoft.sync.dto;

import com.lobosoft.sync.domain.BankTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class TransactionsPage {
    private List<TransactionSummary> transactions;
    private String nextCursor;

    @Data
    @AllArgsConstructor
    public static class TransactionSummary {
        private String providerTransactionId;
        private BigDecimal amount;
        private String currency;
        private String bookingDate; // String: "yyyy-MM-dd"
        private String descriptionRaw;
        private String descriptionDisplay;
        private String direction;
    }
}
