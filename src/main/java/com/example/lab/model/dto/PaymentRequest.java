package com.example.lab.model.dto;

import com.example.lab.model.enums.Currency;
import com.example.lab.model.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @DecimalMax(value = "1000000", message = "Amount cannot exceed 1,000,000")
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    private Currency currency;

    @NotNull(message = "Payment method is required")
    private PaymentMethod method;

    // Card details (optional, required for card payments)
    private String cardNumber;
    private String cardExpiry;
    private String cardCvv;
    private String cardHolderName;
}
