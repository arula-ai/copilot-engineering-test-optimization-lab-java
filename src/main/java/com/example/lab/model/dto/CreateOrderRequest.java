package com.example.lab.model.dto;

import com.example.lab.model.entity.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<OrderItemRequest> items;

    @NotNull(message = "Shipping address is required")
    @Valid
    private Address shippingAddress;

    private String notes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemRequest {

        @NotBlank(message = "Product ID is required")
        private String productId;

        @Min(value = 1, message = "Quantity must be at least 1")
        private int quantity;

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0", message = "Unit price cannot be negative")
        private BigDecimal unitPrice;

        @Min(value = 0, message = "Discount cannot be negative")
        @Max(value = 100, message = "Discount cannot exceed 100%")
        private int discount;
    }
}
