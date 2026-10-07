# Running and understanding the verification suite

The current suite verifies authentication, database isolation, request logging, and governed
SOP workflows. The September 22 local run passed 89 backend tests and 11 frontend tests.
`ApprovalPolicyTest` checks routing in isolation; `LifecycleApiTest` checks authenticated commands,
private-copy visibility, publication history, notification recipients, and legacy route shutdown.
The development Angular workspace now uses that API. Production remains behind its rollout flags;
these checks do not certify client deployment, email delivery, or document intake.

## UI verification and learning exercise

Follow [WORKFLOW-WALKTHROUGH.md](WORKFLOW-WALKTHROUGH.md) to run the same local practice workflow
yourself. The browser walkthrough verified author submission, private owner edits, candidate
replacement, manager approval, published-library visibility, and preserved candidate/activity history.
This is a recorded manual smoke check; unattended browser regression tests are still a next step.

Run `./scripts/dev.ps1 test-frontend` to see the 11 frontend tests, including seven workspace tests.
In `workflow.component.spec.ts`, inspect the stale-save test: it asserts both request/copy versions
are sent and the user's text survives a conflict. The retry test verifies the same command ID is
reused after a network failure, preventing duplicate effects. Other tests cover self-approval
visibility/reason requirements, older-candidate decision blocking, unsaved-navigation protection,
and successful submission when recalculated routing ends the editor's access.

Current local outputs are `.local/logs/workflow-ui-backend-tests.log`,
`.local/logs/workflow-ui-frontend-tests.log`, and `.local/logs/workflow-ui-build.log`.
Frontend coverage is intentionally not a claim of complete UI coverage; browser accessibility,
network failure recovery beyond command retry, and broader navigation still need expansion.

## Approval-policy learning exercise

Run `./scripts/dev.ps1 test-backend -Test ApprovalPolicyTest` (this selected class needs no database),
or use **Backend unit tests** in IntelliJ. Debug
`changingSubmitterDoesNotEraseAnOwnersContribution`: the manager submits a candidate that the
owner helped author. The owner is therefore excluded from normal approval, even though routing
normally selects them. Only the explicit owner/admin exception can authorize a participant.

The policy covers routing, participant exclusion, required self-approval reasons and recipient
deduplication (A01-A06, A14, A24-A25, A29). The integration/API suites below add assignment
revalidation, concurrency, atomic publication and API action visibility; workspace tests verify frontend gating.

## Authenticated API walkthrough

Run `./scripts/dev.ps1 test-backend -Test LifecycleApiTest` or debug that class in IntelliJ.
Start with `authorReviewerEditorAndManagerCompleteWorkflowThroughHttp`. It creates a draft through
HTTP, submits it as the author, creates/edits a private reviewer copy as the process owner,
submits the replacement, verifies self-approval action visibility and then publishes as the manager.
No test sends email. The isolated fixtures roll back after each test.

For V106, debug `rejectedAttemptResubmitsWithAllEarlierContributorsAndKeepsOldDecision` to
inspect why a prior owner-editor still needs explicit self-approval on a later attempt. Debug
`administratorReassignmentArchivesOldOwnerAndEnablesNewReviewer` to follow the transition from
an invalidated assignment to a newly eligible reviewer, including archived routing evidence.
`outboxFailureRollsBackReassignmentAndPreservesOldRouting` verifies recovery is transactional.
Explicit reconciliation when a rejected attempt's published base changed remains a blocked path,
not an automatic rebase. The UI compares stored versions but does not reconcile stale bases.

The suite also checks client-wide published access and new-draft creation, author-only private
draft editing (including an attempted caller-supplied actor ID), stale saves, command retries,
rejection preservation, draft-only cancellation and publication while a reviewer copy is open.
`AuthenticationApiTest.lifecycleEndpointsAreDisabledByDefault` verifies the default deployment
has no new routes. See [LIFECYCLE-API.md](LIFECYCLE-API.md) for the opt-in development contract.

