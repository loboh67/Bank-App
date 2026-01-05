package com.lobosoft.sync.config;

import com.lobosoft.sync.dto.TransactionUpsertedEvent;
import lombok.NonNull;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9095}")
    private String bootstrapServers;

    @Value("${spring.kafka.client-id:sync-service}")
    private String clientId;

    @Bean
    public ProducerFactory<@NonNull String, @NonNull TransactionUpsertedEvent> transactionProducerFactory() {
        Map<String, Object> props = new java.util.HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<@NonNull String, @NonNull TransactionUpsertedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(transactionProducerFactory());
    }
}
