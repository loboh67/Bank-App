package com.lobosoft.api.client;

import com.lobosoft.api.dto.TransactionsPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class SyncServiceClient {

    private final WebClient syncWebClient;

    public TransactionsPage requestNextPage(Long accountId, String userId) {
        return syncWebClient.post()
                .uri("/internal/sync/accounts/{id}/transactions/next-page", accountId)
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(TransactionsPage.class)
                .block();
    }

    public void triggerFullSync(Long accountId, String userId) {
        syncWebClient.post()
                .uri("/internal/sync/accounts/{id}/transactions/full", accountId)
                .header("X-User-Id", userId)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
