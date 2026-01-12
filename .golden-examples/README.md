# Golden Examples - Java 17 Spring Boot Testing Patterns

## Purpose

This folder contains **production-ready test patterns** that serve as references for GitHub Copilot. When generating or refactoring tests, always reference these golden examples to ensure Copilot produces high-quality, idempotent, and maintainable code.

## How to Use Golden Examples

### 1. Reference in Copilot Prompts

Always include the golden example path in your prompts:

```
@workspace Generate tests for PaymentService.

Reference the patterns in .golden-examples/mockito-mocking/MockitoPatterns.golden.java:
- Use @ExtendWith(MockitoExtension.class)
- Apply @Mock for dependencies
- Use @InjectMocks for the service under test
- Verify mock interactions with verify()

Apply these patterns to PaymentServiceTest.java.
```

### 2. Critique-then-Create Methodology

1. **CRITIQUE**: Analyze existing tests against golden examples
2. **CREATE**: Generate new tests following the patterns

### 3. Pattern Selection Guide

| Scenario | Golden Example |
|----------|----------------|
| Multiple similar test cases | `parameterized-tests/` |
| Service with dependencies | `mockito-mocking/` |
| Repository/database tests | `repository-testing/` |
| Async/CompletableFuture | `async-patterns/` |
| Error handling & exceptions | `error-handling/` |
| Input validation, min/max | `boundary-testing/` |

---

## Directory Structure

```
.golden-examples/
├── README.md                       # This file
├── parameterized-tests/            # @ParameterizedTest patterns
│   └── ParameterizedPatterns.golden.java
├── mockito-mocking/                # @Mock, @InjectMocks patterns
│   └── MockitoPatterns.golden.java
├── repository-testing/             # @DataJpaTest patterns
│   └── RepositoryPatterns.golden.java
├── async-patterns/                 # CompletableFuture testing
│   └── AsyncPatterns.golden.java
├── error-handling/                 # Exception testing
│   └── ErrorHandlingPatterns.golden.java
└── boundary-testing/               # Boundary value analysis
    └── BoundaryPatterns.golden.java
```

---

## Pattern Summaries

### 1. Parameterized Tests
Use JUnit 5's `@ParameterizedTest` for data-driven testing:
- `@ValueSource` for single values
- `@CsvSource` for multiple parameters
- `@MethodSource` for complex objects
- `@EnumSource` for enum testing

### 2. Mockito Mocking
Proper mock setup and verification:
- `@ExtendWith(MockitoExtension.class)` class annotation
- `@Mock` for dependencies
- `@InjectMocks` for service under test
- `when().thenReturn()` for stubbing
- `verify()` for interaction testing

### 3. Repository Testing
Database layer testing with Spring:
- `@DataJpaTest` for slice testing
- `@AutoConfigureTestDatabase` configuration
- Test transaction rollback
- Flushing and clearing persistence context

### 4. Async Patterns
Testing asynchronous operations:
- CompletableFuture assertions
- Timeout handling
- Exception completion testing
- Concurrent operation testing

### 5. Error Handling
Comprehensive exception testing:
- `assertThrows()` for expected exceptions
- Custom exception verification
- Error message validation
- Exception chaining

### 6. Boundary Testing
Systematic boundary value analysis:
- MIN-1, MIN, MIN+1 testing
- MAX-1, MAX, MAX+1 testing
- Null/empty handling
- Type coercion edge cases

---

## Quality Criteria

All golden examples meet these standards:

1. **Idempotent**: Same result every run
2. **Isolated**: No test interdependencies
3. **Fast**: No unnecessary I/O or waits
4. **Readable**: Clear AAA structure
5. **Comprehensive**: All edge cases covered
6. **Documented**: Comments explain patterns

---

## Anti-Patterns to Avoid

| Anti-Pattern | Golden Example Fix |
|--------------|-------------------|
| Real database in unit tests | Use `@Mock` or `@DataJpaTest` |
| Thread.sleep() in tests | Use CompletableFuture.get(timeout) |
| Duplicate test logic | Use @ParameterizedTest |
| Catching generic Exception | Use assertThrows with specific type |
| No mock verification | Add verify() assertions |
| Shared mutable state | Reset mocks in @BeforeEach |

---

## Integration with Copilot Instructions

These golden examples are referenced in `.github/copilot-instructions.md`. Copilot is configured to:

1. Recognize golden example patterns
2. Generate code following these patterns
3. Suggest refactoring toward these patterns
4. Flag anti-patterns in existing code

---

*Remember: Quality comes from patterns. Patterns come from examples.*
