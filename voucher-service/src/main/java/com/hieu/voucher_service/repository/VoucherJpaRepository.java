package com.hieu.voucher_service.repository;

import com.hieu.voucher_service.entity.VoucherJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface VoucherJpaRepository extends JpaRepository<VoucherJpaEntity, Long> {

    Optional<VoucherJpaEntity> findByCode(String code);

    Page<VoucherJpaEntity> findByActiveTrue(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VoucherJpaEntity v WHERE v.code = :code")
    Optional<VoucherJpaEntity> findByCodeForUpdate(@Param("code") String code);

    /** Returns only vouchers whose time window contains :now (active=true AND startDate<=now AND endDate>now). */
    @Query("SELECT v FROM VoucherJpaEntity v WHERE v.active = true AND v.startDate <= :now AND v.endDate > :now")
    Page<VoucherJpaEntity> findActiveAtTime(@Param("now") Instant now, Pageable pageable);
}
