package com.hieu.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** Immutable read model for notification data. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationDTO(
        Long id,
        String userId,
        String type,
        String channel,
        String title,
        String content,
        String status,
        boolean isRead,
        String errorMessage,
        String referenceType,
        String referenceId,
        Instant createdAt,
        Instant sentAt,
        Instant readAt
) {}