## Lifecycle schema learning exercise

Run `./scripts/dev.ps1 test-backend -Test LifecycleSchemaIntegrationTest`, or select that class
in IntelliJ. It uses the isolated MySQL schema and rolls back its fixtures. V103 adds the new
tables without changing the MVP tables. The full backend suite also checks authentication and
the existing application against that expanded schema.

Debug `candidateAndReviewerCopyMustBelongToSameRequestEvenWithinSameDocument`: a candidate
from a different request is rejected by MySQL itself, even if both requests concern the same SOP.
Debug `failureAfterSnapshotCreationRollsBackTheWholeTransaction` to see that a later failure
removes the snapshot and document created in that transaction. Auto-increment IDs may have gaps
after rollback; IDs are identities, not version numbers or chronological history positions.

Other tests cover exact Unicode/newline preservation, snapshots independent of mutable copies,
stale/cancelled copy rejection, duplicate capture prevention, foreign keys, history ordering-key
uniqueness and official-guide keys. These verify storage prerequisites for A12 and A26-A31, not
complete publication, participant inheritance, notifications or concurrent workflow execution.

The workspace ships with four local tutorial SOPs sourced from `docs/tutorials/workflow-guides.json`.
The demo initializer preserves their released revisions and appends new ones when content changes.

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

### Transactional workflow exercises

Run these classes with the integration configuration, the gutter Run/Debug action, or:

```powershell
./scripts/dev.ps1 test-backend -Test LifecycleWorkflowIntegrationTest
./scripts/dev.ps1 test-backend -Test WorkflowAtomicityIntegrationTest
```

The first exercises authenticated internal commands: submission, reviewer replacement, participant
inheritance, independent/self approval, exact-candidate validation, configured-client scope,
ownership/role changes, duplicate commands and durable audit/outbox recipients. It does not use
HTTP controllers or an email provider. Debug `ownerEditsPreserveAuthorAndRerouteToManagerOrAdmin`
to inspect why the editor loses their normal approval path.

The second uses committed test-only fixtures and separate concurrent connections. It verifies
one winner when publication races replacement, and one publication for concurrent retries of
the same command. It also injects an outbox INSERT failure after other writes and verifies that
publication/replacement state, audit and retry records roll back. Debug the two outbox-failure
tests for A12/A31; avoid breakpoints in race workers unless you extend their bounded timeouts.
Its cleanup removes only its fixture document and configuration in the isolated test schema;
it does not disable foreign keys or touch development data. An interrupted test process may
leave those fixtures behind; inspect them rather than bypassing the configuration uniqueness check.

These tests cover internal service behavior for A12, A14, A20, A24-A25 and A27-A31. V105's API
tests add reviewer-copy creation/editing, API visibility and rejection/cancellation. The atomicity
suite also checks rejection rollback after outbox failure and content/version rollback after
save-audit failure. V106 adds reassignment coverage; UI visibility and email delivery remain pending.
Tutorial SOP content for these flows remains tied to the future UI release.

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

- Broader workflow combinations beyond the existing permission/concurrency/outbox scenarios.
- Browser end-to-end and accessibility checks using controlled test personas.
- Notification provider fakes, delivery retry scenarios, and client deployment isolation.
- Migration and backup/restore validation before shared-environment changes.

References: [Spring Boot testing](https://docs.spring.io/spring-boot/3.5/reference/testing/spring-boot-applications.html)
and [GitHub workflow artifacts](https://docs.github.com/en/actions/tutorials/store-and-share-data).

## Reader library checks

The browser suite covers descendant filtering, branch-scoped search, persisted code/version labels,
and opening a single current SOP before explicitly requesting history. Navigation regression tests
instantiate the Departments, Subgroups, and Org Hierarchy Report screens and load their initial data.
The API suite checks client-scoped hierarchy, published-only listing/version count, and admin-only,
audited, concurrency-protected code changes. Logs for this increment are under `.local/logs/library-*`.
