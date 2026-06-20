---
name: pre-build-unit-tests
description: >
  Use when about to run a project build, compile, or 'mvn spring-boot:run'.
  Use when new Java service or controller code was just implemented and needs validation.
  Use when asked to "test before build", "generate unit tests", "validate new code",
  "run tests before building", "write tests for the new code", or "pre-build check".
  Generates JUnit 5 unit tests for the latest implemented functionality, runs them,
  and only allows the build to proceed when all tests pass.
  NOT for: integration tests, end-to-end tests, frontend JS testing.
---

# Pre-Build Unit Test Skill

## Purpose

Before every project build, identify the most recently added or modified Java source files, generate JUnit 5 unit tests that cover their core logic, execute the tests, and block the build if any test fails.

---

## Step 1 — Identify Changed Files

Scan `src/main/java/` for the files that were created or modified in the current agent session (or the last logical batch of changes).

Focus on:
- Classes in `service/` — these contain business logic and are the primary test target
- Classes in `controller/` — test request/response mapping and status codes
- Classes in `model/` — test any non-trivial methods

Skip:
- `repository/` interfaces (Spring Data proxies — no logic to unit-test)
- `HairdresserApplication.java`

---

## Step 2 — Generate Tests

For each target class, create or update a corresponding test class at:

```
src/test/java/com/hairdresser/<same-subpackage>/<ClassName>Test.java
```

### Rules

- Framework: **JUnit 5** (`@ExtendWith(MockitoExtension.class)`)
- Mocking: **Mockito** (`@Mock`, `@InjectMocks`, `when(…).thenReturn(…)`, `verify(…)`)
- No Spring context (`@SpringBootTest`) — keep tests fast and isolated
- Test class naming: `<ClassName>Test`
- One `@Test` method per distinct behaviour/branch. Name format: `methodName_scenario_expectedResult`
- Use `assertThat` from AssertJ (`org.assertj.core.api.Assertions`) for readable assertions
- Cover at minimum:
  - Happy path (normal input → expected output)
  - Edge case that returns empty / null / 404 equivalent
  - Any branch involving date/time logic (critical for `BookingService`)

### Mandatory test cases by class

| Class | Required test scenarios |
|---|---|
| `BookingService` | `getAvailableSlots` returns all slots when no bookings exist; `getAvailableSlots` omits already-booked slots; `createBooking` throws / returns error when slot is already taken |
| `SalonService` | `findById` returns salon when present; `findById` throws `NoSuchElementException` (or similar) when absent; `save` persists and returns the salon |
| `SalonController` | `GET /api/salons` returns 200 with list; `GET /api/salons/{id}` returns 404 when salon not found |
| `BookingController` | `POST /api/bookings` returns 201 on success; `POST /api/bookings` returns 409 or 400 when slot is taken |

Only generate tests for classes that actually exist in the codebase. Skip entries in the table above if the class has not been implemented yet.

---

## Step 3 — Verify Test Dependencies in pom.xml

Confirm `pom.xml` contains:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

`spring-boot-starter-test` bundles JUnit 5, Mockito, and AssertJ — no extra entries needed.
Add only if missing.

---

## Step 4 — Run the Tests

Execute:

```bash
cd hairdresser
mvn test
```

Capture output. Parse for:
- `BUILD SUCCESS` → all tests passed, proceed to build
- `BUILD FAILURE` or any `FAILED` test → **stop**, report which tests failed and why, fix the failing code or the test before retrying

---

## Step 5 — Decision Gate

| Outcome | Action |
|---|---|
| All tests pass | Report "All tests passed. Proceeding with build." then continue with `mvn spring-boot:run` or whatever build step was requested |
| Any test fails | Report each failure with class name, method name, and failure message. Fix the root cause (production code or incorrect test expectation). Re-run `mvn test`. Do NOT proceed to build until clean. |

---

## Coding Conventions for Tests

- Follow the same package root as production code: `com.hairdresser`
- No `var` in tests — use explicit types
- Use constructor injection in production code so `@InjectMocks` works correctly
- Do not access the real database or start a Spring context in unit tests
- Use `LocalDate` / `LocalTime` directly in test data; do not use `new Date()` or string parsing where avoidable
