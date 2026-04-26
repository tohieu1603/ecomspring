package com.hieu.inventory_service.controller;

import com.hieu.inventory_service.dto.*;
import com.hieu.inventory_service.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** REST API for inventory management. */
@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Stock management APIs")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create inventory entry")
    public ResponseEntity<InventoryDTO> create(@Valid @RequestBody CreateInventoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(inventoryService.create(req.productId(), req.sku(), req.quantity(), req.minStockLevel()));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory by productId")
    public ResponseEntity<InventoryDTO> getByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getByProductId(productId));
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get inventory by SKU")
    public ResponseEntity<InventoryDTO> getBySku(@PathVariable String sku) {
        return ResponseEntity.ok(inventoryService.getBySku(sku));
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all inventories (paginated)")
    public ResponseEntity<PageDTO<InventoryDTO>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(inventoryService.getAll(page, size));
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock for an order (internal)")
    public ResponseEntity<ReservationResult> reserve(@Valid @RequestBody ReservationRequest req) {
        return ResponseEntity.ok(inventoryService.reserveStock(req));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm reservation (internal)")
    public ResponseEntity<Map<String, Boolean>> confirm(@RequestParam String orderId) {
        var result = inventoryService.confirmReservation(orderId);
        return ResponseEntity.ok(Map.of("success", result.success()));
    }

    @PostMapping("/release")
    @Operation(summary = "Release reservation (internal)")
    public ResponseEntity<Map<String, Boolean>> release(@RequestParam String orderId) {
        var result = inventoryService.releaseReservation(orderId);
        return ResponseEntity.ok(Map.of("success", result.success()));
    }

    @PatchMapping("/{productId}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Adjust stock delta")
    public ResponseEntity<InventoryDTO> adjustStock(
        @PathVariable Long productId,
        @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(inventoryService.adjustStock(productId, body.get("delta")));
    }
}
