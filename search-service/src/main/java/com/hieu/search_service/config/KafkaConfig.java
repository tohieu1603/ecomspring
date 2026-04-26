package com.hieu.search_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Kafka consumer config is driven by spring.kafka.consumer.* in application.yaml.
 * Untyped JSON deserialization: HashMap as default type, no type headers.
 * Spring Boot autoconfigures the listener factory; this class enables @KafkaListener scanning.
 */
@Configuration
@EnableKafka
public class KafkaConfig {
}
