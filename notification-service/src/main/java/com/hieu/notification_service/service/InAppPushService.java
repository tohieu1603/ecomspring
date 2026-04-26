package com.hieu.notification_service.service;

import com.hieu.notification_service.dto.NotificationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages SSE emitter registry and pushes in-app notifications in real time.
 * Emitters are stored per userId; stale/completed emitters are cleaned up automatically.
 */
@Service
@Slf4j
public class InAppPushService {

    /** userId → list of active SSE emitters. */
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /** Register a new SSE connection for a user. */
    public SseEmitter register(String userId) {
        // 5-minute timeout; keep-alive pings sent by controller every 30s
        var emitter = new SseEmitter(300_000L);
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));

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

    /** Send a keep-alive comment to prevent proxy timeout. */
    public void keepAlive(String userId, SseEmitter emitter) {
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
}
