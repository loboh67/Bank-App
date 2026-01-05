package com.lobosoft.enablebanking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = EnableBankingServiceApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=none",
                "grpc.server.port=-1",
                "grpc.client.authService.address=static://localhost:0",
                "grpc.client.authService.negotiationType=plaintext"
        }
)
class EnableBankingServiceApplicationTests {

    @Test
    void contextLoads() {
        // verifies the minimal Spring context starts with test overrides
    }
}
