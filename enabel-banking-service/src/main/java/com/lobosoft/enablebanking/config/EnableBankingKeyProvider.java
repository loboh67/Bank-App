package com.lobosoft.enablebanking.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class EnableBankingKeyProvider {

    private final EnableBankingProperties props;

    private PrivateKey cachedKey;

    public PrivateKey getPrivateKey() {
        if (cachedKey != null) return cachedKey;

        try {
            String pem = Files.readString(
                    Path.of(props.getPrivateKeyPath()),
                    StandardCharsets.UTF_8
            );

            String stripped = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(stripped);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);

            KeyFactory kf = KeyFactory.getInstance("RSA");
            cachedKey = kf.generatePrivate(keySpec);
            return cachedKey;

        } catch (Exception e) {
            throw new IllegalStateException("Failed to load EnableBanking private key from "
                    + props.getPrivateKeyPath(), e);
        }
    }
}
