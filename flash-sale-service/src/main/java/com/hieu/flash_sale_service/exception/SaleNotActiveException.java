package com.hieu.flash_sale_service.exception;

/** Thrown when a flash sale is not in ACTIVE state or outside its time window. */
public class SaleNotActiveException extends RuntimeException {

    public SaleNotActiveException(Long saleId) {
        super("Flash sale id=" + saleId + " is not active");
    }

    public SaleNotActiveException(Long saleId, String detail) {
        super("Flash sale id=" + saleId + " is not active: " + detail);
    }
}
