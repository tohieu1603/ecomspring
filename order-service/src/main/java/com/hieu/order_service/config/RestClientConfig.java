package com.hieu.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * {@link RestClient} bean for service-to-service REST calls (PaymentServiceClient,
 * VoucherServiceClient). Uses the same {@link SimpleClientHttpRequestFactory} as
 * {@link RestTemplateConfig} to avoid extra dependencies; the existing
 * {@code restTemplate} bean is kept for the other four clients.
 */
@Configuration
public class RestClientConfig {

    @Bean("serviceRestClient")
    public RestClient serviceRestClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
