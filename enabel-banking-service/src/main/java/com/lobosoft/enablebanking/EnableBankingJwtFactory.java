package com.lobosoft.enablebanking;

import com.lobosoft.enablebanking.config.EnableBankingKeyProvider;
import com.lobosoft.enablebanking.config.EnableBankingProperties;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class EnableBankingJwtFactory {

    private final EnableBankingKeyProvider keyProvider;
    private final EnableBankingProperties props;

    public String createJwt() {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofDays(1));

        return Jwts.builder()
                .header()
                    .add("typ", "JWT")
                    .add("alg", "RS256")
                    .add("kid", props.getAppId())
                .and()

                .claims()
                    .add("iss", props.getAppId())
                    .add("aud", props.getAudience())
                .and()

                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))

                .signWith(keyProvider.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }
}
