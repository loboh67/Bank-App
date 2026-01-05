package com.lobosoft.enablebanking.service;

import com.lobosoft.enablebanking.client.EnableBankingClient;
import com.lobosoft.enablebanking.config.EnableBankingProperties;
import com.lobosoft.enablebanking.domain.Account;
import com.lobosoft.enablebanking.domain.AccountRepository;
import com.lobosoft.enablebanking.domain.BankSession;
import com.lobosoft.enablebanking.domain.BankSessionRepository;
import com.lobosoft.enablebanking.dto.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnableBankingService {

    private final EnableBankingClient client;
    private final EnableBankingProperties props;
    private final ObjectMapper objectMapper;

    private final WebClient enableBankingWebClient;

    private final BankSessionRepository bankSessionRepository;
    private final AccountRepository accountRepository;

    // ----------------------------------------------------
    // ASPSPs
    // ----------------------------------------------------

    public Mono<@NonNull List<AspspDto>> listAspsps(String country, String filterName) {
        return client.getAspsps(country)
                .map(list -> {
                    if (filterName == null || filterName.isBlank()) return list;

                    String needle = filterName.toLowerCase();
                    return list.stream()
                            .filter(a -> a.getName() != null && a.getName().toLowerCase().contains(needle))
                            .toList();
                });
    }

    // ----------------------------------------------------
    // Start auth
    // ----------------------------------------------------

    public Mono<@NonNull StartAuthResponse> startAuthorizationForUser(AspspDto req, String userId) {
        String state = UUID.randomUUID().toString();

        String validUntil = Instant.now()
                .plus(30, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.MILLIS)
                .toString();

        StartAuthPayload.Access access = StartAuthPayload.Access.builder()
                .validUntil(validUntil)
                .build();

        StartAuthPayload.Aspsp aspsp = StartAuthPayload.Aspsp.builder()
                .name(req.getName())
                .country(req.getCountry())
                .build();

        StartAuthPayload payload = StartAuthPayload.builder()
                .access(access)
                .aspsp(aspsp)
                .state(state)
                .redirectUrl(props.getRedirectUrl())
                .build();

        log.debug(
                "[EB] User {} start auth payload: aspspName={} aspspCountry={} redirectUrl={} validUntil={} state={}",
                userId,
                aspsp.getName(),
                aspsp.getCountry(),
                payload.getRedirectUrl(),
                access.getValidUntil(),
                state
        );

        return client.startAuthorization(payload)
                .flatMap(startResponse ->
                        Mono.fromCallable(() -> {
                                   createOrRefreshSession(
                                           userId,
                                           aspsp.getName(),
                                           aspsp.getCountry(),
                                           state,
                                           Instant.parse(validUntil)
                                   );
                                    return startResponse; // returned downstream
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                );
    }

    // ----------------------------------------------------
    // Callback
    // ----------------------------------------------------

    public Mono<@NonNull ObjectNode> handleAuthCallBack(
            String code,
            String state,
            String error,
            String errorDescription
    ) {
        return Mono.defer(() -> {

            Mono<@NonNull BankSession> sessionMono = Mono.fromCallable(() ->
                            bankSessionRepository.findByState(state)
                                    .orElseThrow(() -> new IllegalStateException("No BankSession for state=" + state))
                    )
                    .subscribeOn(Schedulers.boundedElastic());

            // error from bank
            if (error != null && !error.isBlank()) {
                return sessionMono.flatMap(session ->
                        buildErrorResponse(session, error, Optional.ofNullable(errorDescription).orElse(""))
                );
            }

            // no code, invalid callback
            if (code == null || code.isBlank()) {
                return sessionMono.flatMap(session ->
                        buildErrorResponse(session, "missing_code", "Authorization code is missing")
                );
            }

            // happy path
            return sessionMono.flatMap(session ->
                    handleSuccessfulCallback(session, code, state)
            );
        });
    }

    // ----------------------------------------------------
    // Callback helpers
    // ----------------------------------------------------

    private Mono<@NonNull ObjectNode> buildErrorResponse(BankSession session, String error, String description) {
        return Mono.fromCallable(() -> {
            session.setStatus("FAILED");
            bankSessionRepository.save(session);

            ObjectNode json = objectMapper.createObjectNode();
            json.put("status", "error");
            json.put("error", error);
            json.put("error_description", description);

            return json;
        })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<@NonNull ObjectNode> handleSuccessfulCallback(BankSession session, String code, String state) {
        return client.exchangeCodeForSession(code)   // POST /sessions -> JsonNode
                .flatMap(sessionJson ->
                        Mono.fromCallable(() -> {
                                    updateSessionFromJson(session, sessionJson, state);
                                    upsertAccountsFromSessionJson(session, sessionJson);

                                    ObjectNode json = objectMapper.createObjectNode();
                                    json.put("status", "ok");
                                    json.put("state", state);
                                    json.set("session", sessionJson);

                                    return json;
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                );
    }

    private void updateSessionFromJson(BankSession session, JsonNode sessionJson, String state) {
        JsonNode accessNode = sessionJson.path("access");
        String validUntilStr = accessNode.path("valid_until").asString(null);

        if (validUntilStr != null && !validUntilStr.isBlank()) {
            session.setValidUntil(Instant.parse(validUntilStr));
        }

        session.setStatus("AUTHORIZED");

        bankSessionRepository.save(session);
        log.info("[EB] Updated BankSession id={} for userId={} to AUTHORIZED",
                session.getId(), session.getUserId());
    }

    private void upsertAccountsFromSessionJson(BankSession session, JsonNode sessionJson) {
        String userId = session.getUserId();

        JsonNode accountsNode = sessionJson.path("accounts");

        if (!accountsNode.isArray()) {
            return;
        }

        for (JsonNode accNode : accountsNode) {
            String providerAccountId = accNode.path("uid").asString(null);
            if (providerAccountId == null || providerAccountId.isBlank()) {
                log.warn("[EB] Skipping account with missing uid: {}", accNode);
                continue;
            }

            String identificationHash = accNode.path("identification_hash").asString(null);
            String iban = accNode.path("account_id").path("iban").asString(null);

            String name = accNode.path("name").asString(null);

            if (name == null || name.isBlank()) {
                name = "";
            }

            Account account;
            try {
                Optional<Account> existingByProvider =
                        accountRepository.findByUserIdAndProviderAccountId(userId, providerAccountId);

                Optional<Account> existingByIban;
                if (iban != null && !iban.isBlank()) {
                    existingByIban = accountRepository.findByUserIdAndIban(userId, iban);
                } else {
                    existingByIban = Optional.empty();
                }

                account = existingByProvider
                        .or(() -> existingByIban)
                        .orElseGet(Account::new);

            } catch (Exception repoEx) {
                log.error("[EB] Error loading Account from DB for userId={} providerAccountId={}",
                        userId, providerAccountId, repoEx);
                continue;
            }

            account.setUserId(userId);
            account.setProviderAccountId(providerAccountId);
            account.setIdentificationHash(identificationHash);
            account.setIban(iban);
            account.setName(name);
            account.setStatus("ACTIVE");

            try {
                accountRepository.save(account);
                log.info("[EB] Saved account id={} providerAccountId={} for userId={}",
                        account.getId(), providerAccountId, userId);
            } catch (Exception saveEx) {
                log.error("[EB] Error saving Account for userId={} providerAccountId={}",
                        userId, providerAccountId, saveEx);
            }
        }
    }

    // ----------------------------------------------------
    // Transactions passthrough
    // ----------------------------------------------------

    public Mono<@NonNull JsonNode> getAccountTransactions(
            String providerAccountId,
            String dateFrom,
            String dateTo
    ) {
        return client.getAccountTransactions(providerAccountId, dateFrom, dateTo)
                .doOnNext(json -> {
                    int count = json.path("transactions").size();
                    log.info("Fetched {} transactions for providerAccountId={}", count, providerAccountId);
                });
    }

    public Mono<JsonNode> getAccountTransactionsPaged(String providerAccountId, String continuationKey) {
        return enableBankingWebClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/accounts/{id}/transactions")
                                .queryParam("continuation_key", continuationKey)
                                .build(providerAccountId)
                )
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    @Transactional
    public BankSession createOrRefreshSession(
            String userId,
            String aspspName,
            String aspspCountry,
            String state,
            Instant validUntil
    ) {
        var existingOpt = bankSessionRepository
                .findByUserIdAndAspspNameAndAspspCountry(userId, aspspName, aspspCountry);

        BankSession session;
        if (existingOpt.isPresent()) {
            session = existingOpt.get();
        } else {
            // Create new session
            session = new BankSession();
            session.setUserId(userId);
            session.setAspspName(aspspName);
            session.setAspspCountry(aspspCountry);
        }
        session.setValidUntil(validUntil);
        session.setState(state);

        session.setStatus("PENDING");

        return bankSessionRepository.save(session);
    }
}
