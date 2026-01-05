package com.lobosoft.enablebanking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "enablebanking")
public class EnableBankingProperties {
    private String baseUrl;
    private String privateKeyPath;
    private String redirectUrl;
    private String appId;
    private String audience;
}
