package com.lobosoft.api.dto;

import com.lobosoft.api.model.BankTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class TransactionsPage {
    private List<BankTransaction> transactions;
    private String nextCursor;

    @Data
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
