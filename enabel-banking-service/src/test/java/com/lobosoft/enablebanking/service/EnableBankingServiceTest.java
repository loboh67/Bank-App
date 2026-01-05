package com.lobosoft.enablebanking.service;

import com.lobosoft.enablebanking.client.EnableBankingClient;
import com.lobosoft.enablebanking.config.EnableBankingProperties;
import com.lobosoft.enablebanking.domain.Account;
import com.lobosoft.enablebanking.domain.AccountRepository;
import com.lobosoft.enablebanking.domain.BankSession;
import com.lobosoft.enablebanking.domain.BankSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnableBankingServiceTest {

    @Mock
    private EnableBankingClient client;
    @Mock
    private EnableBankingProperties props;
    @Mock
    private WebClient webClient;
    @Mock
    private BankSessionRepository bankSessionRepository;
    @Mock
    private AccountRepository accountRepository;

    private EnableBankingService service;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        service = new EnableBankingService(
                client,
                props,
                mapper,
                webClient,
                bankSessionRepository,
                accountRepository
        );
    }

    @Test
    void handleAuthCallBack_updatesSessionAndUpsertsAccounts_onSuccess() {
        String state = "state-123";
        String userId = "user-1";

        BankSession session = new BankSession();
        session.setUserId(userId);
        session.setState(state);
        session.setAspspName("bank-x");
        session.setAspspCountry("FI");

        when(bankSessionRepository.findByState(state)).thenReturn(Optional.of(session));
        when(bankSessionRepository.save(any(BankSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.findByUserIdAndProviderAccountId(any(), any())).thenReturn(Optional.empty());
        when(accountRepository.findByUserIdAndIban(any(), any())).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        ObjectNode sessionJson = buildSessionJson();
        when(client.exchangeCodeForSession("code-xyz")).thenReturn(Mono.just(sessionJson));

        ObjectNode response = service
                .handleAuthCallBack("code-xyz", state, null, null)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.path("status").asString()).isEqualTo("ok");
        assertThat(response.path("state").asString()).isEqualTo(state);

        assertThat(session.getStatus()).isEqualTo("AUTHORIZED");
        assertThat(session.getValidUntil()).isNotNull();

        ArgumentCaptor<Account> savedAccount = ArgumentCaptor.forClass(Account.class);
        org.mockito.Mockito.verify(accountRepository).save(savedAccount.capture());
        Account acc = savedAccount.getValue();
        assertThat(acc.getProviderAccountId()).isEqualTo("acc-1");
        assertThat(acc.getIban()).isEqualTo("FI0012345");
        assertThat(acc.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void handleAuthCallBack_returnsErrorAndMarksSessionFailed_onProviderError() {
        String state = "state-error";
        BankSession session = new BankSession();
        session.setState(state);
        session.setUserId("user-err");

        when(bankSessionRepository.findByState(state)).thenReturn(Optional.of(session));
        when(bankSessionRepository.save(any(BankSession.class))).thenAnswer(inv -> inv.getArgument(0));

        ObjectNode response = service
                .handleAuthCallBack(null, state, "access_denied", "User cancelled")
                .block();

        assertThat(response).isNotNull();
        assertThat(response.path("status").asString()).isEqualTo("error");
        assertThat(response.path("error").asString()).isEqualTo("access_denied");
        assertThat(session.getStatus()).isEqualTo("FAILED");
    }

    private ObjectNode buildSessionJson() {
        ObjectNode root = JsonNodeFactory.instance.objectNode();

        ObjectNode access = root.putObject("access");
        access.put("valid_until", Instant.now().plusSeconds(3600).toString());

        ArrayNode accounts = root.putArray("accounts");
        ObjectNode account = accounts.addObject();
        account.put("uid", "acc-1");
        account.put("identification_hash", "idhash");
        account.putObject("account_id").put("iban", "FI0012345");
        account.put("name", "Main account");

        return root;
    }
}
