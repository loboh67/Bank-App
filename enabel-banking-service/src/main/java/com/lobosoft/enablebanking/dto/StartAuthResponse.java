package com.lobosoft.enablebanking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StartAuthResponse {

    private String url;

    @JsonProperty("redirect_url")
    private String redirectUrl;

    @JsonProperty("psu_id_hash")
    private String psuIdHash;
}
