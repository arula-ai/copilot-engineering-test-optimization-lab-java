---
description: "Refactor redundant tests to use @ParameterizedTest for JUnit"
---

# Refactor Redundant Tests to Parameterized

Convert repetitive test patterns into concise, maintainable parameterized tests.

## Prerequisites

Reference the analysis to identify redundant tests.

## Instructions

1. **Identify redundant patterns**:
   - Multiple `@Test` methods with identical structure
   - Only test data differs between methods

2. **Convert to @ParameterizedTest**:
   ```java
   // Before: 6 separate @Test methods

   // After: 1 parameterized test
   @ParameterizedTest(name = "validates {0} as {1}")
   @CsvSource({
       "valid@email.com, true",
       "user@domain.org, true",
       "invalid, false"
   })
   void shouldValidateEmail(String email, boolean expected) {
       assertThat(validator.isValid(email)).isEqualTo(expected);
   }
   ```

3. **For complex objects, use @MethodSource**:
   ```java
   @ParameterizedTest
   @MethodSource("providePaymentScenarios")
   void shouldProcessPayment(PaymentRequest request, boolean expected) { ... }

   static Stream<Arguments> providePaymentScenarios() {
       return Stream.of(
           Arguments.of(validRequest(), true),
           Arguments.of(invalidRequest(), false)
       );
   }
   ```

4. **Reference pattern**: `.golden-examples/parameterized-tests/`

## Target

Reduce line count by **50% or more** while maintaining full test coverage.

## Output

Apply refactoring to the identified test file and verify tests still pass.
