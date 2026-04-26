package com.hieu.inventory_service.service;

import com.hieu.inventory_service.dto.*;
import com.hieu.inventory_service.entity.InventoryEntity;
import com.hieu.inventory_service.entity.StockReservationRecord;
import com.hieu.inventory_service.entity.StockReservationRecord.ReservationStatus;
import com.hieu.inventory_service.exception.InsufficientStockException;
import com.hieu.inventory_service.exception.InventoryNotFoundException;
import com.hieu.inventory_service.kafka.LowStockEventPublisher;
import com.hieu.inventory_service.repository.InventoryRepository;
import com.hieu.inventory_service.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.function.Function;

/**
 * Core business logic for inventory management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;
    private final StockRedisService redisService;
    private final LowStockEventPublisher lowStockPublisher;
    private final ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    @Transactional
    public InventoryDTO create(Long productId, String sku, Integer quantity, Integer minStockLevel) {
        var entity = InventoryEntity.builder()
            .productId(productId)
            .sku(sku)
            .quantity(quantity)
            .reservedQuantity(0)
            .minStockLevel(minStockLevel != null ? minStockLevel : 10)
            .build();
        var saved = inventoryRepository.save(entity);
        redisService.setStock(productId, saved.getAvailableQuantity());
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public InventoryDTO getByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
            .map(this::toDTO)
            .orElseThrow(() -> new InventoryNotFoundException(productId));
    }

    @Transactional(readOnly = true)
    public InventoryDTO getBySku(String sku) {
        return inventoryRepository.findBySku(sku)
            .map(this::toDTO)
            .orElseThrow(() -> new InventoryNotFoundException("SKU: " + sku));
    }

    @Transactional(readOnly = true)
    public PageDTO<InventoryDTO> getAll(int page, int size) {
        Page<InventoryEntity> p = inventoryRepository.findAll(PageRequest.of(page, size));
        return new PageDTO<>(
            p.getContent().stream().map(this::toDTO).toList(),
            p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Transactional
    public InventoryDTO adjustStock(Long productId, int delta) {
        var inventories = inventoryRepository.findAllByProductIdInWithLock(List.of(productId));
        var entity = inventories.stream().findFirst()
            .orElseThrow(() -> new InventoryNotFoundException(productId));
        if (delta > 0) {
            entity.addStock(delta);
        } else if (delta < 0) {
            // Negative delta = shrink total quantity; guard against going below reserved.
            int reduction = -delta;
            if (entity.getQuantity() - reduction < entity.getReservedQuantity()) {
                throw new IllegalArgumentException(
                    "Cannot reduce quantity below reserved level: quantity=" + entity.getQuantity()
                    + " reserved=" + entity.getReservedQuantity() + " reduction=" + reduction);
            }
            entity.subtractStock(reduction);
        }
        var saved = inventoryRepository.save(entity);
        redisService.setStock(productId, saved.getAvailableQuantity());
        return toDTO(saved);
    }

    // -------------------------------------------------------------------------
    // Reserve
    // -------------------------------------------------------------------------

    @Transactional
    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public ReservationResult reserveStock(ReservationRequest request) {
        // Idempotency
        var existing = reservationRepository.findByOrderId(request.orderId());
        if (existing.isPresent()) {
            log.info("Idempotent reserve: orderId={} status={}", request.orderId(), existing.get().getStatus());
            return ReservationResult.success(request.orderId());
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        for (var item : request.items()) {
            if (item.quantity() <= 0) {
                throw new IllegalArgumentException("quantity must be positive for productId=" + item.productId());
            }
        }

        var itemMap = request.items().stream()
            .collect(Collectors.toMap(
                ReservationRequest.ReservationItem::productId,
                ReservationRequest.ReservationItem::quantity));

        // Atomic Redis check
        int redisResult = redisService.reserveStockAtomically(itemMap);
        if (redisResult == -1) {
            log.info("Redis cache miss for order {}, loading from DB", request.orderId());
            var inventories = inventoryRepository.findAllByProductIdInWithLock(itemMap.keySet().stream().toList());
            for (var inv : inventories) {
                redisService.setStock(inv.getProductId(), inv.getAvailableQuantity());
            }
            redisResult = redisService.reserveStockAtomically(itemMap);
        }
        if (redisResult == 0) {
            throw new InsufficientStockException("Insufficient stock for order " + request.orderId());
        }

        // DB reservation with pessimistic lock
        try {
            var inventories = inventoryRepository.findAllByProductIdInWithLock(itemMap.keySet().stream().toList());
            var invMap = inventories.stream()
                .collect(Collectors.toMap(InventoryEntity::getProductId, Function.identity()));

            for (var entry : itemMap.entrySet()) {
                var inv = invMap.get(entry.getKey());
                if (inv == null) throw new InventoryNotFoundException(entry.getKey());
                inv.reserve(entry.getValue());
            }
            inventoryRepository.saveAll(inventories);

            var record = StockReservationRecord.builder()
                .orderId(request.orderId())
                .items(serializeItems(itemMap))
                .status(ReservationStatus.ACTIVE)
                .build();
            reservationRepository.save(record);

            // Publish low-stock events
            inventories.forEach(inv -> {
                if (inv.isLowStock()) lowStockPublisher.publishIfLowStock(inv);
            });

            return ReservationResult.success(request.orderId());
        } catch (Exception e) {
            // Rollback Redis
            for (var entry : itemMap.entrySet()) {
                redisService.releaseStock(entry.getKey(), entry.getValue());
            }
            throw e;
        }
    }

    @Recover
    public ReservationResult recoverReserve(ObjectOptimisticLockingFailureException ex, ReservationRequest request) {
        log.warn("Reserve stock failed after retries for order {}: {}", request.orderId(), ex.getMessage());
        return ReservationResult.failure("stock conflict, retry later");
    }

    // -------------------------------------------------------------------------
    // Confirm
    // -------------------------------------------------------------------------

    @Transactional
    public ReservationResult confirmReservation(String orderId) {
        var record = reservationRepository.findByOrderId(orderId)
            .orElse(null);
        if (record == null || record.getStatus() != ReservationStatus.ACTIVE) {
            log.info("Confirm idempotent: orderId={}", orderId);
            return ReservationResult.success(orderId);
        }

        var itemMap = deserializeItems(record.getItems());
        var inventories = inventoryRepository.findAllByProductIdInWithLock(itemMap.keySet().stream().toList());
        var invMap = inventories.stream()
            .collect(Collectors.toMap(InventoryEntity::getProductId, Function.identity()));

        for (var entry : itemMap.entrySet()) {
            var inv = invMap.get(entry.getKey());
            if (inv != null) inv.confirmReservation(entry.getValue());
        }
        inventoryRepository.saveAll(inventories);

        record.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(record);

        inventories.forEach(inv -> {
            if (inv.isLowStock()) lowStockPublisher.publishIfLowStock(inv);
        });

        return ReservationResult.success(orderId);
    }

    // -------------------------------------------------------------------------
    // Release
    // -------------------------------------------------------------------------

    @Transactional
    public ReservationResult releaseReservation(String orderId) {
        var record = reservationRepository.findByOrderId(orderId)
            .orElse(null);
        if (record == null || record.getStatus() != ReservationStatus.ACTIVE) {
            log.info("Release idempotent: orderId={}", orderId);
            return ReservationResult.success(orderId);
        }

        // Mark RELEASED first to prevent double-release
        record.setStatus(ReservationStatus.RELEASED);
        reservationRepository.save(record);

        var itemMap = deserializeItems(record.getItems());
        var inventories = inventoryRepository.findAllByProductIdInWithLock(itemMap.keySet().stream().toList());

        for (var inv : inventories) {
            var qty = itemMap.get(inv.getProductId());
            if (qty != null) {
                inv.releaseReservation(qty);
                redisService.releaseStock(inv.getProductId(), qty);
            }
        }
        inventoryRepository.saveAll(inventories);

        return ReservationResult.success(orderId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String serializeItems(Map<Long, Integer> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize items", e);
        }
    }

    /**
     * JSON-deserialise {@code items} column. Jackson defaults to String keys when the
     * target is raw {@code Map}, so we parse into String keys then rebuild a typed
     * {@code Map<Long, Integer>} for stable downstream lookups by product id.
     */
    private Map<Long, Integer> deserializeItems(String json) {
        try {
            Map<String, Integer> raw = objectMapper.readValue(json,
                new tools.jackson.core.type.TypeReference<Map<String, Integer>>() {});
            Map<Long, Integer> result = new java.util.LinkedHashMap<>(raw.size());
            raw.forEach((k, v) -> result.put(Long.parseLong(k), v));
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize items: " + json, e);
        }
    }

    public InventoryDTO toDTO(InventoryEntity e) {
        return InventoryDTO.builder()
            .id(e.getId())
            .productId(e.getProductId())
            .sku(e.getSku())
            .quantity(e.getQuantity())
            .reservedQuantity(e.getReservedQuantity())
            .availableQuantity(e.getAvailableQuantity())
            .minStockLevel(e.getMinStockLevel())
            .lowStock(e.isLowStock())
            .lastUpdated(e.getLastUpdated())
            .version(e.getVersion())
            .build();
    }
}
