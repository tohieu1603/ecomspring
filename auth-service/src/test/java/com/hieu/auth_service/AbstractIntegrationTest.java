package com.hieu.auth_service;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests that need a full auth-service infrastructure stack.
 *
 * <p>Spins up real Postgres + Redis + Kafka via Testcontainers once per JVM (static
 * containers are reused across test classes by JUnit 5). This gives us production-like
 * persistence + messaging behaviour — unlike embedded H2 / in-memory Kafka which routinely
 * disagree with real Postgres / Kafka on edge cases.
 *
 * <p>Subclasses need only extend this class; {@link DynamicPropertySource} wires the
 * containers' runtime ports into Spring properties before the context starts.
 *
 * <p>Use the {@code test} profile so {@link com.hieu.auth_service.config.DataSeeder}
 * stays inactive; tests supply their own fixtures.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
public abstract class AbstractIntegrationTest {

    /** Postgres 16 — matches production major version. */
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("authdb")
                    .withUsername("auth")
                    .withPassword("auth")
                    .withReuse(true);

    /** Redis 7 — backs the token blacklist. */
    @Container
    static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"))
                    .withReuse(true);

    /** Kafka — Confluent image is recognised by Testcontainers' Kafka module. */
    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
                    .withReuse(true);

    /**
     * Propagates container endpoints into Spring's Environment before context refresh.
     * Keeps Flyway + JPA aligned on the same jdbc URL (some tests verify both).
     */
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

        // 32-char dev secret — avoids failing JwtProperties validation in the test context.
        registry.add("jwt.secret", () -> "test-secret-test-secret-test-secret-1234");
    }
}
