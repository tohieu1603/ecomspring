package com.hieu.notification_service.service;

import com.hieu.notification_service.dto.CursorPageDTO;
import com.hieu.notification_service.dto.NotificationDTO;
import com.hieu.notification_service.dto.PageDTO;
import com.hieu.notification_service.dto.SendNotificationRequest;
import com.hieu.notification_service.entity.NotificationJpaEntity;
import com.hieu.notification_service.entity.NotificationStatus;
import com.hieu.notification_service.entity.NotificationType;
import com.hieu.notification_service.exception.AccessDeniedException;
import com.hieu.notification_service.exception.NotificationNotFoundException;
import com.hieu.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationApplicationService {

    private final NotificationRepository repository;
    private final EmailService emailService;
    private final InAppPushService inAppPushService;

    /**
     * Persists the notification as PENDING, dispatches delivery based on type,
     * updates status, then pushes to SSE for IN_APP.
     */
    @Transactional
    public NotificationDTO send(SendNotificationRequest req) {
        var entity = NotificationJpaEntity.builder()
                .userId(req.getUserId())
                .type(req.getType().name())
                .channel(req.getChannel())
                .title(req.getTitle())
                .content(req.getContent())
                .status(NotificationStatus.PENDING.name())
                .referenceType(req.getReferenceType())
                .referenceId(req.getReferenceId())
                .build();
        entity = repository.save(entity);

        if (req.getType() == NotificationType.EMAIL) {
            sendEmailAsync(entity, req.getChannel(), req.getTitle(), req.getContent());
        } else if (req.getType() == NotificationType.IN_APP) {
            entity.setStatus(NotificationStatus.SENT.name());
            entity.setSentAt(Instant.now());
            entity = repository.save(entity);
            var dto = toDTO(entity);
            inAppPushService.push(req.getUserId(), dto);
            return dto;
        } else {
            // SMS / PUSH — mocked
            log.info("{} notification not yet implemented, marking SENT for userId={}", req.getType(), req.getUserId());
            entity.setStatus(NotificationStatus.SENT.name());
            entity.setSentAt(Instant.now());
            entity = repository.save(entity);
        }

        return toDTO(entity);
    }

    /** Async email dispatch — updates entity status in a new transaction after SMTP call. */
    @Async("mailExecutor")
    public void sendEmailAsync(NotificationJpaEntity snapshot, String channel, String title, String content) {
        try {
            emailService.send(channel, title, content);
            updateEmailStatus(snapshot.getId(), NotificationStatus.SENT, null);
        } catch (Exception e) {
            log.error("Email notification id={} failed: {}", snapshot.getId(), e.getMessage());
            updateEmailStatus(snapshot.getId(), NotificationStatus.FAILED,
                    e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
    }

    @Transactional
    protected void updateEmailStatus(Long id, NotificationStatus status, String errorMsg) {
        repository.findById(id).ifPresent(n -> {
            n.setStatus(status.name());
            if (status == NotificationStatus.SENT) n.setSentAt(Instant.now());
            if (errorMsg != null) n.setErrorMessage(errorMsg);
            repository.save(n);
        });
    }

    @Transactional(readOnly = true)
    public PageDTO<NotificationDTO> getMyNotifications(String userId, int page, int size) {
        Page<NotificationJpaEntity> p = repository.findByUserIdOrderByCreatedAtDescIdDesc(
                userId, PageRequest.of(page, size));
        return new PageDTO<>(
                p.getContent().stream().map(this::toDTO).toList(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages(),
                p.isLast());
    }

    /**
     * Cursor pagination: id &lt; cursor (newest first).
     * First call: pass cursor=null (defaults to Long.MAX_VALUE).
     */
    @Transactional(readOnly = true)
    public CursorPageDTO<NotificationDTO> getMyFeed(String userId, Long cursor, int size) {
        int clamped = Math.max(1, Math.min(size, 100));
        long safeCursor = cursor != null ? cursor : Long.MAX_VALUE;

        List<NotificationJpaEntity> fetched = repository.findByUserIdAfterCursor(
                userId, safeCursor, PageRequest.ofSize(clamped + 1));

        boolean hasNext = fetched.size() > clamped;
        var page = hasNext ? fetched.subList(0, clamped) : fetched;
        var items = page.stream().map(this::toDTO).toList();
        Long nextCursor = hasNext ? page.get(page.size() - 1).getId() : null;
        return new CursorPageDTO<>(items, nextCursor, items.size(), hasNext);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return repository.countByUserIdAndIsRead(userId, false);
    }

    @Transactional(readOnly = true)
    public NotificationDTO getById(Long id, String userId, boolean isAdmin) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found: " + id));
        if (!isAdmin && !entity.getUserId().equals(userId)) {
            throw new AccessDeniedException("Notification does not belong to this user");
        }
        return toDTO(entity);
    }

    @Transactional
    public NotificationDTO markAsRead(Long id, String userId) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found: " + id));
        if (!entity.getUserId().equals(userId)) {
            throw new AccessDeniedException("Notification does not belong to this user");
        }
        entity.setRead(true);
        entity.setReadAt(Instant.now());
        if (NotificationStatus.SENT.name().equals(entity.getStatus())) {
            entity.setStatus(NotificationStatus.READ.name());
        }
        return toDTO(repository.save(entity));
    }

    @Transactional
    public void markAllReadForUser(String userId) {
        repository.markAllAsReadByUserId(userId, Instant.now(), NotificationStatus.READ.name());
    }

    // ── mapping ──────────────────────────────────────────────────────────────

    private NotificationDTO toDTO(NotificationJpaEntity e) {
        return new NotificationDTO(
                e.getId(), e.getUserId(), e.getType(), e.getChannel(),
                e.getTitle(), e.getContent(), e.getStatus(), e.isRead(),
                e.getErrorMessage(), e.getReferenceType(), e.getReferenceId(),
                e.getCreatedAt(), e.getSentAt(), e.getReadAt());
    }
}
