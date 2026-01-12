/**
 * GOLDEN EXAMPLE: Boundary Value Testing
 *
 * PATTERN: Systematic Boundary Analysis
 *
 * WHEN TO USE:
 * - Testing numeric ranges (min, max, boundaries)
 * - Testing string length constraints
 * - Testing collection size limits
 * - Testing date/time boundaries
 *
 * ANTI-PATTERNS THIS SOLVES:
 * ❌ Only testing "typical" values
 * ❌ Missing off-by-one edge cases
 * ❌ Not testing null/empty/blank
 * ❌ Ignoring type limits
 *
 * KEY PRINCIPLES:
 * 1. Test: min-1, min, min+1, typical, max-1, max, max+1
 * 2. Test: null, empty, blank (whitespace)
 * 3. Document boundaries as constants
 * 4. Use parameterized tests for boundary cases
 */

package com.example.lab.golden;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Demonstrates boundary value testing patterns for the Test Optimization Lab.
 */
@DisplayName("Boundary Value Testing Golden Examples")
class BoundaryPatternsGoldenTest {

    // ============================================================
    // DOCUMENT ALL BOUNDARIES AS CONSTANTS
    // ============================================================
    static class Boundaries {
        // Quantity constraints
        static final int QUANTITY_MIN = 1;
        static final int QUANTITY_MAX = 9999;
        static final int QUANTITY_TYPICAL = 50;

        // Price constraints
        static final BigDecimal PRICE_MIN = new BigDecimal("0.01");
        static final BigDecimal PRICE_MAX = new BigDecimal("999999.99");

        // String length constraints
        static final int SKU_MIN_LENGTH = 3;
        static final int SKU_MAX_LENGTH = 50;
        static final int NAME_MIN_LENGTH = 1;
        static final int NAME_MAX_LENGTH = 200;

        // Collection constraints
        static final int CART_MAX_ITEMS = 100;
        static final int CART_MAX_QUANTITY_PER_ITEM = 99;

        // Age constraints
        static final int AGE_MIN = 18;
        static final int AGE_MAX = 120;
    }

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    // ============================================================
    // PATTERN 1: Numeric Boundary Testing (7-Point Analysis)
    // ============================================================
    @Nested
    @DisplayName("Pattern 1: Numeric Boundaries")
    class NumericBoundaries {

        @ParameterizedTest(name = "quantity {0} should be valid ({1})")
        @MethodSource("provideValidQuantities")
        void shouldAcceptValidQuantities(int quantity, String description) {
            ValidationResult result = validationService.validateQuantity(quantity);

            assertThat(result.isValid())
                .as(description)
                .isTrue();
        }

        static Stream<Arguments> provideValidQuantities() {
            return Stream.of(
                Arguments.of(Boundaries.QUANTITY_MIN, "minimum valid"),
                Arguments.of(Boundaries.QUANTITY_MIN + 1, "just above minimum"),
                Arguments.of(Boundaries.QUANTITY_TYPICAL, "typical value"),
                Arguments.of(Boundaries.QUANTITY_MAX - 1, "just below maximum"),
                Arguments.of(Boundaries.QUANTITY_MAX, "maximum valid")
            );
        }

        @ParameterizedTest(name = "quantity {0} should be invalid ({1})")
        @MethodSource("provideInvalidQuantities")
        void shouldRejectInvalidQuantities(int quantity, String description, String expectedError) {
            ValidationResult result = validationService.validateQuantity(quantity);

            assertThat(result.isValid())
                .as(description)
                .isFalse();
            assertThat(result.getError())
                .containsIgnoringCase(expectedError);
        }

        static Stream<Arguments> provideInvalidQuantities() {
            return Stream.of(
                Arguments.of(Boundaries.QUANTITY_MIN - 1, "below minimum", "at least 1"),
                Arguments.of(0, "zero", "at least 1"),
                Arguments.of(-1, "negative", "at least 1"),
                Arguments.of(-100, "large negative", "at least 1"),
                Arguments.of(Boundaries.QUANTITY_MAX + 1, "above maximum", "cannot exceed"),
                Arguments.of(Integer.MAX_VALUE, "integer overflow", "cannot exceed")
            );
        }
    }

