package com.hieu.notification_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Kafka consumer configuration is driven by {@code spring.kafka.consumer.*} in application.yaml.
 * Untyped JSON deserialization: HashMap as default type, no type headers.
 * Spring Boot autoconfigures the listener factory; this class enables @KafkaListener scanning.
 */
@Configuration
@EnableKafka
public class KafkaConfig {
}
