package com.lobosoft.api.dto;

public record UpdateTransactionsDescriptionRequest(
        String descriptionRaw,
        String descriptionDisplay
) {
}
