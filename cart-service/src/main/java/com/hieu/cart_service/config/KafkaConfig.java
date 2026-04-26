package com.hieu.cart_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Kafka configuration for cart-service.
 * Cart only consumes — no topics owned, no NewTopic beans needed.
 * Consumer props are wired via spring.kafka.consumer.* in application.yaml.
 */
@Configuration
@EnableKafka
public class KafkaConfig {
}
