package com.hieu.order_service.infrastructure.persistence.jpa.repositories;

import com.hieu.order_service.infrastructure.persistence.jpa.entities.OrderJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

    @Query("SELECT o FROM OrderJpaEntity o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<OrderJpaEntity> findByIdWithItems(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderJpaEntity o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<OrderJpaEntity> findByIdWithLock(@Param("id") Long id);

    Optional<OrderJpaEntity> findByOrderNumber(String orderNumber);

    @Query("SELECT o.id FROM OrderJpaEntity o ORDER BY o.createdAt DESC, o.id DESC")
    List<Long> findFirstPageIds(Pageable pageable);

    @Query("SELECT o.id FROM OrderJpaEntity o WHERE (o.createdAt < :createdAt) OR (o.createdAt = :createdAt AND o.id < :id) ORDER BY o.createdAt DESC, o.id DESC")
    List<Long> findIdsAfterCursor(@Param("createdAt") Instant createdAt, @Param("id") Long id, Pageable pageable);

    @Query("SELECT o.id FROM OrderJpaEntity o WHERE o.status = :status ORDER BY o.createdAt DESC, o.id DESC")
    List<Long> findFirstPageIdsByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT o.id FROM OrderJpaEntity o WHERE o.status = :status AND ((o.createdAt < :createdAt) OR (o.createdAt = :createdAt AND o.id < :id)) ORDER BY o.createdAt DESC, o.id DESC")
    List<Long> findIdsAfterCursorByStatus(@Param("status") String status, @Param("createdAt") Instant createdAt, @Param("id") Long id, Pageable pageable);

    @Query("SELECT o FROM OrderJpaEntity o LEFT JOIN FETCH o.items WHERE o.id IN :ids")
    List<OrderJpaEntity> findAllByIdInWithItems(@Param("ids") List<Long> ids);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "items")
    Page<OrderJpaEntity> findByUserId(String userId, Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "items")
    Page<OrderJpaEntity> findByUserIdAndStatus(String userId, String status, Pageable pageable);

    @Query("SELECT o.id FROM OrderJpaEntity o WHERE o.userId = :userId ORDER BY o.createdAt DESC, o.id DESC")
    List<Long> findFirstPageIdsByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT o.id FROM OrderJpaEntity o WHERE o.userId = :userId AND ((o.createdAt < :createdAt) OR (o.createdAt = :createdAt AND o.id < :id)) ORDER BY o.createdAt DESC, o.id DESC")
    List<Long> findIdsAfterCursorByUserId(@Param("userId") String userId, @Param("createdAt") Instant createdAt, @Param("id") Long id, Pageable pageable);

    @Query("SELECT COUNT(o) > 0 FROM OrderJpaEntity o JOIN o.items i WHERE o.userId = :userId AND i.productId = :productId AND o.status IN ('DELIVERED', 'RETURNED')")
    boolean existsByUserIdAndProductId(@Param("userId") String userId, @Param("productId") Long productId);
}
