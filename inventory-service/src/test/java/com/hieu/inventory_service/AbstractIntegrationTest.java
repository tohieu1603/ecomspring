package com.hieu.inventory_service;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base IT class for inventory-service.
 * Postgres 16 + Redis 7 + Kafka 7.5.0 — withReuse(true).
 * Eureka disabled; JWT secret seeded.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.grpc.server.port=0"
})
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("inventorydb")
                    .withUsername("inventoryuser")
                    .withPassword("inventorypass")
                    .withReuse(true);

    @Container
    static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"))
                    .withReuse(true);

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
                    .withReuse(true);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.database-platform",
                () -> "org.hibernate.dialect.PostgreSQLDialect");

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        registry.add("jwt.secret", () -> "test-secret-test-secret-test-sec1");
    }
}
