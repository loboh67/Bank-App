package com.lobosoft.api.dto;

public record MerchantDto(
        Long id,
        String key,
        String name,
        String logoUrl,
        String website
) {}
