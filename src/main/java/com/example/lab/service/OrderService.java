package com.example.lab.service;

import com.example.lab.model.dto.CreateOrderRequest;
import com.example.lab.model.entity.Order;
import com.example.lab.model.entity.OrderItem;
import com.example.lab.model.enums.OrderStatus;
import com.example.lab.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("100");
    private static final BigDecimal STANDARD_SHIPPING = new BigDecimal("9.99");

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
        OrderStatus.DRAFT, Set.of(OrderStatus.PENDING, OrderStatus.CANCELLED),
        OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
        OrderStatus.CONFIRMED, Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
        OrderStatus.PROCESSING, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
        OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
        OrderStatus.DELIVERED, Set.of(OrderStatus.REFUNDED),
        OrderStatus.CANCELLED, Set.of(),
        OrderStatus.REFUNDED, Set.of()
    );

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // Validate items
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        for (var itemRequest : request.getItems()) {
            if (itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException("Item quantity must be positive");
            }
            if (itemRequest.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Item price cannot be negative");
            }
        }

        // Validate shipping address
        if (request.getShippingAddress() == null || !request.getShippingAddress().isValid()) {
            throw new IllegalArgumentException("Invalid shipping address");
        }

        // Create order
        Order order = Order.builder()
            .userId(request.getUserId())
            .shippingAddress(request.getShippingAddress())
            .notes(request.getNotes())
            .status(OrderStatus.DRAFT)
            .build();

        // Add items
        for (var itemRequest : request.getItems()) {
            OrderItem item = OrderItem.builder()
                .productId(itemRequest.getProductId())
                .quantity(itemRequest.getQuantity())
                .unitPrice(itemRequest.getUnitPrice())
                .discount(itemRequest.getDiscount())
                .build();
            order.addItem(item);
        }

        // Calculate totals
        calculateTotals(order);

        return orderRepository.save(order);
    }

    public Optional<Order> getOrder(String id) {
        return orderRepository.findById(id);
    }

    public List<Order> getUserOrders(String userId) {
        return orderRepository.findByUserId(userId);
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Transactional
    public Order updateOrderStatus(String orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        Set<OrderStatus> allowedTransitions = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());

        if (!allowedTransitions.contains(newStatus)) {
            throw new IllegalStateException(
                "Cannot transition from " + currentStatus + " to " + newStatus
            );
        }

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel order in " + order.getStatus() + " status");
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    @Transactional
    public Order addItem(String orderId, String productId, int quantity, BigDecimal unitPrice, int discount) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalStateException("Can only modify orders in draft status");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        OrderItem item = OrderItem.builder()
            .productId(productId)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .discount(discount)
            .build();

        order.addItem(item);
        calculateTotals(order);

        return orderRepository.save(order);
    }

    @Transactional
    public Order removeItem(String orderId, String itemId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalStateException("Can only modify orders in draft status");
        }

        order.getItems().removeIf(item -> item.getId().equals(itemId));
        calculateTotals(order);

        return orderRepository.save(order);
    }

    public void calculateTotals(Order order) {
        BigDecimal subtotal = order.getItems().stream()
            .map(OrderItem::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);

        BigDecimal shipping = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
            ? BigDecimal.ZERO
            : STANDARD_SHIPPING;

        BigDecimal total = subtotal.add(tax).add(shipping).setScale(2, RoundingMode.HALF_UP);

        order.setSubtotal(subtotal);
        order.setTax(tax);
        order.setShipping(shipping);
        order.setTotal(total);
    }

    public LocalDate getEstimatedDelivery(String shippingMethod) {
        int days = switch (shippingMethod.toLowerCase()) {
            case "overnight" -> 1;
            case "express" -> 2;
            default -> 5; // standard
        };

        LocalDate deliveryDate = LocalDate.now().plusDays(days);

        // Skip weekends
        while (deliveryDate.getDayOfWeek().getValue() > 5) {
            deliveryDate = deliveryDate.plusDays(1);
        }

        return deliveryDate;
    }
}
