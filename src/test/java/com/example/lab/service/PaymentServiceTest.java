package com.example.lab.service;

import com.example.lab.model.dto.PaymentRequest;
import com.example.lab.model.entity.Payment;
import com.example.lab.model.enums.Currency;
import com.example.lab.model.enums.PaymentMethod;
import com.example.lab.model.enums.PaymentStatus;
import com.example.lab.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    // WEAK COVERAGE: Only tests credit card happy path
    // Missing: debit_card, bank_transfer, wallet, crypto payment methods
    // Missing: validation errors, edge cases, refunds, cancellations

    @BeforeEach
    void setUp() {
        // Could extract common setup here
    }

    @Test
    void shouldProcessCreditCardPaymentSuccessfully() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .orderId("order-123")
            .userId("user-456")
            .amount(new BigDecimal("99.99"))
            .currency(Currency.USD)
            .method(PaymentMethod.CREDIT_CARD)
            .cardNumber("4111111111111111")
            .cardExpiry("12/30")
            .cardCvv("123")
            .cardHolderName("John Doe")
            .build();

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId("pay-789");
            return payment;
        });

        // Act
        Payment result = paymentService.processPayment(request);

        // Assert
        assertNotNull(result);
        assertEquals("pay-789", result.getId());
        // Note: Status could be COMPLETED or FAILED due to random simulation
        assertNotNull(result.getStatus());
    }

    // MISSING TESTS:
    // - processPayment with DEBIT_CARD
    // - processPayment with BANK_TRANSFER
    // - processPayment with WALLET
    // - processPayment with CRYPTO
    // - processPayment with invalid amount (negative, zero, over max)
    // - processPayment with unsupported currency
    // - processPayment with expired card
    // - processPayment with invalid card number
    // - processPayment with missing CVV

    @Test
    void shouldValidateCardNumber() {
        // Only tests one valid card
        assertTrue(paymentService.isValidLuhn("4111111111111111"));
    }

    // MISSING TESTS:
    // - Invalid card numbers (fails Luhn)
    // - Card number too short
    // - Card number too long
    // - Card number with letters
    // - Null card number

    @Test
    void shouldValidateCardExpiry() {
        assertTrue(paymentService.isValidExpiry("12/30"));
    }

    // MISSING TESTS:
    // - Expired card
    // - Invalid format
    // - Invalid month (13, 00)
    // - Null expiry

    @Test
    void shouldCalculateCreditCardFee() {
        BigDecimal fee = paymentService.calculateProcessingFee(
            new BigDecimal("100"),
            PaymentMethod.CREDIT_CARD
        );
        assertEquals(new BigDecimal("2.90"), fee);
    }

    // MISSING TESTS:
    // - Fee for DEBIT_CARD
    // - Fee for BANK_TRANSFER
    // - Fee for WALLET
    // - Fee for CRYPTO
    // - Edge case: $0 amount
    // - Edge case: very large amount

    // MISSING TEST SECTIONS:
    // - getPayment
    // - getUserPayments
    // - getOrderPayments
    // - refundPayment
    // - cancelPayment
    // - Error handling
}
