package com.hieu.notification_service.controller;

import com.hieu.common.security.AuthenticatedUser;
import com.hieu.notification_service.dto.*;
import com.hieu.notification_service.service.InAppPushService;
import com.hieu.notification_service.service.NotificationApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management API")
public class NotificationController {

    private final NotificationApplicationService notificationService;
    private final InAppPushService inAppPushService;

    private static final ScheduledExecutorService keepAliveScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                var t = new Thread(r, "notif-sse-keepalive");
                t.setDaemon(true);
                return t;
            });

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM') or hasAnyAuthority('ROLE_ADMIN', 'ROLE_SYSTEM')")
    @Operation(summary = "Send a notification (ADMIN only)")
    public ResponseEntity<NotificationDTO> send(@Valid @RequestBody SendNotificationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.send(req));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my notifications (offset paginated)")
    public ResponseEntity<PageDTO<NotificationDTO>> getMyNotifications(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getMyNotifications(user.userId(), page, size));
    }

    @GetMapping("/my/feed")
    @Operation(summary = "Cursor-based feed for infinite scroll")
    public ResponseEntity<CursorPageDTO<NotificationDTO>> getMyFeed(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getMyFeed(user.userId(), cursor, size));
    }

    @GetMapping("/my/unread-count")
    @Operation(summary = "Unread notification count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(user.userId())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID (own or ADMIN)")
    public ResponseEntity<NotificationDTO> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        boolean isAdmin = user.hasAnyRole("ROLE_ADMIN", "ADMIN");
        return ResponseEntity.ok(notificationService.getById(id, user.userId(), isAdmin));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<NotificationDTO> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(notificationService.markAsRead(id, user.userId()));
    }

    @PutMapping("/my/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark all notifications as read")
    public void markAllAsRead(@AuthenticationPrincipal AuthenticatedUser user) {
        notificationService.markAllReadForUser(user.userId());
    }

    /**
     * SSE stream: client subscribes once and receives real-time notification pushes.
     * Keep-alive comments sent every 30 seconds to prevent proxy timeout.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE stream of real-time notifications")
    public SseEmitter stream(@AuthenticationPrincipal AuthenticatedUser user) {
        var emitter = inAppPushService.register(user.userId());
        // Schedule keep-alive every 30 seconds
        var future = keepAliveScheduler.scheduleAtFixedRate(
                () -> inAppPushService.keepAlive(user.userId(), emitter),
                30, 30, TimeUnit.SECONDS);
        emitter.onCompletion(() -> future.cancel(true));
        emitter.onTimeout(() -> future.cancel(true));
        emitter.onError(e -> future.cancel(true));
        return emitter;
    }
}
