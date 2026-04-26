package com.hieu.flash_sale_service.dto;

/** Response for POST /api/flash-sales/{id}/participate. */
public record ParticipateResponse(
        boolean success,
        Long participationId,
        int remainingSlots,
        String reason
) {}
