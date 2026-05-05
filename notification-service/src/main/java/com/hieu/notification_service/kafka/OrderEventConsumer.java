package com.hieu.notification_service.kafka;

import com.hieu.notification_service.dto.SendNotificationRequest;
import com.hieu.notification_service.entity.NotificationType;
import com.hieu.notification_service.service.NotificationApplicationService;
import com.hieu.notification_service.service.UserProfileEmailResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes order.* topics and triggers IN_APP (+ EMAIL when channel available) notifications.
 * Payload is an untyped {@link Map} via JSON deserialisation.
 * If payload doesn't carry email, falls back to userProfileGrpcClient.lookupEmail().
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final NotificationApplicationService notificationService;
    private final UserProfileEmailResolver emailResolver;

    @KafkaListener(topics = KafkaTopics.ORDER_PLACED, groupId = "notification-service")
    public void onOrderPlaced(Map<String, Object> payload) {
        var userId = str(payload, "userId");
        var orderNumber = str(payload, "orderNumber");
        var title = "Đơn hàng " + orderNumber + " đã được đặt";
        var content = "Đơn hàng " + orderNumber + " của bạn đã được đặt thành công.";
        send(userId, title, content, "ORDER", orderNumber, str(payload, "email"), true);
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CONFIRMED, groupId = "notification-service")
    public void onOrderConfirmed(Map<String, Object> payload) {
        var userId = str(payload, "userId");
        var orderNumber = str(payload, "orderNumber");
        var title = "Đơn hàng " + orderNumber + " đã được xác nhận";
        send(userId, title, title, "ORDER", orderNumber, null, false);
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CANCELLED, groupId = "notification-service")
    public void onOrderCancelled(Map<String, Object> payload) {
        var userId = str(payload, "userId");
        var orderNumber = str(payload, "orderNumber");
        var title = "Đơn hàng " + orderNumber + " đã hủy";
        send(userId, title, title, "ORDER", orderNumber, str(payload, "email"), true);
    }

    @KafkaListener(topics = KafkaTopics.ORDER_SHIPPED, groupId = "notification-service")
    public void onOrderShipped(Map<String, Object> payload) {
        var userId = str(payload, "userId");
        var orderNumber = str(payload, "orderNumber");
        var title = "Đơn " + orderNumber + " đã được giao cho đơn vị vận chuyển";
        send(userId, title, title, "ORDER", orderNumber, str(payload, "email"), true);
    }

    @KafkaListener(topics = KafkaTopics.ORDER_DELIVERED, groupId = "notification-service")
    public void onOrderDelivered(Map<String, Object> payload) {
        var userId = str(payload, "userId");
        var orderNumber = str(payload, "orderNumber");
        var title = "Đơn hàng " + orderNumber + " đã được giao thành công";
        send(userId, title, title, "ORDER", orderNumber, str(payload, "email"), true);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void send(String userId, String title, String content,
                      String refType, String refId, String email, boolean sendEmail) {
        // IN_APP always
        notificationService.send(SendNotificationRequest.builder()
                .userId(userId).type(NotificationType.IN_APP)
                .title(title).content(content)
                .referenceType(refType).referenceId(refId)
                .build());

        if (!sendEmail) return;

        // Resolve email: payload first, then gRPC fallback
        String resolvedEmail = (email != null && !email.isBlank())
                ? email
                : emailResolver.lookupEmail(userId).orElse(null);

        if (resolvedEmail != null) {
            notificationService.send(SendNotificationRequest.builder()
                    .userId(userId).type(NotificationType.EMAIL)
                    .channel(resolvedEmail).title(title).content(content)
                    .referenceType(refType).referenceId(refId)
                    .build());
        } else {
            log.debug("No email resolved for userId={}, skipping EMAIL notification", userId);
        }
    }

    private static String str(Map<String, Object> m, String key) {
        var v = m.get(key);
        return v != null ? v.toString() : "";
    }
}
