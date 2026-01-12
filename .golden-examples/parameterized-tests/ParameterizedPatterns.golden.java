/**
 * GOLDEN EXAMPLE: Parameterized Testing with JUnit 5
 *
 * PATTERN: Data-Driven Testing
 *
 * WHEN TO USE:
 * - Multiple test cases with identical logic but different inputs/outputs
 * - Validation functions with many edge cases
 * - Enum-based behavior testing
 * - Reducing test code duplication
 *
 * ANTI-PATTERNS THIS SOLVES:
 * ❌ Copy-pasted tests with only data differences
 * ❌ Long test files with repetitive structure
 * ❌ Missing edge cases due to test fatigue
 * ❌ Inconsistent test naming
 *
 * KEY PRINCIPLES:
 * 1. Group related test cases by behavior
 * 2. Use descriptive display names with placeholders
 * 3. Include boundary values in test data
 * 4. Document WHY each test case exists
 */

package com.example.lab.golden;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Demonstrates all parameterized test patterns for the Test Optimization Lab.
 */
@DisplayName("Parameterized Testing Golden Examples")
class ParameterizedPatternsGoldenTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    // ============================================================
    // PATTERN 1: @ValueSource - Single Parameter Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 1: @ValueSource")
    class ValueSourcePatterns {

        @ParameterizedTest(name = "should accept valid email: {0}")
        @ValueSource(strings = {
            "user@example.com",
            "user.name@example.com",
            "user+tag@example.com",
            "user@subdomain.example.com",
            "a@b.co"
        })
        void shouldAcceptValidEmails(String email) {
            assertThat(validationService.isValidEmail(email)).isTrue();
        }

        @ParameterizedTest(name = "should reject invalid email: {0}")
        @ValueSource(strings = {
            "",
            "invalid",
            "@example.com",
            "user@",
            "user@.com",
            "user space@example.com"
        })
        void shouldRejectInvalidEmails(String email) {
            assertThat(validationService.isValidEmail(email)).isFalse();
        }

        @ParameterizedTest(name = "should accept valid age: {0}")
        @ValueSource(ints = {18, 19, 30, 65, 100, 120})
        void shouldAcceptValidAges(int age) {
            assertThat(validationService.isValidAge(age)).isTrue();
        }

        @ParameterizedTest(name = "should reject invalid age: {0}")
        @ValueSource(ints = {-1, 0, 17, 121, 200})
        void shouldRejectInvalidAges(int age) {
            assertThat(validationService.isValidAge(age)).isFalse();
        }
    }

    // ============================================================
    // PATTERN 2: @CsvSource - Multiple Parameters
    // ============================================================
    @Nested
    @DisplayName("Pattern 2: @CsvSource")
    class CsvSourcePatterns {

        @ParameterizedTest(name = "password \"{0}\" should be {1} ({2})")
        @CsvSource({
            // password, expected, reason
            "short, false, too short",
            "alllowercase1!, false, no uppercase",
            "ALLUPPERCASE1!, false, no lowercase",
            "NoNumbers!!, false, no numbers",
            "NoSpecial123, false, no special char",
            "ValidPass1!, true, meets all requirements",
            "C0mpl3x!Pass, true, complex password",
            "A1!bcdef, true, minimum requirements"
        })
        void shouldValidatePassword(String password, boolean expected, String reason) {
            ValidationResult result = validationService.validatePassword(password);
            assertThat(result.isValid())
                .as("Password '%s' should be %s because %s", password, expected ? "valid" : "invalid", reason)
                .isEqualTo(expected);
        }

        @ParameterizedTest(name = "amount ${0} in {1} should be {2}")
        @CsvSource({
            "100.00, USD, true",
            "0.01, USD, true",
            "999999.99, USD, true",
            "0.00, USD, false",
            "-1.00, USD, false",
            "1000000.00, USD, false"
        })
        void shouldValidatePaymentAmount(String amount, String currency, boolean expected) {
            BigDecimal amountValue = new BigDecimal(amount);
            assertThat(validationService.isValidAmount(amountValue, currency)).isEqualTo(expected);
        }
    }

    // ============================================================
    // PATTERN 3: @CsvFileSource - External Test Data
    // ============================================================
    @Nested
    @DisplayName("Pattern 3: @CsvFileSource")
    class CsvFileSourcePatterns {

        // @ParameterizedTest(name = "SKU {0} should be {1}")
        // @CsvFileSource(resources = "/test-data/sku-validation.csv", numLinesToSkip = 1)
        // void shouldValidateSku(String sku, boolean expected, String reason) {
        //     assertThat(validationService.isValidSku(sku))
        //         .as(reason)
        //         .isEqualTo(expected);
        // }

        // CSV file format (sku-validation.csv):
        // sku,expected,reason
        // SKU-001,true,valid format
        // SKU001,false,missing hyphen
        // ,false,empty string
    }

    // ============================================================
    // PATTERN 4: @MethodSource - Complex Objects
    // ============================================================
    @Nested
    @DisplayName("Pattern 4: @MethodSource")
    class MethodSourcePatterns {

        @ParameterizedTest(name = "order with {0} items, subtotal ${1}")
        @MethodSource("provideValidOrders")
        void shouldCalculateOrderTotal(int itemCount, BigDecimal expectedSubtotal, Order order) {
            OrderCalculation result = validationService.calculateOrder(order);

            assertThat(result.getItemCount()).isEqualTo(itemCount);
            assertThat(result.getSubtotal()).isEqualByComparingTo(expectedSubtotal);
        }

        static Stream<Arguments> provideValidOrders() {
            return Stream.of(
                Arguments.of(1, new BigDecimal("10.00"), createOrderWithItems(1, "10.00")),
                Arguments.of(3, new BigDecimal("30.00"), createOrderWithItems(3, "10.00")),
                Arguments.of(5, new BigDecimal("100.00"), createOrderWithItems(5, "20.00"))
            );
        }

        @ParameterizedTest(name = "should apply {0}% discount correctly")
        @MethodSource("provideDiscountScenarios")
        void shouldApplyDiscount(int discountPercent, BigDecimal originalPrice, BigDecimal expectedPrice) {
            BigDecimal result = validationService.applyDiscount(originalPrice, discountPercent);
            assertThat(result).isEqualByComparingTo(expectedPrice);
        }

        static Stream<Arguments> provideDiscountScenarios() {
            return Stream.of(
                Arguments.of(0, new BigDecimal("100.00"), new BigDecimal("100.00")),
                Arguments.of(10, new BigDecimal("100.00"), new BigDecimal("90.00")),
                Arguments.of(50, new BigDecimal("100.00"), new BigDecimal("50.00")),
                Arguments.of(100, new BigDecimal("100.00"), new BigDecimal("0.00"))
            );
        }

        private static Order createOrderWithItems(int count, String priceEach) {
            // Factory method for test data
            return new Order(count, new BigDecimal(priceEach));
        }
    }

    // ============================================================
    // PATTERN 5: @EnumSource - Enum Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 5: @EnumSource")
    class EnumSourcePatterns {

        @ParameterizedTest(name = "payment method {0} should be supported")
        @EnumSource(PaymentMethod.class)
        void shouldSupportAllPaymentMethods(PaymentMethod method) {
            assertThat(validationService.isPaymentMethodSupported(method)).isTrue();
        }

        @ParameterizedTest(name = "premium payment method {0} should have lower fee")
        @EnumSource(value = PaymentMethod.class, names = {"WALLET", "BANK_TRANSFER"})
        void shouldHaveLowerFeesForPremiumMethods(PaymentMethod method) {
            BigDecimal fee = validationService.getPaymentFee(method);
            assertThat(fee).isLessThanOrEqualTo(new BigDecimal("1.0"));
        }

        @ParameterizedTest(name = "order status {0} should allow cancellation: {1}")
        @CsvSource({
            "PENDING, true",
            "CONFIRMED, true",
            "PROCESSING, false",
            "SHIPPED, false",
            "DELIVERED, false",
            "CANCELLED, false"
        })
        void shouldDetermineIfOrderCanBeCancelled(OrderStatus status, boolean canCancel) {
            assertThat(validationService.canCancelOrder(status)).isEqualTo(canCancel);
        }
    }

    // ============================================================
    // PATTERN 6: @NullAndEmptySource - Null Handling
    // ============================================================
    @Nested
    @DisplayName("Pattern 6: @NullAndEmptySource")
    class NullAndEmptySourcePatterns {

        @ParameterizedTest(name = "should reject null/empty email: [{0}]")
        @NullAndEmptySource
        void shouldRejectNullAndEmptyEmail(String email) {
            assertThat(validationService.isValidEmail(email)).isFalse();
        }

        @ParameterizedTest(name = "should reject null/empty/blank email: [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        void shouldRejectNullEmptyAndBlankEmail(String email) {
            assertThat(validationService.isValidEmail(email)).isFalse();
        }
    }

    // ============================================================
    // PATTERN 7: Boundary Value Testing with Parameterization
    // ============================================================
    @Nested
    @DisplayName("Pattern 7: Boundary Values")
    class BoundaryValuePatterns {

        private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
        private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999.99");

        @ParameterizedTest(name = "amount ${0} should be valid")
        @MethodSource("provideValidBoundaryAmounts")
        void shouldAcceptValidBoundaryAmounts(BigDecimal amount, String description) {
            assertThat(validationService.isValidAmount(amount, "USD"))
                .as(description)
                .isTrue();
        }

        static Stream<Arguments> provideValidBoundaryAmounts() {
            return Stream.of(
                Arguments.of(MIN_AMOUNT, "minimum valid amount"),
                Arguments.of(MIN_AMOUNT.add(new BigDecimal("0.01")), "just above minimum"),
                Arguments.of(new BigDecimal("100.00"), "typical amount"),
                Arguments.of(MAX_AMOUNT.subtract(new BigDecimal("0.01")), "just below maximum"),
                Arguments.of(MAX_AMOUNT, "maximum valid amount")
            );
        }

        @ParameterizedTest(name = "amount ${0} should be invalid")
        @MethodSource("provideInvalidBoundaryAmounts")
        void shouldRejectInvalidBoundaryAmounts(BigDecimal amount, String description) {
            assertThat(validationService.isValidAmount(amount, "USD"))
                .as(description)
                .isFalse();
        }

        static Stream<Arguments> provideInvalidBoundaryAmounts() {
            return Stream.of(
                Arguments.of(BigDecimal.ZERO, "zero amount"),
                Arguments.of(new BigDecimal("-0.01"), "negative amount"),
                Arguments.of(MIN_AMOUNT.subtract(new BigDecimal("0.01")), "below minimum"),
                Arguments.of(MAX_AMOUNT.add(new BigDecimal("0.01")), "above maximum")
            );
        }
    }

    // ============================================================
    // Helper Classes for Examples
    // ============================================================

    static class ValidationService {
        boolean isValidEmail(String email) { return email != null && email.contains("@") && !email.contains(" "); }
        boolean isValidAge(int age) { return age >= 18 && age <= 120; }
        ValidationResult validatePassword(String password) { return new ValidationResult(password != null && password.length() >= 8); }
        boolean isValidAmount(BigDecimal amount, String currency) { return amount != null && amount.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(new BigDecimal("999999.99")) <= 0; }
        boolean isValidSku(String sku) { return sku != null && sku.contains("-"); }
        OrderCalculation calculateOrder(Order order) { return new OrderCalculation(order.itemCount, order.priceEach.multiply(BigDecimal.valueOf(order.itemCount))); }
        BigDecimal applyDiscount(BigDecimal price, int percent) { return price.multiply(BigDecimal.valueOf(100 - percent)).divide(BigDecimal.valueOf(100)); }
        boolean isPaymentMethodSupported(PaymentMethod method) { return true; }
        BigDecimal getPaymentFee(PaymentMethod method) { return method == PaymentMethod.WALLET ? BigDecimal.ZERO : new BigDecimal("2.5"); }
        boolean canCancelOrder(OrderStatus status) { return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED; }
    }

    static class ValidationResult {
        private final boolean valid;
        ValidationResult(boolean valid) { this.valid = valid; }
        boolean isValid() { return valid; }
    }

    static class Order {
        final int itemCount;
        final BigDecimal priceEach;
        Order(int itemCount, BigDecimal priceEach) { this.itemCount = itemCount; this.priceEach = priceEach; }
    }

    static class OrderCalculation {
        private final int itemCount;
        private final BigDecimal subtotal;
        OrderCalculation(int itemCount, BigDecimal subtotal) { this.itemCount = itemCount; this.subtotal = subtotal; }
        int getItemCount() { return itemCount; }
        BigDecimal getSubtotal() { return subtotal; }
    }

    enum PaymentMethod { CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, WALLET, CRYPTO }
    enum OrderStatus { PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED }
}

/**
 * SUMMARY OF PARAMETERIZED TEST PATTERNS:
 *
 * 1. @ValueSource - Single parameter, simple types
 * 2. @CsvSource - Multiple parameters inline
 * 3. @CsvFileSource - External CSV test data
 * 4. @MethodSource - Complex objects, reusable data
 * 5. @EnumSource - Enum value testing
 * 6. @NullAndEmptySource - Null/empty handling
 * 7. Combine patterns for boundary value testing
 *
 * NAMING CONVENTIONS:
 * - Use (name = "...") for descriptive test names
 * - Include {0}, {1}, etc. for parameter substitution
 * - Add context (why, not just what)
 */
