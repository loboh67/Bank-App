package com.lobosoft.api.dto;

public record BankAccountResponse(
        Long id,
        String name,
        String iban
) {}