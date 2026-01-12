# GitHub Copilot Instructions - Test Optimization Lab (Java)

> **Purpose**: This repository contains a Java/Spring Boot test optimization lab. This file configures GitHub Copilot to follow the Critique-then-Create methodology and reference Golden Examples.

---

## Core Methodology: Critique-then-Create

**ALWAYS follow this two-phase approach:**

### Phase 1: CRITIQUE (Analysis)
Before generating ANY test code:
1. Identify the service under test
2. Analyze existing test coverage
3. Identify anti-patterns and gaps
4. Document coverage gaps

### Phase 2: CREATE (Implementation)
After completing analysis:
1. Reference Golden Examples from `.golden-examples/` folder
2. Address identified gaps systematically
3. Follow established patterns for consistency

---

## Golden Examples Location
**Path**: `/.golden-examples/`

| Pattern | Location | Use For |
|---------|----------|---------|
| Parameterized Tests | `parameterized-tests/` | @ParameterizedTest patterns |
| Mockito Mocking | `mockito-mocking/` | @Mock, @InjectMocks |
| Repository Testing | `repository-testing/` | @DataJpaTest patterns |
| Async Patterns | `async-patterns/` | CompletableFuture testing |
| Error Handling | `error-handling/` | Exception testing |
| Boundary Testing | `boundary-testing/` | Boundary value analysis |

## Java Testing Standards

### Service Test Setup
```java
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        // Additional setup if needed
    }
}
```

### Parameterized Tests
```java
@ParameterizedTest
@CsvSource({
    "valid@email.com, true",
    "invalid, false",
    "'', false"
})
void shouldValidateEmail(String email, boolean expected) {
    assertThat(service.isValidEmail(email)).isEqualTo(expected);
}
```

### Exception Testing
```java
@Test
void shouldThrowWhenAmountNegative() {
    assertThrows(InvalidPaymentException.class, () ->
        service.processPayment(createPaymentWithAmount(-100))
    );
}
```

### DO Generate for Java:
- @ParameterizedTest for validation rules
- @Mock for all dependencies
- verify() for mock interaction testing
- assertThrows for exception testing

### DO NOT Generate for Java:
- Thread.sleep() in tests
- Catching generic Exception
- Missing mock verification
- Real database in unit tests

---

## Prompt Format

```
@workspace [Action] [Target] [with specific requirements].

CRITIQUE CONTEXT:
[Analysis findings from Phase 1]

CREATE REQUIREMENTS:
Reference [specific Golden Example path]

Apply these patterns:
[Numbered list of specific patterns to use]

[Output format expectations]
```

---

*Remember: Quality comes from patterns. Patterns come from examples. Examples come from Golden Examples.*
