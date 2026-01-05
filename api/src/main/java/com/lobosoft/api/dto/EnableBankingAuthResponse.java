package com.lobosoft.api.dto;

public record EnableBankingAuthResponse(
        String psu_id_hash,
        String redirect_url,
        String url
) {}
