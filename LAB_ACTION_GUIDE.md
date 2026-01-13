# Lab Action Guide – Java/Spring Boot

Follow these lean steps using the Critique-then-Create methodology. Use the custom agents in `.github/agents/` and prompts in `.github/prompts/` throughout.

## How to Use Custom Agents & Prompts

### Selecting an Agent
1. Open **GitHub Copilot Chat** in VS Code (Ctrl+Shift+I or Cmd+Shift+I)
2. Click the **Agents dropdown** at the top of the chat input (shows current agent name)
3. Select the agent you want to use (e.g., "Test Critique", "Test Create", "Test Quality Gate")
4. The agent's instructions will guide Copilot's responses for that conversation

### Using Prompts
- Type `/` in Copilot Chat to see available prompts
- Select a prompt like `/coverage-analysis` or `/fix-flaky-test`
- The prompt template will be applied to your request

### Agent Mode vs Custom Agents
- **Agent Mode**: Default Copilot behavior with full tool access
- **Custom Agents**: Specialized agents with domain-specific instructions (select from dropdown)

---

## Quick Reference

| Stage | Agent (Select from Dropdown) | Core Artifacts / Commands |
| --- | --- | --- |
| 0 | Agent Mode (default) | `mvn clean compile`, `mvn test`, `mvn jacoco:report` |
| 1 | **Test Critique** | `docs/TEST_ANALYSIS.md`, `.golden-examples/` |
| 2 | **Test Create** + Prompts | `*Test.java` files, coverage improvement |
| 3 | **Test Quality Gate** | `Jenkinsfile`, `pom.xml` (SonarQube), JaCoCo thresholds |
| 4 | Agent Mode (default) | Final validation, commit changes |

---

## Stage 0 – Environment Setup

Use **Agent Mode** (default) for setup commands:

- `#runInTerminal cd java` (navigate to Java project folder)
- `#runInTerminal mvn clean compile` (verify build)
- `#runInTerminal mvn test` (verify tests run)
- `#runInTerminal mvn jacoco:report` (generate baseline coverage)
- `#runInTerminal open target/site/jacoco/index.html` (review baseline coverage)
- Note: Record baseline coverage percentage for comparison

---

## Stage 1 – Coverage Gap Analysis (CRITIQUE Phase)

**Select "Test Critique" agent from the dropdown**, then:

1. Ask: "Analyze all services in `com.example.lab.service` for coverage gaps"
2. Ask: "Identify anti-patterns like flaky tests and redundancies"
3. Ask: "Document findings and save to `docs/TEST_ANALYSIS.md`"

**Alternative**: Use `/coverage-analysis` prompt for quick analysis

After analysis, verify:
- `#runInTerminal cat docs/TEST_ANALYSIS.md` (confirm file created)
- Review `.golden-examples/` to understand available patterns

### Key Analysis Areas
- PaymentService: Only CREDIT_CARD tested, missing other payment methods
- UserService: Redundant validation tests (not parameterized)
- OrderService: Flaky async tests (Thread.sleep, no timeout)
- NotificationService: Missing test file entirely

---

## Stage 2 – Test Enhancement (CREATE Phase)

**Select "Test Create" agent from the dropdown** for all Stage 2 tasks.

### Task 2.1 – Refactor Redundant Tests
- Reference: `#file:docs/TEST_ANALYSIS.md` redundancy section
- Use prompt: `/refactor-to-parameterized` for UserServiceTest.java
- Reference: `.golden-examples/parameterized-tests/` for @ParameterizedTest pattern
- Verify: `#runInTerminal mvn test -Dtest=UserServiceTest`
- Target: 50%+ line reduction using @CsvSource/@MethodSource

### Task 2.2 – Fix Flaky Tests
- Reference: `#file:docs/TEST_ANALYSIS.md` anti-patterns section
- Use prompt: `/fix-flaky-test` for OrderServiceTest.java
- Reference: `.golden-examples/async-patterns/` for CompletableFuture patterns
- Verify: `#runInTerminal for i in {1..5}; do mvn test -Dtest=OrderServiceTest; done`

