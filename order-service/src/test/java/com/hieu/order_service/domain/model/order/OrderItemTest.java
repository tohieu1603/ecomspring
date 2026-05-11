package com.hieu.order_service.domain.model.order;

import com.hieu.order_service.domain.model.order.valueobject.Money;
import com.hieu.order_service.domain.model.order.valueobject.ProductId;
import com.hieu.order_service.domain.model.order.valueobject.ProductName;
import com.hieu.order_service.domain.model.order.valueobject.Quantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OrderItem")
class OrderItemTest {

    static OrderItem item(long unitPrice, int qty) {
        return OrderItem.create(
                ProductId.of(1L),
                ProductName.of("Áo thun"),
                10L, "SKU-001", null,
                Money.of(BigDecimal.valueOf(unitPrice)),
                Quantity.of(qty));
    }

    @Nested
    @DisplayName("subtotal()")
    class SubtotalCalculation {

        @Test
        @DisplayName("subtotal = unitPrice × quantity")
        void subtotal_isUnitPriceTimesQuantity() {
            var i = item(150_000, 3);
            assertThat(i.subtotal().amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(450_000));
        }

        @Test
        @DisplayName("quantity=1 → subtotal == unitPrice")
        void subtotal_singleUnit_equalsUnitPrice() {
            var i = item(99_000, 1);
            assertThat(i.subtotal().amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(99_000));
        }

        @Test
        @DisplayName("large quantity does not overflow")
        void subtotal_largeQuantity_noOverflow() {
            var i = item(1_000, Quantity.MAX);
            assertThat(i.subtotal().amount()).isPositive();
        }
    }

    @Nested
    @DisplayName("Quantity validation")
    class QuantityValidation {

        @Test
        @DisplayName("quantity=0 → IllegalArgumentException")
        void zeroQuantity_throws() {
            assertThatThrownBy(() -> item(100_000, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("quantity âm → IllegalArgumentException")
        void negativeQuantity_throws() {
            assertThatThrownBy(() -> item(100_000, -1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("quantity > MAX → IllegalArgumentException")
        void aboveMaxQuantity_throws() {
            assertThatThrownBy(() -> item(100_000, Quantity.MAX + 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("max");
        }
    }
}
