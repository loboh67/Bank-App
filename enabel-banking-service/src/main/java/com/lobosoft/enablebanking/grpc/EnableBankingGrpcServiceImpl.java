package com.lobosoft.enablebanking.grpc;

import com.lobosoft.enablebanking.service.EnableBankingService;
import io.grpc.stub.StreamObserver;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class EnableBankingGrpcServiceImpl extends EnableBankingGrpcServiceGrpc.EnableBankingGrpcServiceImplBase {

    private final EnableBankingService service;

    @Override
    public void getAccountTransactions(
            GetAccountTransactionsRequest request,
            StreamObserver<GetAccountTransactionsResponse> responseObserver
    ) {
        String providerAccountId = request.getProviderAccountId();
        String from = emptyToNull(request.getFromDate());
        String to = emptyToNull(request.getToDate());
        String continuationKeyFromSync = emptyToNull(request.getContinuationKey());

        log.info("[gRPC] GetAccountTransactions for account={} from={} to={} continuationKey={}",
                providerAccountId, from, to, continuationKeyFromSync);

        try {
            Mono<@NonNull JsonNode> mono;

            if (continuationKeyFromSync != null) {
                mono = service.getAccountTransactionsPaged(providerAccountId, continuationKeyFromSync);
            } else {
                mono = service.getAccountTransactions(providerAccountId, from, to);
            }

            JsonNode json = mono.block();
            assert json != null;

            JsonNode txArray = json.path("transactions");
            String continuationKeyFromProvider = textOrNull(json, "continuation_key");

            log.info("[gRPC] Provider returned {} transactions, continuationKey='{}'",
                    txArray.isArray() ? txArray.size() : 0,
                    continuationKeyFromProvider
            );

            GetAccountTransactionsResponse.Builder responseBuilder =
                    GetAccountTransactionsResponse.newBuilder();

            if (txArray.isArray()) {
                for (JsonNode txNode : txArray) {
                    responseBuilder.addTransactions(toProtoTransaction(txNode));
                }
            }

            if (continuationKeyFromProvider != null && !continuationKeyFromProvider.isBlank()) {
                responseBuilder.setContinuationKey(continuationKeyFromProvider);
            }

            GetAccountTransactionsResponse response = responseBuilder.build();

            log.info("[gRPC] Sending gRPC response for account={} with {} transactions and continuationKey='{}'",
                    providerAccountId,
                    response.getTransactionsCount(),
                    response.getContinuationKey()
            );

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("[gRPC] Error in GetAccountTransactions", e);
            responseObserver.onError(e);
        }
    }

    private Transaction toProtoTransaction(JsonNode txNode) {
        String transactionId = textOrNull(txNode, "transaction_id");
        String entryRef = textOrNull(txNode, "entry_reference");

        String providerTransactionId = transactionId != null && !transactionId.isBlank()
                ? transactionId
                : entryRef != null ? entryRef : "";

        JsonNode amountNode = txNode.path("transaction_amount");
        String amount = textOrDefault(amountNode, "amount", "0.00");
        String currency = textOrDefault(amountNode, "currency", "EUR");

        String creditDebit = textOrDefault(txNode, "credit_debit_indicator", "");
        String direction;
        if ("DBIT".equalsIgnoreCase(creditDebit)) {
            direction = "DEBIT";
        } else if ("CRDT".equalsIgnoreCase(creditDebit)) {
            direction = "CREDIT";
        } else {
            direction = "";
        }

        String bookingDate = textOrDefault(txNode, "booking_date", "");
        String valueDate   = textOrDefault(txNode, "value_date", "");

        String description = "";
        JsonNode remInfo = txNode.path("remittance_information");
        if (remInfo.isArray() && !remInfo.isEmpty()) {
            description = remInfo.get(0).asString("");
        } else {
            description = textOrDefault(txNode, "note", "");
        }

        return Transaction.newBuilder()
                .setProviderTransactionId(providerTransactionId)
                .setEntryReference(entryRef != null ? entryRef : "")
                .setAmount(amount)
                .setCurrency(currency)
                .setDirection(direction)
                .setBookingDate(bookingDate)
                .setValueDate(valueDate)
                .setDescription(description)
                .setRawJson(txNode.toString())
                .build();
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asString();
    }

    private String textOrDefault(JsonNode node, String field, String def) {
        String s = textOrNull(node, field);
        return (s == null) ? def : s;
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

}