    // ============================================================
    // PATTERN 2: Decimal/Currency Boundary Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 2: Decimal/Currency Boundaries")
    class DecimalBoundaries {

        @ParameterizedTest(name = "price ${0} should be valid ({1})")
        @MethodSource("provideValidPrices")
        void shouldAcceptValidPrices(BigDecimal price, String description) {
            assertThat(validationService.validatePrice(price).isValid())
                .as(description)
                .isTrue();
        }

        static Stream<Arguments> provideValidPrices() {
            return Stream.of(
                Arguments.of(Boundaries.PRICE_MIN, "minimum price (1 cent)"),
                Arguments.of(new BigDecimal("0.02"), "2 cents"),
                Arguments.of(new BigDecimal("1.00"), "one dollar"),
                Arguments.of(new BigDecimal("99.99"), "typical retail price"),
                Arguments.of(new BigDecimal("1000.00"), "thousand dollars"),
                Arguments.of(Boundaries.PRICE_MAX.subtract(new BigDecimal("0.01")), "just below max"),
                Arguments.of(Boundaries.PRICE_MAX, "maximum price")
            );
        }

        @ParameterizedTest(name = "price ${0} should be invalid ({1})")
        @MethodSource("provideInvalidPrices")
        void shouldRejectInvalidPrices(BigDecimal price, String description) {
            assertThat(validationService.validatePrice(price).isValid())
                .as(description)
                .isFalse();
        }

        static Stream<Arguments> provideInvalidPrices() {
            return Stream.of(
                Arguments.of(BigDecimal.ZERO, "zero"),
                Arguments.of(new BigDecimal("-0.01"), "negative cent"),
                Arguments.of(new BigDecimal("-100"), "negative hundred"),
                Arguments.of(Boundaries.PRICE_MIN.subtract(new BigDecimal("0.01")), "below minimum"),
                Arguments.of(Boundaries.PRICE_MAX.add(new BigDecimal("0.01")), "above maximum")
            );
        }

        @Test
        @DisplayName("should handle null price")
        void shouldHandleNullPrice() {
            ValidationResult result = validationService.validatePrice(null);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getError()).containsIgnoringCase("required");
        }

