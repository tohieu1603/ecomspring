package com.hieu.order_service.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hieu.order_service.application.dto.OrderDTO;
import com.hieu.order_service.domain.exception.DuplicateOrderException;
import com.hieu.order_service.domain.model.order.IdempotencyRecord;
import com.hieu.order_service.domain.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Redis Lua + DB hybrid idempotency guard for order creation.
 *
 * <ol>
 *   <li>Atomic Redis Lua SETNX: "new" → proceed; "{...}" JSON → return cached DTO; "PROCESSING" → reject.</li>
 *   <li>DB record inserted for durable audit trail.</li>
 *   <li>On success, write full DTO JSON to Redis + update DB record to COMPLETED.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final Duration REDIS_TTL = Duration.ofMinutes(30);
    private static final long REDIS_TTL_SECONDS = REDIS_TTL.toSeconds();
    private static final String REDIS_PREFIX = "order:idem:";
    private static final String PROCESSING_TOKEN = "PROCESSING";

    private final IdempotencyRepository idempotencyRepository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final DefaultRedisScript<String> claimIdempotencyScript;

    /** Returns Optional.empty() → proceed; Optional.of(dto) → short-circuit. */
    @Transactional
    public Optional<OrderDTO> checkOrCreate(String userId, String key) {
        var redisKey = REDIS_PREFIX + userId + ":" + key;

        // Step 1: Atomic Redis Lua claim
        String luaResult = redis.execute(
                claimIdempotencyScript,
                List.of(redisKey),
                PROCESSING_TOKEN,
                String.valueOf(REDIS_TTL_SECONDS)
        );

        if (luaResult == null || "new".equals(luaResult)) {
            // New request — insert DB record and proceed
            idempotencyRepository.save(IdempotencyRecord.create(key));
            return Optional.empty();
        }

        // Step 2: If result starts with '{' it's a cached DTO JSON
        if (luaResult.startsWith("{")) {
            try {
                return Optional.of(objectMapper.readValue(luaResult, OrderDTO.class));
            } catch (Exception e) {
                log.warn("Redis idempotency cache corrupt for key {}, falling back to DB", redisKey);
            }
        }

        // Step 3: "PROCESSING" or unknown — check DB as fallback
        if (PROCESSING_TOKEN.equals(luaResult)) {
            // Check DB to see if it might be a stale PROCESSING from a crashed node
            var dbRecord = idempotencyRepository.findByKey(key);
            if (dbRecord.isPresent()) {
                var record = dbRecord.get();
                if (record.getStatus() == IdempotencyRecord.Status.COMPLETED
                        && record.getResponseBody() != null) {
                    try {
                        return Optional.of(objectMapper.readValue(record.getResponseBody(), OrderDTO.class));
                    } catch (Exception e) {
                        log.warn("DB idempotency body corrupt for key {}", key);
                    }
                }
                throw new DuplicateOrderException(key);
            }
            throw new DuplicateOrderException(key);
        }

        // Fallback: DB secondary durable store
        return idempotencyRepository.findByKey(key).map(record -> {
            if (record.getStatus() == IdempotencyRecord.Status.COMPLETED
                    && record.getResponseBody() != null) {
                try {
                    return objectMapper.readValue(record.getResponseBody(), OrderDTO.class);
                } catch (Exception e) {
                    log.warn("DB idempotency body corrupt for key {}", key);
                }
            }
            return (OrderDTO) null;
        }).map(Optional::ofNullable).orElseGet(() -> {
            idempotencyRepository.save(IdempotencyRecord.create(key));
            return Optional.empty();
        });
    }

    @Transactional
    public void markCompleted(String key, Long orderId, OrderDTO dto) {
        idempotencyRepository.findByKey(key).ifPresent(record -> {
            try {
                var json = objectMapper.writeValueAsString(dto);
                record.markCompleted(orderId, json);
                idempotencyRepository.save(record);
                var redisKey = REDIS_PREFIX + dto.userId() + ":" + key;
                cacheToRedis(redisKey, json);
            } catch (Exception e) {
                log.warn("Failed to persist idempotency completion for key {}: {}", key, e.getMessage());
            }
        });
    }

    private void cacheToRedis(String redisKey, String json) {
        try {
            redis.opsForValue().set(redisKey, json, REDIS_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache idempotency to Redis key {}: {}", redisKey, e.getMessage());
        }
    }
}
