package com.hieu.search_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.elasticsearch.uris=http://localhost:9200",
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration," +
                        "org.springframework.kafka.annotation.EnableKafka," +
                        "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
        })
@ActiveProfiles("test")
class SearchServiceApplicationTests {

    @Test
    void contextLoads() {
        // Context smoke test — Elasticsearch + Kafka excluded to avoid infra deps
    }
}
