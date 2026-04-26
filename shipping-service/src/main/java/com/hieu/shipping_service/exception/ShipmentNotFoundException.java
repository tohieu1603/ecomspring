package com.hieu.shipping_service.exception;

public class ShipmentNotFoundException extends RuntimeException {
    public ShipmentNotFoundException(Long id) {
        super("Shipment not found: " + id);
    }
    public ShipmentNotFoundException(String detail) {
        super("Shipment not found: " + detail);
    }
}
