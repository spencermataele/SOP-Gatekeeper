# SOP Gatekeeper local development

## Workspace

Repository: https://github.com/spencermataele/SOP-Gatekeeper

Initial baseline: `working_branch`, commit `b85cad10249993ebc3a855ff19fef27774c3d24b`.
The application still uses the internal Maven name `woven1` and database name `woven`.

| Component | Local toolchain |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5.5 |
| Build | Maven 3.9.11 through `backend/mvnw.cmd` |
| Frontend | Angular 14, repository-pinned Node 18.19.0 and npm 9.6.7 |
| Database | MySQL 8.4.8, project-local instance |

Tools, generated credentials, logs, Maven cache, and database files live in ignored `.local/`.
Maven installs Node into ignored `frontend/node/`. No system PATH change is required.

## Start development

Run from this repository in PowerShell. Start the database once:

```powershell
./scripts/dev.ps1 database
```

Wait for `ready for connections` in `.local/logs/mysql.err.log`, then open two terminals:

```powershell
./scripts/dev.ps1 backend-lifecycle
```

```powershell
./scripts/dev.ps1 frontend
```

Open http://localhost:4200. Backend health: http://localhost:8080/actuator/health.
The database listens only on `127.0.0.1:3307`; the backend listens on `127.0.0.1:8080`.
Flyway creates the schema and MVP seed records on the first backend startup. The governed UI
uses its separate lifecycle tables. After startup, run `./scripts/prepare-workflow-demo.ps1`
to initialize local practice accounts, process ownership, and tutorial SOPs. See
[the walkthrough](docs/WORKFLOW-WALKTHROUGH.md). Use `backend` only for the previous MVP mode;
the development frontend expects `backend-lifecycle`.
The existing seeded Admin password must come from the original administrator guide;
its hash in the migration cannot be used as a login password.
For this workspace, a separate `LocalDeveloper` administrator was created in the local
database. Its generated login password is in `.local/login.json` (ignored by Git).
This account does not exist in the deployed application.

The script supplies database credentials and a generated JWT secret from `.local/settings.json`.
It does not load the repository's tracked `.env` file. Development Angular builds use
localhost; production builds retain the existing Railway URL.

Stop foreground servers with Ctrl+C, then stop the database:

```powershell
./scripts/dev.ps1 stop-database
```

Database files persist between runs. Do not delete `.local/mysql-data` to resolve routine startup errors.

## Verification

```powershell
./scripts/dev.ps1 build
./scripts/dev.ps1 prepare-tests
./scripts/dev.ps1 test-unit
./scripts/dev.ps1 test-backend
./scripts/dev.ps1 test-frontend
```

`build` packages the application with tests skipped; run the test actions separately.
Start local MySQL before `prepare-tests`. It creates a separate `sop_gatekeeper_test` schema with
its own restricted account. Backend integration tests use that schema, not `woven`; unit tests
need no database. See [TESTING.md](docs/TESTING.md) for IntelliJ debugging and report locations.
Frontend tests use installed Chrome in headless mode;
set `CHROME_BIN` first if Chrome is installed elsewhere.

## Recreating the local tools

Extract the official [Temurin Java 21 Windows x64 JDK](https://adoptium.net/temurin/releases/?version=21)
and [MySQL 8.4 Windows x64 ZIP](https://dev.mysql.com/downloads/mysql/8.4.html)
under `.local/tools/`, keeping their `jdk-*` and `mysql-*` top-level directories.
Create `.local/logs/`, then run the build action to obtain Maven, Node, npm, and dependencies.
The first build needs network access. The database action initializes a fresh local database
and generates its credentials automatically.

## Initial engineering discussion

### Original setup baseline — September 16, 2026

- Full Maven reactor production build: passed; tests run separately below.
- Backend tests: 3 passed, including application startup against local MySQL.
- Frontend ChromeHeadless tests: 2 passed, 1 failed (inherited null-subgroups expectation below).
- Angular development server: compiled successfully; HTTP 200 at localhost:4200.
- Backend health: UP. Local administrator login, current-user lookup, and authenticated
  retrieval of 8 seeded SOPs succeeded. CORS accepts http://localhost:4200.
- Dependency installation reported 71 npm audit findings (7 low, 27 moderate, 35 high,
  2 critical), across the installed dependency tree. No automatic dependency upgrades applied.
- Full build and test logs are in `.local/logs/`; the completed browser test run is
  `frontend-tests-approved.log`. The first sandboxed browser test attempt was blocked
  from spawning child processes; the rerun with process permissions reached the test assertions.

The app has been verified as separate local frontend/backend services. This does not
establish that the packaged JAR correctly serves the SPA or that all business workflows pass.

### Testing foundation update

Backend verification now passes 11 tests and packages successfully. The frontend suite now
passes 4 tests with HTML coverage. A separate test schema/account protects development data;
the isolation tests also pass with a conflicting inherited Flyway URL. Reports and runnable
learning exercises are documented in [TESTING.md](docs/TESTING.md). The original null-subgroups
test was replaced by parent/child contract and error-propagation checks, backed by real-MySQL
relationship tests. CI is configured but requires a pushed branch and hosted run for validation.

### Decisions to make

- Confirm the target users and first complete SOP workflow to improve.
- Agree on the next release's acceptance criteria and deployment target.
- Review the inherited test expectations, authentication configuration, and deployment packaging.
- Plan dependency upgrades as a separate change from reproducing the MVP.

Initial inspection found that the Docker build selects only the backend module, while the
backend copies `frontend/dist` rather than Angular's `frontend/dist/frontend` output.
Deployment packaging needs verification before relying on a single executable JAR or Docker image.
The original frontend test confused child subgroups with the parent relationship. Its replacement
and the backend relationship checks are explained in TESTING.md.
