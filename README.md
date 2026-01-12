# Test Optimization Lab - Java Spring Boot

Hands-on lab for improving test coverage using GitHub Copilot with the Critique-then-Create methodology.

## Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 17 | Programming Language |
| **Spring Boot** | 3.2 | Application Framework |
| **JUnit** | 5 | Testing Framework |
| **Mockito** | Latest | Mocking Framework |
| **JaCoCo** | Latest | Code Coverage |

This lab uses an e-commerce domain (payments, orders, users) with intentionally weak test coverage (~30-40%) for you to analyze and improve.

## What You'll Do

| Stage | Focus | Outcome |
|-------|-------|---------|
| **Stage 1** | Coverage Gap Analysis | Analysis with prioritized findings |
| **Stage 2** | Test Enhancement | Refactored tests, +15% coverage |
| **Stage 3** | Quality Gates | Jenkins pipeline with SonarQube integration |

## Quick Start

```bash
mvn clean test
mvn jacoco:report
open target/site/jacoco/index.html
```

## Custom Agents & Prompts

Located in `.github/agents/` and `.github/prompts/`:

| Agent/Prompt | Purpose |
|--------------|---------|
| **Test Critique Agent** | Analyze coverage gaps, identify anti-patterns |
| **Test Create Agent** | Generate tests from analysis |
| **Test Quality Gate Agent** | Configure Jenkins/SonarQube |
| `/coverage-analysis` | Quick coverage gap analysis |
| `/refactor-to-parameterized` | Convert to `@ParameterizedTest` |
| `/fix-flaky-test` | Diagnose timing issues |
| `/generate-error-tests` | Exception tests |
| `/generate-boundary-tests` | 7-point boundary value tests |

## Issues to Find & Fix

| Issue Type | File |
|------------|------|
| Weak coverage (~35%) | `PaymentServiceTest.java` |
| Redundant tests | `UserServiceTest.java` |
| Flaky async tests | `OrderServiceTest.java` |
| Missing tests | `NotificationService` (no tests) |

## Golden Examples

Reference implementations in `.golden-examples/`:

- `parameterized-tests/` - `@ParameterizedTest` patterns
- `mockito-mocking/` - BDD Mockito patterns
- `async-patterns/` - `CompletableFuture` tests
- `error-handling/` - Exception tests
- `boundary-testing/` - 7-point boundary analysis
- `repository-testing/` - `@DataJpaTest` patterns

## Target Metrics

| Metric | Baseline | Target |
|--------|----------|--------|
| Line Coverage | ~30-40% | >75% |
| Branch Coverage | ~25-35% | >60% |
| Flaky Tests | Present | Fixed |
| Redundant Tests | Present | Parameterized |

## Prerequisites

- JDK 17+
- Maven 3.9+
- VS Code or IntelliJ with GitHub Copilot

## License

See [LICENSE](LICENSE) file for details.


