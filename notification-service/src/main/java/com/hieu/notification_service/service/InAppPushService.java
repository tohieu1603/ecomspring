package com.hieu.notification_service.service;

import com.hieu.notification_service.dto.NotificationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages SSE emitter registry and pushes in-app notifications in real time.
 * Emitters are stored per userId; stale/completed emitters are cleaned up automatically.
 * Keep-alive scheduling is owned here so the lifecycle is fully encapsulated
 * (fixes the race where onCompletion was attached after the scheduler was started in the controller).
 */
@Service
@Slf4j
public class InAppPushService {

    /** userId → list of active SSE emitters. */
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private final ScheduledExecutorService keepAlive =
            Executors.newScheduledThreadPool(2, Thread.ofVirtual().factory());

    /**
     * Register a new SSE connection for a user.
     * Keep-alive scheduling and lifecycle callbacks are wired BEFORE returning the emitter
     * to avoid a race where {@code onCompletion} fires before {@code future} is assigned.
     */
    public SseEmitter register(String userId) {
        // 5-minute timeout; keep-alive comments sent every 15s
        var emitter = new SseEmitter(300_000L);
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Schedule keep-alive BEFORE attaching callbacks so future is never null
        ScheduledFuture<?> future = keepAlive.scheduleAtFixedRate(
                () -> sendKeepAlive(userId, emitter), 15, 15, TimeUnit.SECONDS);

        emitter.onCompletion(() -> { future.cancel(true); remove(userId, emitter); });
        emitter.onTimeout(()   -> { future.cancel(true); remove(userId, emitter); });
        emitter.onError(e      -> { future.cancel(true); remove(userId, emitter); });

        log.debug("SSE registered userId={}", userId);
        return emitter;
    }

    /** Push a notification event to all active emitters of the target user. */
    @Async("sseExecutor")
    public void push(String userId, NotificationDTO dto) {
        var list = emitters.get(userId);
        if (list == null || list.isEmpty()) return;

        list.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("notification").data(dto));
                return false;
            } catch (IOException e) {
                log.debug("SSE push failed for userId={}, removing emitter", userId);
                return true;
            }
        });
    }

    private void sendKeepAlive(String userId, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().comment("keep-alive"));
        } catch (IOException e) {
            remove(userId, emitter);
        }
    }

    private void remove(String userId, SseEmitter emitter) {
        var list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emitters.remove(userId, list);
        }
    }

    @jakarta.annotation.PreDestroy
    public void shutdown() {
        keepAlive.shutdown();
        try {
            if (!keepAlive.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                keepAlive.shutdownNow();
            }
        } catch (InterruptedException e) {
            keepAlive.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
