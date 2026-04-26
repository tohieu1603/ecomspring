package com.hieu.voucher_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Theo dõi per-user voucher usage để enforce usageLimitPerUser.
 * Được tạo khi apply thành công, xóa khi release.
 */
@Entity
@Table(
    name = "voucher_usage_records",
    indexes = {
        @Index(name = "idx_usage_voucher_user", columnList = "voucher_id, user_id"),
        @Index(name = "idx_usage_order", columnList = "order_id")
    }
)
public class VoucherUsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK-like reference (không dùng @ManyToOne để tránh join cost). */
    @Column(name = "voucher_id", nullable = false)
    private Long voucherId;

    /** User đã apply voucher. Dùng String để match auth-service (UUID string). */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /** Order ID — dùng cho idempotent release. */
    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private Instant usedAt;

    // Required by JPA
    public VoucherUsageRecord() {}

    public VoucherUsageRecord(Long voucherId, String userId, String orderId) {
        this.voucherId = voucherId;
        this.userId = userId;
        this.orderId = orderId;
        this.usedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getVoucherId() { return voucherId; }
    public String getUserId() { return userId; }
    public String getOrderId() { return orderId; }
    public Instant getUsedAt() { return usedAt; }
}
