package com.hieu.inventory_service.repository;

import com.hieu.inventory_service.entity.StockReservationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Data access for {@link StockReservationRecord}. */
@Repository
public interface StockReservationRepository extends JpaRepository<StockReservationRecord, Long> {

    Optional<StockReservationRecord> findByOrderId(String orderId);
}
