# SOP Gatekeeper

SOP Gatekeeper manages standard operating procedures, their process ownership, and changes
that require review. The product is being developed into an organization's authoritative
source for published procedures and their revision history.

## Current application

The MVP includes authentication, organizational hierarchy and user administration, business
processes and owners, SOP authoring, change requests, approval/publication, in-app notifications,
and an organizational hierarchy report.

The MVP does not yet implement all approved governance rules. New SOPs currently publish
immediately, authorization needs strengthening, and automated tests cover only a small part
of the workflow. The [lifecycle specification](docs/SOP-LIFECYCLE-SPEC.md) defines target behavior
and acceptance scenarios. It distinguishes confirmed decisions from remaining proposals;
it is not a description of already implemented functionality.

## Architecture

| Component | Implementation |
| --- | --- |
| Browser application | Angular 14 and TypeScript |
| API | Java 21 and Spring Boot 3.5.5 |
| Persistence | MySQL, Spring Data JPA, and Flyway migrations |
| Authentication | Spring Security, JWT, and BCrypt password hashing |
| Build | Maven wrapper; repository-pinned Node 18.19.0 and npm 9.6.7 |

Backend requests flow through controllers, services, and repositories. The frontend runs
separately during local development and calls the backend at `http://localhost:8080`.
The internal Maven project name is `woven1`, and the database name is `woven`.

The approved hosting direction is a separate application deployment, database, and private
file storage with separate credentials for each client. All deployments share one codebase
and release process. Automated client provisioning is future work.

## Repository layout

```text
backend/          Spring Boot application, migrations, and Java tests
frontend/         Angular application and browser tests
scripts/dev.ps1   Local development commands for Windows PowerShell
docs/             Product rules and architectural decisions
DEVELOPMENT.md    Tool setup, startup instructions, and verified baseline
compose.yaml      Existing container configuration; deployment validation pending
```

## Local development

For a fresh checkout, follow [DEVELOPMENT.md](DEVELOPMENT.md) to install the project-local
Java and MySQL tools. The build command downloads Maven, Node/npm, and application dependencies.
The commands below assume that tool setup has been completed.

From the repository root, start the database:

```powershell
./scripts/dev.ps1 database
```

Wait for `ready for connections` in `.local/logs/mysql.err.log`. Then start the API and frontend
in separate terminals:

```powershell
./scripts/dev.ps1 backend
```

```powershell
./scripts/dev.ps1 frontend
```

Open [the application](http://localhost:4200). Check [API health](http://localhost:8080/actuator/health).
MySQL listens on `127.0.0.1:3307`. Flyway applies migrations and seed data during backend startup.
Local credentials and data live under ignored `.local/`; do not commit them. See DEVELOPMENT.md
for the existing workspace's local login and fresh-database login requirements.

Stop the API and frontend with Ctrl+C in their terminals, then stop MySQL:

```powershell
./scripts/dev.ps1 stop-database
```

### IntelliJ IDEA

Open the repository's root `pom.xml` as a Maven project. Configure its project SDK and Maven
runner JDK to use Java 21 (the local installation is under `.local/tools/jdk-*`). The scripts
can be run from IntelliJ's PowerShell terminal. Shared **Backend unit tests** and **Backend
integration tests** configurations are provided in `.run/`. See the [testing guide](docs/TESTING.md)
for setup, debugging exercises, and report locations.

## Build and verification

```powershell
./scripts/dev.ps1 build
./scripts/dev.ps1 prepare-tests
./scripts/dev.ps1 test-unit
./scripts/dev.ps1 test-backend
./scripts/dev.ps1 test-frontend
```

`build` packages the application with tests skipped. Run both test commands separately.
Start local MySQL before `prepare-tests`, which provisions a separate test schema and restricted
test account. Database-backed tests never fall back to application database credentials.
Frontend tests use headless Chrome; set `CHROME_BIN` if Chrome is installed outside the default path.

The testing-foundation verification results are:

- Backend tests and package: passed; 11 tests cover authentication, database isolation,
  department relationships, and request logging.
- Frontend: 4 tests passed with a generated coverage report.
- Isolation check: inherited Flyway connection settings cannot redirect the test database.

These results do not mean all product workflows are tested. The inherited frontend failure was
replaced with valid parent/child contract and error-propagation checks; real database tests verify
parent enforcement. See [TESTING.md](docs/TESTING.md) for details. Initial dependency audit findings
remain recorded in [DEVELOPMENT.md](DEVELOPMENT.md).

## Development direction

The [specification](docs/SOP-LIFECYCLE-SPEC.md) defines approval routing, explicit self-approval
exceptions, immutable published revisions, historical ordering, side-by-side comparison, durable
notifications, and audit/report visibility. Word and Google Docs intake and subscription foundations
are planned features, not currently available capabilities.

The proposed [data model and migration plan](docs/LIFECYCLE-DATA-MODEL.md) describes the next
increment, preservation of MVP records, concurrency rules, and architect review decisions.
Run `./scripts/migration-preflight.ps1` for a read-only inventory of the local development database.

The lifecycle foundation includes additive V103-V105 migrations, draft/reviewer-copy editing,
submit/replace/approve/reject/cancel services, participant tracking, audit and queued notifications.
The backend suite passes 81 tests, including authenticated HTTP workflows and concurrent MySQL
transactions. The new [lifecycle API](docs/LIFECYCLE-API.md) is disabled by default pending UI/configuration
cutover. The existing MVP screens/data remain unchanged. Notification delivery, tutorial seed
replacement, rejected-work resubmission and reassignment remain subsequent increments.

Changes should include relevant acceptance scenarios, automated verification, and a short manual
walkthrough for architectural review. GitHub Actions runs component builds/tests and preserves
reports on pushes and pull requests. The workflow still needs a hosted run after pushing, and
required-check branch rules must be configured separately. Full lifecycle permissions, browser
end-to-end scenarios, and accessibility checks will grow with those features.

## Deployment status

The repository contains Docker and Railway-related configuration, but the packaged deployment
needs further verification. Known issues include frontend build inclusion and the path used to
copy Angular output into the backend JAR. Production frontend configuration currently contains
the existing Railway API address. Do not treat the local build result as deployment readiness.

Deployment automation, per-client configuration, backups/restoration, and controlled releases
will be established before a client pilot. See the specification for the agreed isolation model.
