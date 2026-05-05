package com.hieu.auth_service.interfaces.rest.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * C5: Per-IP rate limiter for /login and /register using Bucket4j in-memory buckets.
 *
 * <p>Limits: login → 5 req/min, register → 3 req/min. Exceeding limit returns HTTP 429
 * with a JSON error body matching the global error schema.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH    = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";

    // Separate caches per endpoint so limits are independent per IP.
    private final ConcurrentHashMap<String, Bucket> loginBuckets    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> registerBuckets = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String ip   = request.getRemoteAddr();

        Bucket bucket = null;
        if (LOGIN_PATH.equals(path)) {
            bucket = loginBuckets.computeIfAbsent(ip, k -> newBucket(5));
        } else if (REGISTER_PATH.equals(path)) {
            bucket = registerBuckets.computeIfAbsent(ip, k -> newBucket(3));
        }

        if (bucket != null && !bucket.tryConsume(1)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    Map.of("code", "AUTH-1014", "message", "Too many requests"));
            return;
        }

        chain.doFilter(request, response);
    }

    /** Creates a Bandwidth-limited bucket with {@code reqPerMinute} tokens refilled every 60 s. */
    private static Bucket newBucket(int reqPerMinute) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(reqPerMinute)
                        .refillGreedy(reqPerMinute, Duration.ofMinutes(1))
                        .build())
                .build();
    }
}
