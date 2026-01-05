package com.lobosoft.sync.dto;

public record UpsertResult(
        Long transactionId,
        boolean isNew
) {
}
