package com.lobosoft.enablebanking.grpc;

import com.lobosoft.enablebanking.service.EnableBankingService;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnableBankingGrpcServiceImplTest {

    @Mock
    private EnableBankingService service;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void getAccountTransactions_mapsResponseAndContinuationKey() {
        EnableBankingGrpcServiceImpl grpc = new EnableBankingGrpcServiceImpl(service);

        ObjectNode json = mapper.createObjectNode();
        ArrayNode txs = json.putArray("transactions");
        ObjectNode tx = txs.addObject();
        tx.put("transaction_id", "t-1");
        tx.put("entry_reference", "er-1");
        ObjectNode amount = tx.putObject("transaction_amount");
        amount.put("amount", "10.50");
        amount.put("currency", "EUR");
        tx.put("credit_debit_indicator", "DBIT");
        tx.put("booking_date", "2024-01-01");
        tx.put("note", "Coffee");
        json.put("continuation_key", "ck-123");

        when(service.getAccountTransactions("acc-1", "2024-01-01", "2024-01-31"))
                .thenReturn(Mono.just(json));

        GetAccountTransactionsRequest request = GetAccountTransactionsRequest.newBuilder()
                .setProviderAccountId("acc-1")
                .setFromDate("2024-01-01")
                .setToDate("2024-01-31")
                .build();

        CapturingObserver observer = new CapturingObserver();
        grpc.getAccountTransactions(request, observer);

        assertThat(observer.error).isNull();
        assertThat(observer.response).isNotNull();
        assertThat(observer.response.getTransactionsCount()).isEqualTo(1);
        Transaction resultTx = observer.response.getTransactions(0);
        assertThat(resultTx.getProviderTransactionId()).isEqualTo("t-1");
        assertThat(resultTx.getDirection()).isEqualTo("DEBIT");
        assertThat(resultTx.getAmount()).isEqualTo("10.50");
        assertThat(observer.response.getContinuationKey()).isEqualTo("ck-123");
    }

    private static class CapturingObserver implements StreamObserver<GetAccountTransactionsResponse> {
        GetAccountTransactionsResponse response;
        Throwable error;

        @Override
        public void onNext(GetAccountTransactionsResponse value) {
            this.response = value;
        }

        @Override
        public void onError(Throwable t) {
            this.error = t;
        }

        @Override
        public void onCompleted() {
            // no-op
        }
    }
}
