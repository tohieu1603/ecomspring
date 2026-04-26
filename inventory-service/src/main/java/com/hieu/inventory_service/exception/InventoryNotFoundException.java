package com.hieu.inventory_service.exception;

/** Thrown when an inventory record cannot be found. */
public class InventoryNotFoundException extends RuntimeException {

    public InventoryNotFoundException(Long productId) {
        super("Inventory not found for productId: " + productId);
    }

    public InventoryNotFoundException(String message) {
        super(message);
    }
}
