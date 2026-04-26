package com.hieu.payment_service.controller;

import com.hieu.common.api.ApiResponse;
import com.hieu.common.security.AuthenticatedUser;
import com.hieu.payment_service.dto.*;
import com.hieu.payment_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Payment management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Initiate a new payment")
    public ResponseEntity<ApiResponse<PaymentDTO>> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        PaymentDTO dto = paymentService.initiatePayment(user.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(dto, "Payment initiated"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPayment(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        boolean isAdmin = user.hasRole("ROLE_ADMIN") || user.hasRole("ADMIN");
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPayment(id, user.userId(), isAdmin)));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment by order ID")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPaymentByOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPaymentByOrder(orderId)));
    }

    @GetMapping("/my")
    @Operation(summary = "List my payments (paginated)")
    public ResponseEntity<ApiResponse<PageDTO<PaymentDTO>>> getMyPayments(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getMyPayments(user.userId(), page, size)));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm payment (PENDING → PAID)")
    public ResponseEntity<ApiResponse<PaymentDTO>> confirmPayment(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.confirmPayment(id, user.userId(), request.getTransactionId()), "Payment confirmed"));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel payment (PENDING/FAILED → CANCELLED)")
    public ResponseEntity<ApiResponse<PaymentDTO>> cancelPayment(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.cancelPayment(id, user.userId()), "Payment cancelled"));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Request refund (PAID → REFUND_REQUESTED)")
    public ResponseEntity<ApiResponse<PaymentDTO>> requestRefund(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody RefundRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.requestRefund(id, user.userId(), request.getReason()), "Refund requested"));
    }

    @PostMapping("/{id}/process-refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process refund (REFUND_REQUESTED → REFUNDED) — Admin only")
    public ResponseEntity<ApiResponse<PaymentDTO>> processRefund(
            @PathVariable Long id,
            @RequestBody(required = false) RefundRequest request) {
        java.math.BigDecimal amount = (request != null) ? request.getRefundAmount() : null;
        String reason = (request != null) ? request.getReason() : null;
        return ResponseEntity.ok(ApiResponse.ok(paymentService.processRefund(id, amount, reason), "Refund processed"));
    }
}
