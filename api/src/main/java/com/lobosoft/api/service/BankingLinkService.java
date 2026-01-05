package com.lobosoft.api.service;

import com.lobosoft.api.dto.EnableBankingAuthResponse;
import com.lobosoft.api.dto.StartAuthRequest;
import com.lobosoft.api.dto.StartAuthResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankingLinkService {

    private final WebClient enableBankingWebClient;

    public Mono<@NonNull StartAuthResponse> startAuth(
            UUID userId,
            StartAuthRequest request
    ) {
        return enableBankingWebClient.post()
                .uri("/enablebanking/auth")
                .header("X-User-Id", userId.toString())
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                        res.createException().flatMap(ex -> {
                            log.warn("EnableBanking /enablebanking/auth returned {} body={}",
                                    ex.getStatusCode(), ex.getResponseBodyAsString());
                            return Mono.error(ex);
                        }))
                .bodyToMono(EnableBankingAuthResponse.class)
                .map(res -> new StartAuthResponse(
                        res.url(),
                        res.redirect_url()
                ));
    }
}
