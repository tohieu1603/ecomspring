package com.hieu.notification_service.repository;

import com.hieu.notification_service.entity.NotificationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationJpaEntity, Long> {

    Page<NotificationJpaEntity> findByUserIdOrderByCreatedAtDescIdDesc(String userId, Pageable pageable);

    /** Cursor pagination: id < cursor, newest first. */
    @Query("SELECT n FROM NotificationJpaEntity n WHERE n.userId = :userId AND n.id < :cursor ORDER BY n.id DESC")
    List<NotificationJpaEntity> findByUserIdAfterCursor(
            @Param("userId") String userId,
            @Param("cursor") long cursor,
            Pageable pageable);

    long countByUserIdAndIsRead(String userId, boolean isRead);

    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.isRead = true, n.readAt = :now, n.status = :status, n.updatedAt = :now " +
           "WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(
            @Param("userId") String userId,
            @Param("now") Instant now,
            @Param("status") String status);
}