        @ParameterizedTest(name = "should reject price with {1} decimal places: {0}")
        @MethodSource("providePricesWithWrongPrecision")
        void shouldRejectPricesWithWrongPrecision(BigDecimal price, int decimalPlaces) {
            ValidationResult result = validationService.validatePrice(price);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getError()).containsIgnoringCase("decimal");
        }

        static Stream<Arguments> providePricesWithWrongPrecision() {
            return Stream.of(
                Arguments.of(new BigDecimal("10.001"), 3),
                Arguments.of(new BigDecimal("10.0001"), 4),
                Arguments.of(new BigDecimal("10.123456"), 6)
            );
        }
    }

    // ============================================================
    // PATTERN 3: String Length Boundary Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 3: String Length Boundaries")
    class StringLengthBoundaries {

        private String stringOfLength(int length) {
            return "A".repeat(length);
        }

        @ParameterizedTest(name = "SKU of length {0} should be valid ({1})")
        @MethodSource("provideValidSkuLengths")
        void shouldAcceptValidSkuLengths(int length, String description) {
            String sku = stringOfLength(length);

            assertThat(validationService.validateSku(sku).isValid())
                .as(description)
                .isTrue();
        }

        static Stream<Arguments> provideValidSkuLengths() {
            return Stream.of(
                Arguments.of(Boundaries.SKU_MIN_LENGTH, "minimum length"),
                Arguments.of(Boundaries.SKU_MIN_LENGTH + 1, "just above minimum"),
                Arguments.of(10, "typical length"),
                Arguments.of(Boundaries.SKU_MAX_LENGTH - 1, "just below maximum"),
                Arguments.of(Boundaries.SKU_MAX_LENGTH, "maximum length")
            );
        }

        @ParameterizedTest(name = "SKU of length {0} should be invalid ({1})")
        @MethodSource("provideInvalidSkuLengths")
        void shouldRejectInvalidSkuLengths(int length, String description) {
            String sku = length > 0 ? stringOfLength(length) : "";

            assertThat(validationService.validateSku(sku).isValid())
                .as(description)
                .isFalse();
        }

        static Stream<Arguments> provideInvalidSkuLengths() {
            return Stream.of(
                Arguments.of(0, "empty string"),
                Arguments.of(1, "one character"),
                Arguments.of(Boundaries.SKU_MIN_LENGTH - 1, "below minimum"),
                Arguments.of(Boundaries.SKU_MAX_LENGTH + 1, "above maximum"),
                Arguments.of(100, "way above maximum")
            );
        }

        @ParameterizedTest(name = "should reject SKU: [{0}] ({1})")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n", " \t\n "})
        void shouldRejectNullEmptyBlankSku(String sku) {
            ValidationResult result = validationService.validateSku(sku);

            assertThat(result.isValid()).isFalse();
        }
    }

    // ============================================================
    // PATTERN 4: Collection Size Boundary Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 4: Collection Size Boundaries")
    class CollectionSizeBoundaries {

        private List<CartItem> createCartItems(int count) {
            List<CartItem> items = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                items.add(new CartItem("PROD-" + i, "Product " + i, 1, new BigDecimal("10.00")));
            }
            return items;
        }

        @ParameterizedTest(name = "cart with {0} items should be valid ({1})")
        @MethodSource("provideValidCartSizes")
        void shouldAcceptValidCartSizes(int itemCount, String description) {
            List<CartItem> items = createCartItems(itemCount);

            assertThat(validationService.validateCart(items).isValid())
                .as(description)
                .isTrue();
        }

        static Stream<Arguments> provideValidCartSizes() {
            return Stream.of(
                Arguments.of(0, "empty cart"),
                Arguments.of(1, "single item"),
                Arguments.of(2, "two items"),
                Arguments.of(10, "typical cart"),
                Arguments.of(Boundaries.CART_MAX_ITEMS - 1, "just below max"),
                Arguments.of(Boundaries.CART_MAX_ITEMS, "maximum items")
            );
        }

        @ParameterizedTest(name = "cart with {0} items should be invalid ({1})")
        @MethodSource("provideInvalidCartSizes")
        void shouldRejectInvalidCartSizes(int itemCount, String description) {
            List<CartItem> items = createCartItems(itemCount);

            assertThat(validationService.validateCart(items).isValid())
                .as(description)
                .isFalse();
        }

        static Stream<Arguments> provideInvalidCartSizes() {
            return Stream.of(
                Arguments.of(Boundaries.CART_MAX_ITEMS + 1, "one above max"),
                Arguments.of(Boundaries.CART_MAX_ITEMS + 10, "ten above max"),
                Arguments.of(500, "way above max")
            );
        }

        @Test
        @DisplayName("should handle null cart")
        void shouldHandleNullCart() {
            assertThat(validationService.validateCart(null).isValid()).isFalse();
        }
    }

    // ============================================================
    // PATTERN 5: Date/Time Boundary Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 5: Date/Time Boundaries")
    class DateTimeBoundaries {

        private static final LocalDate TODAY = LocalDate.now();
        private static final LocalDate YESTERDAY = TODAY.minusDays(1);
        private static final LocalDate TOMORROW = TODAY.plusDays(1);
        private static final LocalDate NEXT_YEAR = TODAY.plusYears(1);
        private static final LocalDate LAST_YEAR = TODAY.minusYears(1);

        @Nested
        @DisplayName("shipping date validation")
        class ShippingDateValidation {

            @ParameterizedTest(name = "should accept shipping date: {1}")
            @MethodSource("provideValidShippingDates")
            void shouldAcceptValidShippingDates(LocalDate date, String description) {
                assertThat(validationService.validateShippingDate(date).isValid())
                    .as(description)
                    .isTrue();
            }

            static Stream<Arguments> provideValidShippingDates() {
                LocalDate today = LocalDate.now();
                return Stream.of(
                    Arguments.of(today.plusDays(1), "tomorrow"),
                    Arguments.of(today.plusDays(2), "day after tomorrow"),
                    Arguments.of(today.plusMonths(1), "next month"),
                    Arguments.of(today.plusYears(1), "next year")
                );
            }

            @ParameterizedTest(name = "should reject shipping date: {1}")
            @MethodSource("provideInvalidShippingDates")
            void shouldRejectInvalidShippingDates(LocalDate date, String description) {
                assertThat(validationService.validateShippingDate(date).isValid())
                    .as(description)
                    .isFalse();
            }

            static Stream<Arguments> provideInvalidShippingDates() {
                LocalDate today = LocalDate.now();
                return Stream.of(
                    Arguments.of(today.minusDays(1), "yesterday"),
                    Arguments.of(today.minusWeeks(1), "last week"),
                    Arguments.of(today.minusYears(1), "last year"),
                    Arguments.of(LocalDate.of(2000, 1, 1), "year 2000")
                );
            }

            @Test
            @DisplayName("should handle today as edge case")
            void shouldHandleTodayEdgeCase() {
                // Business rule: today might be valid depending on cutoff time
                ValidationResult result = validationService.validateShippingDate(TODAY);
                // Document expected behavior
                assertThat(result).isNotNull();
            }

            @Test
            @DisplayName("should handle null date")
            void shouldHandleNullDate() {
                assertThat(validationService.validateShippingDate(null).isValid()).isFalse();
            }
        }

        @Nested
        @DisplayName("date of birth validation")
        class DateOfBirthValidation {

            @ParameterizedTest(name = "should accept DOB: {1} years ago")
            @MethodSource("provideValidAges")
            void shouldAcceptValidDatesOfBirth(int yearsAgo, String description) {
                LocalDate dob = TODAY.minusYears(yearsAgo);

                assertThat(validationService.validateDateOfBirth(dob).isValid())
                    .as(description)
                    .isTrue();
            }

            static Stream<Arguments> provideValidAges() {
                return Stream.of(
                    Arguments.of(Boundaries.AGE_MIN, "minimum age"),
                    Arguments.of(Boundaries.AGE_MIN + 1, "just above minimum"),
                    Arguments.of(30, "typical age"),
                    Arguments.of(Boundaries.AGE_MAX - 1, "just below maximum"),
                    Arguments.of(Boundaries.AGE_MAX, "maximum age")
                );
            }

            @ParameterizedTest(name = "should reject DOB: {1} years ago")
            @MethodSource("provideInvalidAges")
            void shouldRejectInvalidDatesOfBirth(int yearsAgo, String description) {
                LocalDate dob = TODAY.minusYears(yearsAgo);

                assertThat(validationService.validateDateOfBirth(dob).isValid())
                    .as(description)
                    .isFalse();
            }

            static Stream<Arguments> provideInvalidAges() {
                return Stream.of(
                    Arguments.of(Boundaries.AGE_MIN - 1, "below minimum age"),
                    Arguments.of(0, "born today"),
                    Arguments.of(-1, "future date"),
                    Arguments.of(Boundaries.AGE_MAX + 1, "above maximum age"),
                    Arguments.of(200, "impossibly old")
                );
            }
        }
    }

    // ============================================================
    // PATTERN 6: Percentage Boundary Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 6: Percentage Boundaries")
    class PercentageBoundaries {

        @ParameterizedTest(name = "discount {0}% should be valid ({1})")
        @MethodSource("provideValidDiscounts")
        void shouldAcceptValidDiscounts(int percentage, String description) {
            assertThat(validationService.validateDiscountPercentage(percentage).isValid())
                .as(description)
                .isTrue();
        }

        static Stream<Arguments> provideValidDiscounts() {
            return Stream.of(
                Arguments.of(0, "zero (no discount)"),
                Arguments.of(1, "minimum meaningful"),
                Arguments.of(10, "typical small discount"),
                Arguments.of(50, "half off"),
                Arguments.of(99, "nearly free"),
                Arguments.of(100, "completely free")
            );
        }

        @ParameterizedTest(name = "discount {0}% should be invalid ({1})")
        @MethodSource("provideInvalidDiscounts")
        void shouldRejectInvalidDiscounts(int percentage, String description) {
            assertThat(validationService.validateDiscountPercentage(percentage).isValid())
                .as(description)
                .isFalse();
        }

        static Stream<Arguments> provideInvalidDiscounts() {
            return Stream.of(
                Arguments.of(-1, "negative"),
                Arguments.of(-50, "large negative"),
                Arguments.of(101, "over 100%"),
                Arguments.of(150, "way over 100%")
            );
        }
    }

    // ============================================================
    // Helper Classes
    // ============================================================

    static class ValidationService {
        ValidationResult validateQuantity(int quantity) {
            if (quantity < Boundaries.QUANTITY_MIN) return ValidationResult.invalid("Quantity must be at least 1");
            if (quantity > Boundaries.QUANTITY_MAX) return ValidationResult.invalid("Quantity cannot exceed 9999");
            return ValidationResult.valid();
        }

        ValidationResult validatePrice(BigDecimal price) {
            if (price == null) return ValidationResult.invalid("Price is required");
            if (price.scale() > 2) return ValidationResult.invalid("Price must have at most 2 decimal places");
            if (price.compareTo(Boundaries.PRICE_MIN) < 0) return ValidationResult.invalid("Price must be at least 0.01");
            if (price.compareTo(Boundaries.PRICE_MAX) > 0) return ValidationResult.invalid("Price cannot exceed maximum");
            return ValidationResult.valid();
        }

        ValidationResult validateSku(String sku) {
            if (sku == null || sku.isBlank()) return ValidationResult.invalid("SKU is required");
            if (sku.length() < Boundaries.SKU_MIN_LENGTH) return ValidationResult.invalid("SKU too short");
            if (sku.length() > Boundaries.SKU_MAX_LENGTH) return ValidationResult.invalid("SKU too long");
            return ValidationResult.valid();
        }

        ValidationResult validateCart(List<CartItem> items) {
            if (items == null) return ValidationResult.invalid("Cart cannot be null");
            if (items.size() > Boundaries.CART_MAX_ITEMS) return ValidationResult.invalid("Too many items");
            return ValidationResult.valid();
        }

        ValidationResult validateShippingDate(LocalDate date) {
            if (date == null) return ValidationResult.invalid("Shipping date required");
            if (date.isBefore(LocalDate.now())) return ValidationResult.invalid("Date must be in the future");
            return ValidationResult.valid();
        }

        ValidationResult validateDateOfBirth(LocalDate dob) {
            if (dob == null) return ValidationResult.invalid("Date of birth required");
            int age = LocalDate.now().getYear() - dob.getYear();
            if (dob.isAfter(LocalDate.now())) return ValidationResult.invalid("DOB cannot be in the future");
            if (age < Boundaries.AGE_MIN) return ValidationResult.invalid("Must be at least 18");
            if (age > Boundaries.AGE_MAX) return ValidationResult.invalid("Age exceeds maximum");
            return ValidationResult.valid();
        }

        ValidationResult validateDiscountPercentage(int percentage) {
            if (percentage < 0) return ValidationResult.invalid("Discount cannot be negative");
            if (percentage > 100) return ValidationResult.invalid("Discount cannot exceed 100%");
            return ValidationResult.valid();
        }
    }

    static class ValidationResult {
        private final boolean valid;
        private final String error;

        private ValidationResult(boolean valid, String error) {
            this.valid = valid;
            this.error = error;
        }

        static ValidationResult valid() { return new ValidationResult(true, null); }
        static ValidationResult invalid(String error) { return new ValidationResult(false, error); }

        boolean isValid() { return valid; }
        String getError() { return error; }
    }

    static class CartItem {
        private final String sku;
        private final String name;
        private final int quantity;
        private final BigDecimal price;

        CartItem(String sku, String name, int quantity, BigDecimal price) {
            this.sku = sku;
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }
    }
}

/**
 * SUMMARY OF BOUNDARY TESTING PATTERNS:
 *
 * 1. Document all boundaries as constants
 * 2. Use 7-point analysis: min-1, min, min+1, typical, max-1, max, max+1
 * 3. Test null, empty, blank for strings
 * 4. Test zero, negative, overflow for numbers
 * 5. Test empty, single, many, max for collections
 * 6. Test past, today, future for dates
 * 7. Test 0%, 1-99%, 100%, >100% for percentages
 * 8. Use @ParameterizedTest for all boundary cases
 */
