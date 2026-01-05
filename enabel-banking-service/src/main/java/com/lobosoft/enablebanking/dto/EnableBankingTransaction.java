package com.lobosoft.enablebanking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class EnableBankingTransaction {

    @JsonProperty("entry_reference")
    private String entryReference;

    @JsonProperty("transaction_amount")
    private TransactionAmount transactionAmount;

    @JsonProperty("credit_debit_indicator")
    private String creditDebitIndicator; // "DBIT" / "CRDT"

    @JsonProperty("status")
    private String status; // "BOOK", "PDNG", ...

    @JsonProperty("booking_date")
    private String bookingDate; // "2025-12-04"

    @JsonProperty("value_date")
    private String valueDate;

    @JsonProperty("transaction_date")
    private String transactionDate;

    @JsonProperty("remittance_information")
    private List<String> remittanceInformation;

    @JsonProperty("note")
    private String note;

    @JsonProperty("transaction_id")
    private String transactionId;

    @Data
    public static class TransactionAmount {
        private String currency; // "EUR"
        private String amount;   // "8.99" (string vinda da API)
    }
}
