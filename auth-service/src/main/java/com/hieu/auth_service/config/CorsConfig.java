package com.hieu.auth_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS configuration for browser-based clients.
 *
 * <p>Origins are supplied via the {@code cors.allowed-origins} property (comma-
 * separated) so prod/staging can differ from dev without code changes. Credentials are
 * enabled so cookies / Authorization headers can round-trip; wildcard origins are
 * therefore disallowed by the CORS spec and we fall back to the concrete list.
 */
@Configuration
public class CorsConfig {

    /**
     * Builds a {@link CorsFilter} registered high in the Spring filter chain
     * (Spring Security picks it up automatically).
     *
     * @param allowedOrigins comma-separated origins from {@code cors.allowed-origins}
     * @return configured CORS filter
     */
    @Bean
    public CorsFilter corsFilter(
            @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
            List<String> allowedOrigins) {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
