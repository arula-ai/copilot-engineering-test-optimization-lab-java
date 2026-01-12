package com.example.lab.service;

import com.example.lab.model.dto.PaymentRequest;
import com.example.lab.model.entity.Payment;
import com.example.lab.model.enums.Currency;
import com.example.lab.model.enums.PaymentMethod;
import com.example.lab.model.enums.PaymentStatus;
import com.example.lab.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");
    private static final Set<Currency> SUPPORTED_CURRENCIES = Set.of(
        Currency.USD, Currency.EUR, Currency.GBP, Currency.CAD, Currency.AUD, Currency.JPY
    );

    @Transactional
    public Payment processPayment(PaymentRequest request) {
        // Validate amount
        if (request.getAmount().compareTo(MIN_AMOUNT) < 0) {
            throw new IllegalArgumentException("Amount must be at least " + MIN_AMOUNT);
        }
        if (request.getAmount().compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount cannot exceed " + MAX_AMOUNT);
        }

        // Validate currency
        if (!SUPPORTED_CURRENCIES.contains(request.getCurrency())) {
            throw new IllegalArgumentException("Unsupported currency: " + request.getCurrency());
        }

        // Validate card details for card payments
        if (isCardPayment(request.getMethod())) {
            validateCardDetails(request);
        }

        // Create payment
        Payment payment = Payment.builder()
            .orderId(request.getOrderId())
            .userId(request.getUserId())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .method(request.getMethod())
            .status(PaymentStatus.PROCESSING)
            .transactionId(generateTransactionId())
            .build();

        if (isCardPayment(request.getMethod()) && request.getCardNumber() != null) {
            payment.setCardLastFour(request.getCardNumber().substring(
                request.getCardNumber().length() - 4
            ));
        }

        // Simulate payment processing
        boolean success = simulatePaymentProcessing(request);

        if (success) {
            payment.setStatus(PaymentStatus.COMPLETED);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage("Payment processing failed");
        }

        return paymentRepository.save(payment);
    }

    public Optional<Payment> getPayment(String id) {
        return paymentRepository.findById(id);
    }

    public List<Payment> getUserPayments(String userId) {
        return paymentRepository.findByUserId(userId);
    }

    public List<Payment> getOrderPayments(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @Transactional
    public Payment refundPayment(String paymentId, BigDecimal refundAmount, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Can only refund completed payments");
        }

        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed payment amount");
        }

        if (reason == null || reason.trim().length() < 10) {
            throw new IllegalArgumentException("Refund reason must be at least 10 characters");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment cancelPayment(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Can only cancel pending payments");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        return paymentRepository.save(payment);
    }

    public boolean validateCardDetails(PaymentRequest request) {
        // Validate card number using Luhn algorithm
        if (!isValidLuhn(request.getCardNumber())) {
            throw new IllegalArgumentException("Invalid card number");
        }

        // Validate expiry
        if (!isValidExpiry(request.getCardExpiry())) {
            throw new IllegalArgumentException("Card has expired");
        }

        // Validate CVV
        if (!isValidCVV(request.getCardCvv())) {
            throw new IllegalArgumentException("Invalid CVV");
        }

        // Validate holder name
        if (request.getCardHolderName() == null || request.getCardHolderName().trim().length() < 2) {
            throw new IllegalArgumentException("Invalid cardholder name");
        }

        return true;
    }

    public boolean isValidLuhn(String cardNumber) {
        if (cardNumber == null) return false;

        String sanitized = cardNumber.replaceAll("\\D", "");
        if (sanitized.length() < 13 || sanitized.length() > 19) {
            return false;
        }

        int sum = 0;
        boolean isEven = false;

        for (int i = sanitized.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(sanitized.charAt(i));

            if (isEven) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            isEven = !isEven;
        }

        return sum % 10 == 0;
    }

    public boolean isValidExpiry(String expiry) {
        if (expiry == null) return false;

        Pattern pattern = Pattern.compile("^(\\d{2})/(\\d{2})$");
        var matcher = pattern.matcher(expiry);

        if (!matcher.matches()) return false;

        int month = Integer.parseInt(matcher.group(1));
        int year = Integer.parseInt(matcher.group(2)) + 2000;

        if (month < 1 || month > 12) return false;

        YearMonth expiryDate = YearMonth.of(year, month);
        return expiryDate.isAfter(YearMonth.now());
    }

    public boolean isValidCVV(String cvv) {
        return cvv != null && cvv.matches("^\\d{3,4}$");
    }

    public BigDecimal calculateProcessingFee(BigDecimal amount, PaymentMethod method) {
        BigDecimal rate = switch (method) {
            case CREDIT_CARD -> new BigDecimal("0.029");
            case DEBIT_CARD -> new BigDecimal("0.015");
            case BANK_TRANSFER -> new BigDecimal("0.005");
            case WALLET -> new BigDecimal("0.02");
            case CRYPTO -> new BigDecimal("0.01");
        };

        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isCardPayment(PaymentMethod method) {
        return method == PaymentMethod.CREDIT_CARD || method == PaymentMethod.DEBIT_CARD;
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean simulatePaymentProcessing(PaymentRequest request) {
        // Simulate 95% success rate
        return Math.random() > 0.05;
    }
}
