package com.hieu.order_service.infrastructure.messaging;

import com.hieu.order_service.application.events.KafkaTopics;
import com.hieu.order_service.application.events.OrderIntegrationEvents;
import com.hieu.order_service.domain.events.DomainEvent;
import com.hieu.order_service.domain.events.order.*;
import com.hieu.order_service.domain.events.returnrequest.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for {@link DomainEventToIntegrationMapper} — the outbox
 * domain→integration event mapping. Verifies topic routing, partition key =
 * aggregateId, payload field mapping (incl. OrderPlaced address/item snapshot),
 * the in-process-only events that map to null ("drop"), and the loud failure
 * on an unmapped event type.
 */
@DisplayName("DomainEventToIntegrationMapper")
class DomainEventToIntegrationMapperTest {

    private final DomainEventToIntegrationMapper mapper = new DomainEventToIntegrationMapper();

    @Test
    @DisplayName("OrderPlacedEvent → order.placed topic, key=aggregateId, flattened address + items")
    void orderPlaced_mapsSnapshot() {
        var address = new OrderPlacedEvent.AddressSnapshot(
                "Nguyen Van A", "0901234567",
                "123 Le Loi", "Ben Thanh", "District 1", "Ho Chi Minh", "VN", "70000");
        var item = new OrderPlacedEvent.ItemSnapshot(
                1L, "Product A", 10L, "SKU-001", BigDecimal.valueOf(100_000), 2);
        var event = new OrderPlacedEvent(
                7L, "ORD-1", "user-1", BigDecimal.valueOf(200_000), "COD", address, List.of(item));

        var routed = mapper.map(event);

        assertThat(routed.topic()).isEqualTo(KafkaTopics.ORDER_PLACED);
        assertThat(routed.key()).isEqualTo("7");
        assertThat(routed.payload()).isInstanceOf(OrderIntegrationEvents.OrderPlaced.class);
        var payload = (OrderIntegrationEvents.OrderPlaced) routed.payload();
        assertThat(payload.orderId()).isEqualTo(7L);
        assertThat(payload.orderNumber()).isEqualTo("ORD-1");
        assertThat(payload.userId()).isEqualTo("user-1");
        assertThat(payload.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(200_000));
        assertThat(payload.street()).isEqualTo("123 Le Loi");
        assertThat(payload.ward()).isEqualTo("Ben Thanh");
        assertThat(payload.district()).isEqualTo("District 1");
        assertThat(payload.city()).isEqualTo("Ho Chi Minh");
        assertThat(payload.country()).isEqualTo("VN");
        assertThat(payload.items()).singleElement()
                .satisfies(i -> {
                    assertThat(i.productId()).isEqualTo(1L);
                    assertThat(i.productName()).isEqualTo("Product A");
                    assertThat(i.quantity()).isEqualTo(2);
                    assertThat(i.unitPrice()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
                });
    }

    @Test
    @DisplayName("OrderConfirmedEvent → order.confirmed, paymentId carried")
    void orderConfirmed() {
        var routed = mapper.map(new OrderConfirmedEvent(7L, "ORD-1", "user-1", 99L));
        assertThat(routed.topic()).isEqualTo(KafkaTopics.ORDER_CONFIRMED);
        assertThat(routed.key()).isEqualTo("7");
        var p = (OrderIntegrationEvents.OrderConfirmed) routed.payload();
        assertThat(p.paymentId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("OrderShippedEvent → order.shipped, shipmentId carried")
    void orderShipped() {
        var routed = mapper.map(new OrderShippedEvent(7L, "ORD-1", "user-1", 55L));
        assertThat(routed.topic()).isEqualTo(KafkaTopics.ORDER_SHIPPED);
        var p = (OrderIntegrationEvents.OrderShipped) routed.payload();
        assertThat(p.shipmentId()).isEqualTo(55L);
    }

    @Test
    @DisplayName("OrderDeliveredEvent → order.delivered")
    void orderDelivered() {
        var routed = mapper.map(new OrderDeliveredEvent(7L, "ORD-1", "user-1"));
        assertThat(routed.topic()).isEqualTo(KafkaTopics.ORDER_DELIVERED);
        assertThat(routed.payload()).isInstanceOf(OrderIntegrationEvents.OrderDelivered.class);
    }

    @Test
    @DisplayName("OrderCancelledEvent → order.cancelled, reason + voucherCode carried")
    void orderCancelled() {
        var routed = mapper.map(new OrderCancelledEvent(7L, "ORD-1", "user-1", "đổi ý", "SUMMER20"));
        assertThat(routed.topic()).isEqualTo(KafkaTopics.ORDER_CANCELLED);
        var p = (OrderIntegrationEvents.OrderCancelled) routed.payload();
        assertThat(p.reason()).isEqualTo("đổi ý");
        assertThat(p.voucherCode()).isEqualTo("SUMMER20");
    }

    @Test
    @DisplayName("OrderFailedEvent → order.failed, reason carried")
    void orderFailed() {
        var routed = mapper.map(new OrderFailedEvent(7L, "ORD-1", "user-1", "payment timeout"));
        assertThat(routed.topic()).isEqualTo(KafkaTopics.ORDER_FAILED);
        var p = (OrderIntegrationEvents.OrderFailed) routed.payload();
        assertThat(p.reason()).isEqualTo("payment timeout");
    }

    @Test
    @DisplayName("OrderReturnRequestedEvent → order.return-requested, ids + reason carried")
    void returnRequested() {
        var routed = mapper.map(new OrderReturnRequestedEvent(7L, 50L, "user-1", "hỏng"));
        assertThat(routed.topic()).isEqualTo(KafkaTopics.ORDER_RETURN_REQUESTED);
        var p = (OrderIntegrationEvents.OrderReturnRequested) routed.payload();
        assertThat(p.returnRequestId()).isEqualTo(50L);
        assertThat(p.reason()).isEqualTo("hỏng");
    }

    @Test
    @DisplayName("OrderReturnApprovedEvent → order.return-approved")
    void returnApproved() {
        var routed = mapper.map(new OrderReturnApprovedEvent(7L, 50L, "user-1"));
        assertThat(routed.topic()).isEqualTo(KafkaTopics.ORDER_RETURN_APPROVED);
        assertThat(((OrderIntegrationEvents.OrderReturnApproved) routed.payload()).returnRequestId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("OrderReturnRejectedEvent → order.return-rejected")
    void returnRejected() {
        var routed = mapper.map(new OrderReturnRejectedEvent(7L, 50L, "user-1"));
        assertThat(routed.topic()).isEqualTo(KafkaTopics.ORDER_RETURN_REJECTED);
        assertThat(((OrderIntegrationEvents.OrderReturnRejected) routed.payload()).returnRequestId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("OrderReturnedEvent → order.returned, refundAmount carried")
    void returned() {
        var routed = mapper.map(new OrderReturnedEvent(7L, 50L, "user-1", BigDecimal.valueOf(99_000)));
        assertThat(routed.topic()).isEqualTo(KafkaTopics.ORDER_RETURNED);
        var p = (OrderIntegrationEvents.OrderReturned) routed.payload();
        assertThat(p.refundAmount()).isEqualByComparingTo(BigDecimal.valueOf(99_000));
    }

    @Test
    @DisplayName("in-process-only events map to null (dropped, never sent to Kafka)")
    void inProcessEvents_droppedAsNull() {
        assertThat(mapper.map(new OrderInventoryReservedEvent(7L, "ORD-1", "res-1"))).isNull();
        assertThat(mapper.map(new OrderPaymentInitiatedEvent(7L, "ORD-1", 99L))).isNull();
        assertThat(mapper.map(new OrderPaymentCompletedEvent(7L, "ORD-1", "user-1"))).isNull();
    }

    @Test
    @DisplayName("unmapped DomainEvent type → IllegalStateException (fail loud)")
    void unmapped_throws() {
        DomainEvent unknown = new DomainEvent() {
            @Override public java.util.UUID eventId() { return java.util.UUID.randomUUID(); }
            @Override public java.time.Instant occurredOn() { return java.time.Instant.now(); }
            @Override public String aggregateId() { return "x"; }
        };
        assertThatThrownBy(() -> mapper.map(unknown))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unmapped DomainEvent");
    }
}
