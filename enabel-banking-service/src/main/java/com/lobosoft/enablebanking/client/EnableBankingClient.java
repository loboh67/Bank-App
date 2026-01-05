package com.lobosoft.enablebanking.client;

import com.lobosoft.enablebanking.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnableBankingClient {

    private final WebClient enableBankingWebClient;

    public Mono<@NonNull List<AspspDto>> getAspsps(String country) {
        return enableBankingWebClient.get()
                .uri(uriBuilder -> {
                        var b = uriBuilder.path("/aspsps");
                        if (country != null) {
                            b.queryParam("country", country);
                        }
                        return b.build();
                })
                .retrieve()
                .bodyToMono(AspspsResponse.class)
                .map(AspspsResponse::getAspsps);
    }

    public Mono<@NonNull StartAuthResponse> startAuthorization(StartAuthPayload payload) {
        return enableBankingWebClient.post()
                .uri("/auth")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(StartAuthResponse.class);
    }

    public Mono<@NonNull JsonNode> exchangeCodeForSession(String code) {
        Map<String, Object> body = Map.of("code", code);

        return enableBankingWebClient.post()
                .uri("/sessions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<@NonNull JsonNode> getAccountTransactions(
            String providerAccountId,
            String dateFrom,
            String dateTo
    ) {
        return enableBankingWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/accounts/{id}/transactions")
                        .queryParamIfPresent("date_from", Optional.ofNullable(dateFrom))
                        .queryParamIfPresent("date_to", Optional.ofNullable(dateTo))
                        .build(providerAccountId))
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<@NonNull EnableBankingTransactionsResponse> fetchTransactions(
            String providerAccountId,
            @Nullable String continuationKey
    ) {
        return enableBankingWebClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/accounts/{id}/transactions")
                                .queryParamIfPresent("continuation_key", Optional.ofNullable(continuationKey))
                                .build(providerAccountId)
                )
                .retrieve()
                .bodyToMono(EnableBankingTransactionsResponse.class);
    }

}
