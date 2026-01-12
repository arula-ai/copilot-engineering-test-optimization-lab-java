---
description: "Diagnose and fix flaky tests by identifying timing issues, shared state, and non-deterministic patterns"
---

# Fix Flaky Test

Diagnose the root cause of test flakiness and apply deterministic patterns.

## Prerequisites

Reference the anti-patterns to identify flaky tests.

## Common Flaky Patterns & Fixes

| Anti-Pattern | Fix |
|--------------|-----|
| `Thread.sleep()` | Use `CompletableFuture.get(timeout, unit)` |
| `get()` without timeout | Add timeout parameter |
| `LocalDateTime.now()` | Inject and mock `Clock` |
| Shared static state | Use `@BeforeEach` reset |
| Missing `@Timeout` | Add `@Timeout(5)` annotation |

**Correct Pattern**:
```java
@Test
@Timeout(5)
void shouldCompleteAsyncOperation() {
    CompletableFuture<Result> future = service.processAsync(request);

    assertThat(future)
        .succeedsWithin(Duration.ofSeconds(2))
        .satisfies(result -> assertThat(result.isSuccess()).isTrue());
}
```

**Reference**: `.golden-examples/async-patterns/`

## Diagnosis Steps

1. **Identify the failure pattern**:
   - Does it fail randomly? (timing issue)
   - Does it fail after other tests? (shared state)
   - Does it fail only in CI? (environment dependency)

2. **Run test in isolation**:
   ```bash
   mvn test -Dtest=TestClass#testMethod
   ```

3. **Run multiple times to confirm flakiness**:
   ```bash
   for i in {1..10}; do mvn test -Dtest=TestClass#testMethod; done
   ```

## Output

1. Document the root cause
2. Show before/after code
3. Apply the fix
4. Verify with 5+ consecutive passes
