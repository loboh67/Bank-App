package com.lobosoft.enablebanking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class EnableBankingTransactionsResponse {
    private List<EnableBankingTransaction> transactions;

    @JsonProperty("continuation_key")
    private String continuationKey;
}
