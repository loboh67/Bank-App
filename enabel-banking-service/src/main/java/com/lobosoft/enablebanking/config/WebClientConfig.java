package com.lobosoft.enablebanking.config;

import com.lobosoft.enablebanking.EnableBankingJwtFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final EnableBankingProperties props;
    private final EnableBankingJwtFactory jwtFactory;

    @Bean
    public WebClient enableBankingWebClient() {
        var strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024) // 10MB
                )
                .build();

        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .exchangeStrategies(strategies)
                .filter(authorizationFilter())
                .build();
    }

    private ExchangeFilterFunction authorizationFilter() {
        return (request, next) -> {
            String jwt = jwtFactory.createJwt();

            ClientRequest newRequest = ClientRequest.from(request)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(jwt))
                    .build();

            return next.exchange(newRequest);
        };
    }
}
