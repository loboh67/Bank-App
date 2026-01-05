package com.lobosoft.enablebanking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StartAuthPayload {

    private Access access;
    private Aspsp aspsp;
    private String state;

    @JsonProperty("redirect_url")
    private String redirectUrl;

    @Data
    @Builder
    public static class Access {
        @JsonProperty("valid_until")
        private String validUntil;
    }

    @Data
    @Builder
    public static class Aspsp {
        private String name;
        private String country;
    }
}
