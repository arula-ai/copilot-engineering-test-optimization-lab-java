package com.example.lab.service;

import com.example.lab.model.dto.CreateOrderRequest;
import com.example.lab.model.entity.Address;
import com.example.lab.model.entity.Order;
import com.example.lab.model.entity.OrderItem;
import com.example.lab.model.enums.OrderStatus;
import com.example.lab.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest validOrderRequest;

    @BeforeEach
    void setUp() {
        validOrderRequest = CreateOrderRequest.builder()
            .userId("user-123")
            .items(List.of(
                CreateOrderRequest.OrderItemRequest.builder()
                    .productId("prod-1")
                    .quantity(2)
                    .unitPrice(new BigDecimal("25.00"))
                    .discount(0)
                    .build(),
                CreateOrderRequest.OrderItemRequest.builder()
                    .productId("prod-2")
                    .quantity(1)
                    .unitPrice(new BigDecimal("50.00"))
                    .discount(10)
                    .build()
            ))
            .shippingAddress(Address.builder()
                .street("123 Main St")
                .city("Boston")
                .state("MA")
                .postalCode("02101")
                .country("US")
                .build())
            .build();
    }

    @Test
    void shouldCreateOrderSuccessfully() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("order-456");
            return order;
        });

        Order result = orderService.createOrder(validOrderRequest);

        assertNotNull(result);
        assertEquals("order-456", result.getId());
        assertEquals(OrderStatus.DRAFT, result.getStatus());
    }

    @Test
    void shouldCalculateTotalsCorrectly() {
        Order order = Order.builder().build();
        order.setItems(List.of(
            OrderItem.builder()
                .productId("p1")
                .quantity(2)
                .unitPrice(new BigDecimal("25.00"))
                .discount(0)
                .build(),
            OrderItem.builder()
                .productId("p2")
                .quantity(1)
                .unitPrice(new BigDecimal("50.00"))
                .discount(10)
                .build()
        ));

        orderService.calculateTotals(order);

        // Subtotal: (25*2) + (50*0.9) = 50 + 45 = 95
        assertEquals(0, new BigDecimal("95.00").compareTo(order.getSubtotal()));
        // Tax: 95 * 0.08 = 7.60
        assertEquals(0, new BigDecimal("7.60").compareTo(order.getTax()));
        // Shipping: $9.99 (subtotal < 100)
        assertEquals(0, new BigDecimal("9.99").compareTo(order.getShipping()));
        // Total: 95 + 7.60 + 9.99 = 112.59
        assertEquals(0, new BigDecimal("112.59").compareTo(order.getTotal()));
    }

    @Test
    void shouldGiveFreeShippingForOrdersOver100() {
        Order order = Order.builder().build();
        order.setItems(List.of(
            OrderItem.builder()
                .productId("p1")
                .quantity(5)
                .unitPrice(new BigDecimal("25.00"))
                .discount(0)
                .build()
        ));

        orderService.calculateTotals(order);

        assertEquals(BigDecimal.ZERO, order.getShipping());
    }

    // FLAKY TEST: Depends on timing
    @Test
    void shouldUpdateOrderStatus() {
        Order existingOrder = Order.builder()
            .id("order-789")
            .userId("user-123")
            .status(OrderStatus.DRAFT)
            .build();

        when(orderRepository.findById("order-789")).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.updateOrderStatus("order-789", OrderStatus.PENDING);

        assertEquals(OrderStatus.PENDING, result.getStatus());
    }

    @Test
    void shouldRejectInvalidRepeatedTransition() {
        Order existingOrder = Order.builder()
            .id("order-race")
            .userId("user-123")
            .status(OrderStatus.DRAFT)
            .build();

        when(orderRepository.findById("order-race")).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // First valid transition
        Order updated = orderService.updateOrderStatus("order-race", OrderStatus.PENDING);
        assertEquals(OrderStatus.PENDING, updated.getStatus());

        // Second transition to the same status is invalid and should throw
        assertThrows(IllegalStateException.class, () ->
            orderService.updateOrderStatus("order-race", OrderStatus.PENDING)
        );
    }

    @Test
    void shouldCancelDraftOrder() {
        Order existingOrder = Order.builder()
            .id("order-cancel")
            .userId("user-123")
            .status(OrderStatus.DRAFT)
            .build();

        when(orderRepository.findById("order-cancel")).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.cancelOrder("order-cancel");

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
    }

    @Test
    void shouldThrowWhenCancellingShippedOrder() {
        Order shippedOrder = Order.builder()
            .id("order-shipped")
            .userId("user-123")
            .status(OrderStatus.SHIPPED)
            .build();

        when(orderRepository.findById("order-shipped")).thenReturn(Optional.of(shippedOrder));

        assertThrows(IllegalStateException.class, () ->
            orderService.cancelOrder("order-shipped")
        );
    }

    @Test
    void shouldEstimateDeliveryForStandardShipping() {
        LocalDate estimate = orderService.getEstimatedDelivery("standard");

        LocalDate now = LocalDate.now();
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(now, estimate);

        // Standard shipping should be approximately 5 days, allow up to 7 days accounting for weekend skips
        assertTrue(daysBetween >= 5 && daysBetween <= 7, "Expected 5-7 days, got " + daysBetween);

        // Delivery date must be a weekday
        assertTrue(estimate.getDayOfWeek().getValue() <= 5, "Estimated delivery falls on a weekend");
    }

    // MISSING TESTS:
    // - createOrder with empty items
    // - createOrder with negative quantity
    // - createOrder with negative price
    // - createOrder with invalid address
    // - getOrder
    // - getUserOrders
    // - getOrdersByStatus
    // - All status transitions
    // - addItem
    // - removeItem
    // - Express/overnight shipping estimates
}
