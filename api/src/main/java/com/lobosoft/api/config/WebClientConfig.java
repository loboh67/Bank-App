package com.lobosoft.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${services.auth.base-url:http://localhost:8082}")
    private String authServiceBaseUrl;

    @Value("${services.enable-banking.base-url:http://localhost:8081}")
    private String enableBankingBaseUrl;

    @Value("${services.sync.base-url:http://localhost:8080}")
    private String syncServiceBaseUrl;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient enableBankingWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(enableBankingBaseUrl)
                .build();
    }

    @Bean
    public WebClient authWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(authServiceBaseUrl)
                .build();
    }

    @Bean
    public WebClient syncWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(syncServiceBaseUrl)
                .build();
    }
}
