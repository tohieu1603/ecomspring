package com.hieu.api_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Key resolvers for Spring Cloud Gateway's {@code RequestRateLimiter} filter.
 *
 * <p>Two strategies are exposed:
 * <ul>
 *   <li><b>ipKeyResolver</b> (primary): throttles per client IP — safe default for
 *       unauthenticated endpoints like {@code /api/auth/login}.</li>
 *   <li><b>userKeyResolver</b>: throttles per JWT {@code X-User-Id} when available,
 *       falls back to IP. Useful for heavy authenticated endpoints so one user
 *       cannot DoS the platform from a single IP.</li>
 * </ul>
 *
 * <p>Routes select the resolver via {@code filters.RequestRateLimiter.key-resolver}.
 */
@Configuration
public class RateLimiterConfig {

    /** Default IP-based limiter. */
    @Primary
    @Bean("ipKeyResolver")
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            var remote = exchange.getRequest().getRemoteAddress();
            String key = remote == null ? "unknown" : remote.getAddress().getHostAddress();
            return Mono.just("ip:" + key);
        };
    }

    /** User-based limiter with IP fallback. */
    @Bean("userKeyResolver")
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            var remote = exchange.getRequest().getRemoteAddress();
            String ip = remote == null ? "unknown" : remote.getAddress().getHostAddress();
            return Mono.just("ip:" + ip);
        };
    }
}
