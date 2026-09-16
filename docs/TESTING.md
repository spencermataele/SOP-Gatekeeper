# Running and understanding the verification suite

This is the first testing-foundation increment. It verifies existing authentication, database
isolation, department relationships, and request logging. It does not yet certify the planned
approval lifecycle, self-approval policy, multi-client deployments, or document comparison.

## Quick start on this Windows workspace

The project-local tools described in [DEVELOPMENT.md](../DEVELOPMENT.md) must be installed.
If MySQL is not already running, start it, then provision the test schema once:

```powershell
./scripts/dev.ps1 database
./scripts/dev.ps1 prepare-tests
```

`prepare-tests` creates `sop_gatekeeper_test` and a dedicated `gatekeeper_test` account. Its
privileges cover that schema only. It does not clear development data or drop existing schemas.
The random local test password is stored in ignored `.local/test-db.properties`.

Run checks from the repository root:

```powershell
./scripts/dev.ps1 test-unit
./scripts/dev.ps1 test-backend
./scripts/dev.ps1 test-frontend
```

The first command runs fast backend unit tests without starting Spring or connecting to MySQL.
The second includes real-MySQL integration tests. The third launches headless Chrome and writes
an HTML coverage report. Each command returns a nonzero exit code when a test fails.

Run a single backend class or method:

```powershell
./scripts/dev.ps1 test-backend -Test AuthenticationApiTest
./scripts/dev.ps1 test-backend -Test 'AuthenticationApiTest#wrongPasswordDoesNotIssueAToken'
```

## IntelliJ run and debug

Import the root Maven project and use Java 21 for the project SDK. Shared configurations under
`.run/` should appear as **Backend unit tests** and **Backend integration tests**. They use the
Maven module named `backend`; if IntelliJ assigned another module name, select the backend module
in the configuration. Run or Debug either configuration using IntelliJ's usual controls.

Provision the test database first for integration tests. Configurations contain no credentials;
the test support code reads the ignored local settings file. You can also use the gutter icon next
to an individual test. Named configurations and their XML are provided, but native IntelliJ UI
execution must be confirmed on your installation.

Try these learning exercises:

1. Debug `AuthControllerTest.invalidCredentialsNeverLoadAUserOrIssueAToken`. Observe the mocked
   authentication rejection and the assertion that no user lookup or token generation occurs.
2. Run `Woven1ApplicationTests`. The console/report verifies the test database identity and that
   its account cannot read `woven.users` in the development schema.
3. Debug `AuthenticationApiTest.authenticatedAuthorCanRetrieveTheirIdentityAndPublishedSops`.
   Unlike the unit test, this uses actual Spring security, BCrypt, JWT, repositories, and MySQL.
4. Open the frontend coverage report after running the browser tests. The report includes only
   code loaded by the suite; its percentage is not application-wide coverage. Passing these tests
   does not mean every screen is covered.

## Where results live

| Evidence | Location |
| --- | --- |
| Java test summaries and JUnit XML | `backend/target/surefire-reports/` |
| Integration-test application log | `backend/target/test-logs/backend.log` |
| Frontend HTML coverage | `frontend/coverage/frontend/index.html` |
| Local development application log | `.local/logs/backend-application.log` after starting through the script |
| CI reports and console output | Workflow run artifacts: `backend-verification` and `frontend-verification` |

IntelliJ's direct JUnit runner displays its own results instead of generating Maven Surefire
reports; run the Maven/script command when you want the XML/text report files. Test reports and
logs are generated artifacts, not source-controlled files.

## Isolation and repeatability

All database-backed test classes extend `DatabaseTest`. Its dynamic properties fix the host to
loopback, port to 3307, database to `sop_gatekeeper_test`, and user to `gatekeeper_test`. A dedicated
test JWT key is used. No arbitrary database URL is accepted. If the test password is unavailable,
the test fails instead of falling back to application credentials.

Locally the schema shares the MySQL server with development, but uses separate data and restricted
credentials. This is test-data isolation, not a separate server instance. CI starts a disposable
MySQL 8.4.8 service for each backend job. Both use the actual Flyway migrations.

Mutating integration tests use transactions that roll back after each test. The deterministic
`test.author` fixture exists only within its test transaction; it is not a persistent login. The
seeded MVP records remain baseline fixtures for this increment. Do not run simultaneous integration
suites against the same local schema; each parallel CI job gets its own service.

MockMvc executes synchronous requests on the test thread, allowing rollback of their database
work. Future tests using real HTTP servers, asynchronous workers, or independently committed
transactions need explicit fixture cleanup or disposable databases; do not assume these also roll back.

## What the corrected frontend test proves

The previous failing test supplied `subgroups: null` and expected a non-null response. Subgroups
are children; the department's parent is `orgGroupId`. The HTTP service does not manufacture children.

The replacement checks preserve parent IDs and actual child data, accept the backend's empty-array
response for a department without children, and propagate an API validation error to the caller.
Real-MySQL service tests separately verify that a department without children retains its parent
and that a nonexistent parent cannot create a department. No production behavior was changed to
satisfy the test. The frontend's mocked 400 response tests error propagation, not a claim about
the backend's exact status for every missing-parent condition.

## Logs and correlation

HTTP responses include a server-generated `X-Request-ID`. The request filter logs method, status,
outcome, and duration; the application log pattern includes the request ID. It intentionally omits
request paths, queries, headers, bodies, and exception messages that might contain confidential data.
Client-provided request IDs are not trusted. The logging context is restored after success or failure.

Existing ad-hoc runtime prints of SOP content and approval debug values have been removed, and raw
Hibernate SQL output is disabled. This is a diagnostic baseline, not the future business audit trail.
Background-job correlation, production JSON logging, and notification delivery reports will be added
with their respective features. Logging confidentiality tests cover the new request filter, not an
exhaustive audit of every third-party logger.

## Continuous integration

`.github/workflows/verify.yml` runs on pushes, pull requests, and manual dispatch. Backend and frontend
jobs are independent. They run tests and package/build their respective components. Reports upload
even after failures and expire after 14 days. The workflow does not deploy or access client databases.
Its database credentials are disposable test values, not production secrets.

This checks the components separately; it does not establish that the existing executable JAR
correctly serves the Angular application. Deployment packaging remains a separate task.

The workflow must be pushed before GitHub can execute it. Repository branch protection/rulesets
must be configured separately to make successful checks mandatory for merging. A local passing
run does not establish that a hosted CI run passed.

## Next verification increments

- Permission matrix and approved lifecycle tests, including self-approval and participant tracking.
- Concurrent saves/publication and transaction/outbox failure injection.
- Browser end-to-end and accessibility checks using controlled test personas.
- Notification provider fakes, delivery retry scenarios, and client deployment isolation.
- Migration and backup/restore validation before shared-environment changes.

References: [Spring Boot testing](https://docs.spring.io/spring-boot/3.5/reference/testing/spring-boot-applications.html)
and [GitHub workflow artifacts](https://docs.github.com/en/actions/tutorials/store-and-share-data).
