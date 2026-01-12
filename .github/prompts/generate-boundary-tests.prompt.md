---
description: "Generate boundary value tests using 7-point analysis for numeric limits, string lengths, and collection sizes"
---

# Generate Boundary Value Tests

Create thorough boundary tests using the 7-point analysis method.

## Prerequisites

Reference the boundary gaps in `#file:docs/TEST_ANALYSIS.md`.

## 7-Point Boundary Analysis

For each boundary, test these 7 values:

1. **min - 1** (invalid, just below minimum)
2. **min** (valid, at minimum)
3. **min + 1** (valid, just above minimum)
4. **typical** (valid, normal value)
5. **max - 1** (valid, just below maximum)
6. **max** (valid, at maximum)
7. **max + 1** (invalid, just above maximum)

## Instructions

### Document Boundaries as Constants

```java
static class Boundaries {
    static final BigDecimal AMOUNT_MIN = new BigDecimal("0.01");
    static final BigDecimal AMOUNT_MAX = new BigDecimal("999999.99");
    static final int QUANTITY_MIN = 1;
    static final int QUANTITY_MAX = 9999;
}
```

### Testing Pattern

```java
@Nested
class AmountValidation {

    @ParameterizedTest(name = "amount {0} should be valid={1} ({2})")
    @MethodSource("provideAmountBoundaries")
    void shouldValidateAmountBoundaries(
            BigDecimal amount, boolean expected, String reason) {
        assertThat(validator.isValidAmount(amount)).isEqualTo(expected);
    }

    static Stream<Arguments> provideAmountBoundaries() {
        return Stream.of(
            Arguments.of(new BigDecimal("0"), false, "below minimum"),
            Arguments.of(Boundaries.AMOUNT_MIN, true, "at minimum"),
            Arguments.of(new BigDecimal("0.02"), true, "above minimum"),
            Arguments.of(new BigDecimal("500"), true, "typical value"),
            Arguments.of(new BigDecimal("999999.98"), true, "below maximum"),
            Arguments.of(Boundaries.AMOUNT_MAX, true, "at maximum"),
            Arguments.of(new BigDecimal("1000000"), false, "above maximum")
        );
    }

    @ParameterizedTest
    @NullSource
    void shouldRejectNullAmount(BigDecimal amount) {
        assertThrows(NullPointerException.class,
            () -> validator.isValidAmount(amount));
    }
}
```

**Reference**: `.golden-examples/boundary-testing/`

## Boundary Categories

1. **Numeric Values**: amounts, quantities, percentages, ages
2. **String Lengths**: names, descriptions, codes, identifiers
3. **Collection Sizes**: list items, array elements, batch sizes
4. **Date/Time**: past dates, future dates, time ranges
5. **Special Values**: null, empty, whitespace, zero

## Output

Generate tests that document boundaries as constants and test all 7 points for each boundary identified.