### Task 2.3 – Generate Mockito Tests
- Reference: `#file:docs/TEST_ANALYSIS.md` coverage gaps
- Reference: `.golden-examples/mockito-mocking/` for BDD Mockito patterns
- Use `@EnumSource(PaymentMethod.class)` for all payment methods
- Verify: `#runInTerminal mvn test -Dtest=PaymentServiceTest`

### Task 2.4 – Generate Exception Tests
- Reference: `#file:docs/TEST_ANALYSIS.md` exception gaps
- Use prompt: `/generate-error-tests` for PaymentService
- Reference: `.golden-examples/error-handling/`
- Verify exception messages and error codes

### Task 2.5 – Generate Boundary Tests
- Use prompt: `/generate-boundary-tests` for validators
- Reference: `.golden-examples/boundary-testing/` for 7-point analysis
- Document boundaries as constants in test class

### Verification
- `#runInTerminal mvn clean test jacoco:report`
- Compare coverage to Stage 0 baseline
- Target: Coverage improved by 15%+

---

## Stage 3 – Quality Gates & CI (Jenkins + SonarQube)

**Select "Test Quality Gate" agent from the dropdown**, then:

1. Ask: "Configure JaCoCo coverage thresholds in pom.xml"
2. Ask: "Add SonarQube Maven plugin and profile"
3. Ask: "Generate Jenkinsfile with SonarQube integration"
4. Ask: "Configure Surefire for parallel test execution"

### Configuration Updates
- `pom.xml` – JaCoCo thresholds (60% LINE, 50% BRANCH)
- `pom.xml` – SonarQube profile (`-Psonar`)
- `pom.xml` – Surefire parallel execution
- `Jenkinsfile` – Pipeline with test, coverage, and quality gate stages

### Verification
- `#runInTerminal mvn clean verify` (verify thresholds enforced)
- `#runInTerminal mvn help:effective-pom | grep -A20 jacoco` (verify JaCoCo config)
- `#runInTerminal cat Jenkinsfile` (verify pipeline structure)

---

## Stage 4 – Final Validation & Submission

Switch back to **Agent Mode** (default) for final validation:

- `#runInTerminal mvn clean compile` (no errors)
- `#runInTerminal mvn test` (all tests pass)
- `#runInTerminal mvn verify` (thresholds met)
- Review `docs/TEST_ANALYSIS.md` for completeness
- Commit changes with meaningful message
- Push branch and open PR if required

---

## Agent & Prompt Reference

### Custom Agents (Select from Dropdown)

| Agent | When to Use |
| --- | --- |
| **Test Critique** | Stage 1 – analyzing coverage gaps, anti-patterns |
| **Test Create** | Stage 2 – generating tests from analysis |
| **Test Quality Gate** | Stage 3 – Jenkins/SonarQube configuration |
| **Lab Validator** | Validate lab setup and instructions |

### Prompts (Type `/` in Chat)

| Prompt | Purpose |
| --- | --- |
| `/coverage-analysis` | Quick coverage gap analysis |
| `/refactor-to-parameterized` | Converting to @ParameterizedTest |
| `/fix-flaky-test` | Diagnosing and fixing timing issues |
| `/generate-error-tests` | Exception and error path tests |
| `/generate-boundary-tests` | 7-point boundary value tests |

---

## Golden Examples Reference

| Pattern | Location | Use For |
| --- | --- | --- |
| Parameterized Tests | `.golden-examples/parameterized-tests/` | @ParameterizedTest patterns |
| Mockito Mocking | `.golden-examples/mockito-mocking/` | BDD style mocking |
| Repository Testing | `.golden-examples/repository-testing/` | @DataJpaTest patterns |
| Async Patterns | `.golden-examples/async-patterns/` | CompletableFuture tests |
| Error Handling | `.golden-examples/error-handling/` | Exception tests |
| Boundary Testing | `.golden-examples/boundary-testing/` | Edge case tests |

---

## Workflow Loop

```
Test Critique Agent → docs/TEST_ANALYSIS.md → Test Create Agent → Test Quality Gate Agent
```

Each stage builds on the previous. The analysis file (`docs/TEST_ANALYSIS.md`) is the bridge between CRITIQUE and CREATE phases.
