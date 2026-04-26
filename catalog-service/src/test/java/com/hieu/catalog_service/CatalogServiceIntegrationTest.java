package com.hieu.catalog_service;

import com.hieu.catalog_service.application.command.product.CreateProductCommand;
import com.hieu.catalog_service.application.dto.PageDTO;
import com.hieu.catalog_service.application.dto.ProductSummaryDTO;
import com.hieu.catalog_service.application.handler.product.CreateProductHandler;
import com.hieu.catalog_service.application.handler.product.ListProductsHandler;
import com.hieu.catalog_service.application.query.product.ListProductsQuery;
import com.hieu.catalog_service.domain.exception.VariantSkuAlreadyExistsException;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductStatus;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Full-stack integration tests for catalog-service.
 * Containers are shared (static) across all tests for speed.
 */
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.grpc.server.port=0"
})
class CatalogServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("catalogdb")
                    .withUsername("cataloguser")
                    .withPassword("catalogpass");

    @SuppressWarnings("resource")
    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        // Disable Kafka in catalog-service tests — events are AFTER_COMMIT best-effort
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9999");
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }

    @Autowired
    CreateProductHandler createProductHandler;

    @Autowired
    ListProductsHandler listProductsHandler;

    @Autowired
    ProductRepository productRepository;

    // ── Helper ────────────────────────────────────────────────────────────────

    private CreateProductCommand buildCmd(String name, String sku) {
        var variant = new CreateProductCommand.VariantCmd(
                sku,
                new BigDecimal("99.99"),
                new BigDecimal("50.00"),
                new BigDecimal("89.99"),
                null,
                new BigDecimal("0.5"),
                10,
                List.of()
        );
        return new CreateProductCommand(
                name, "A test product", null, "TestBrand",
                null, List.of(), null, null, null,
                List.of(variant), false, "test-user"
        );
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void shouldCreateProductWithVariantsAndFetchById() {
        var cmd = buildCmd("Integration Widget", "SKU-IW-001");
        var dto = createProductHandler.handle(cmd);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isPositive();
        assertThat(dto.slug()).isNotBlank();
        // Default status is DRAFT (activate=false)
        assertThat(dto.status()).isEqualTo(ProductStatus.DRAFT.name());
        // Variants hydrated
        assertThat(dto.variants()).hasSize(1);
        assertThat(dto.variants().get(0).sku()).isEqualTo("SKU-IW-001");

        // Fetch back via repository
        var saved = productRepository.findByIdWithVariants(ProductId.of(dto.id()));
        assertThat(saved).isPresent();
        assertThat(saved.get().getVariants()).hasSize(1);
        assertThat(saved.get().getStatus()).isEqualTo(ProductStatus.DRAFT);
    }

    @Test
    void shouldEnforceSkuUnique() {
        // Create first product with SKU-DUP-001
        createProductHandler.handle(buildCmd("First Product", "SKU-DUP-001"));

        // Second product with same SKU must throw
        assertThatThrownBy(() -> createProductHandler.handle(buildCmd("Second Product", "SKU-DUP-001")))
                .isInstanceOf(VariantSkuAlreadyExistsException.class);
    }

    @Test
    void shouldPaginateWithCursor() {
        // Seed 5 products with unique SKUs
        for (int i = 1; i <= 5; i++) {
            createProductHandler.handle(buildCmd("Paged Product " + i, "SKU-PAGE-" + System.nanoTime()));
        }

        // First page: limit=2
        PageDTO<ProductSummaryDTO> page1 = listProductsHandler.handle(new ListProductsQuery(null, 2));
        assertThat(page1.items()).hasSize(2);
        assertThat(page1.nextCursor()).isNotNull();

        // Second page: use cursor from first
        PageDTO<ProductSummaryDTO> page2 = listProductsHandler.handle(
                new ListProductsQuery(page1.nextCursor(), 2));
        assertThat(page2.items()).hasSize(2);
        // Items on page2 must not overlap page1
        var page1Ids = page1.items().stream().map(ProductSummaryDTO::id).toList();
        page2.items().forEach(item -> assertThat(page1Ids).doesNotContain(item.id()));

        // Keep advancing until last page — nextCursor must be null
        String cursor = page2.nextCursor();
        while (cursor != null) {
            PageDTO<ProductSummaryDTO> next = listProductsHandler.handle(
                    new ListProductsQuery(cursor, 2));
            cursor = next.nextCursor();
            if (cursor == null) {
                // Last page reached
                assertThat(next.nextCursor()).isNull();
            }
        }
    }
}
