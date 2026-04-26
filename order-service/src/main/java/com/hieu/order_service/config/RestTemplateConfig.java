package com.hieu.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Shared {@link RestTemplate} for service-to-service REST calls. Connect/read timeouts
 * keep saga steps bounded — without them a dead downstream would tie up the thread pool.
 * Boot 4 moved {@code RestTemplateBuilder} out of core autoconfig, so we build manually
 * with {@link SimpleClientHttpRequestFactory} (avoids the Apache HttpClient transitive).
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        return new RestTemplate(factory);
    }
}
