# Governed lifecycle API (development contract)

These endpoints are implemented and tested but disabled by default. The current Angular screens
still use the separate MVP endpoints/tables. Enabling the new API does not migrate client data,
replace those screens, deliver email, or disable the old routes. Complete cutover must do all
required routing/permission changes together; do not present the MVP endpoints as governed.

The API uses `/api/lifecycle`, bearer authentication and the explicitly configured client.
`LIFECYCLE_API_ENABLED=true` enables it on backend startup. Client configuration, process owners
and optional reporting lines must be configured first; no seed IDs are interpreted as authority.
Automated API tests enable it only against `sop_gatekeeper_test` with isolated fixtures.
The local development server has not been restarted or reconfigured for this increment.

## Approved access policy

Every authenticated user in the client deployment may read published SOPs and start a draft for
any configured process. This is client-wide access, not a process-membership model. Cross-client
and inconsistent organization/process references are rejected. Product-managed guides cannot
be edited through client workflow commands.

Drafts are private to their author. Only that author edits/submits/cancels the original draft.
Assigned, currently eligible independent reviewers may create and edit their own review copies.
Authors cannot read another reviewer's private working copy; everyone involved reviews the
immutable current candidate after submission. Administrator status alone does not grant draft
override or independent approval. Self-approval eligibility follows the approved participant policy.

Request reads are allowed to the author, currently eligible assigned reviewers, or eligible
participant self-approvers. The response provides available `actions`, including `SELF_APPROVE`
only when eligible. Each mutation independently repeats its checks. Copy reads are limited to
the recorded editor within the configured client; a stale copy can still be inspected by its
editor, but cannot be saved/submitted after candidate replacement or request completion.

## Endpoints

| Method/path (under `/api/lifecycle`) | Purpose |
| --- | --- |
| `GET /documents` | Current published content for this client; excludes unpublished drafts. |
| `POST /documents` | Create a new document, request and author working copy; no publication. |
| `POST /documents/{documentId}/drafts` | Copy the expected published revision into a new request authored by the caller. |
| `GET /requests/{requestId}` | Authorized request state, version, current candidate and available actions. |
| `GET /requests/{requestId}/copies/{copyId}` | Editor's private working-copy content, state and version. |
| `PUT /requests/{requestId}/copies/{copyId}` | Save the caller's editable copy with expected request/copy versions. |
| `POST /requests/{requestId}/reviewer-copies` | Assigned reviewer copies the current candidate without changing its review status. |
| `POST /requests/{requestId}/submit` | Freeze original draft or atomically replace the review candidate with the reviewer's copy. |
| `POST /requests/{requestId}/approve` | Independently approve or explicitly self-approve and publish. |
| `POST /requests/{requestId}/reject` | Assigned independent reviewer rejects the exact candidate with a reason. |
| `POST /requests/{requestId}/cancel` | Author cancels an unchanged DRAFT, preserving its content and audit. |

Mutation bodies carry a client-generated UUID `commandId`. Retry an uncertain request with the
same ID and identical body. Do not reuse it for changed input or another actor. Responses to
retries describe the original operation, not necessarily the resource's latest state; GET the
request/copy before further edits. New draft/copy/save results contain `requestId`, `copyId`,
and copy `version`. Candidate transitions return `candidateId`; cancellation returns request
`version`. Successful operations currently return HTTP 200.

Create body:

```json
{
  "processId": 1,
  "title": "Procedure title",
  "description": "Purpose",
  "details": "Steps",
  "commandId": "<new UUID>"
}
```

Save requires `requestVersion`, `copyVersion`, `title`, `description`, `details`, `commandId`.
Starting a revision requires `publishedRevisionId`, `commandId`. Submission requires `copyId`,
`requestVersion`, `copyVersion`, `commandId` and a nonblank `reason` for reviewer replacement.
Suggest/reject require `candidateId`, `requestVersion`, `commandId`; rejection also needs a
nonblank `reason`. Approval adds `mode` (`NORMAL` or `SELF_APPROVAL`), with a nonblank `reason`
for self-approval. Cancellation needs `requestVersion`, `commandId`.

Draft text may be incomplete; submission requires nonblank title, description and details.
Title length is limited to 255 characters. Text is opaque content, never executable HTML;
the future UI must render it safely. No actor/role/participant identity is accepted from a body.

`400` indicates invalid input, `403` denied access, `404` a missing workflow record/configuration,
and `409` a state/version/routing or persistence conflict. Persistence errors do not disclose
SQL, table names, credentials or SOP bodies. An absent feature flag produces no controller route.

## Transaction behavior and limitations

V105 adds typed command results, draft/copy audit references and rejection decisions. A database
constraint prevents a rejection decision from being used as publication evidence. Existing
approval records retain their APPROVED meaning. Previous Flyway scripts remain unchanged.

Create, save, suggest, submit, approve, reject and cancel all record retry results and audit in
their transactions. Save/create/cancel do not send governance notifications. Rejection queues
the author's notification and closes editable reviewer copies, preserving the candidate.
Opening a copy does not increment the request version or prevent publication; saving/replacing
after a winner changes the request returns a conflict. Self-approval remains a separate explicit
mode, never inferred from the actor's role.

Revising rejected work as a linked new attempt, audited reassignment, governance administration,
reporting-line cycle validation, workflow inbox/list pagination, arbitrary version comparisons,
email/in-app delivery workers, tutorial seed replacement and UI integration remain future work.
The published list is an initial unpaginated contract; add pagination before client-scale rollout.
New-SOP creation serializes by process; transitions serialize by document/request. Publication
and replacement races are tested, but a complete load/deadlock retry strategy remains release work.

## Run and inspect the tests

```powershell
./scripts/dev.ps1 test-backend -Test LifecycleApiTest
./scripts/dev.ps1 test-backend -Test WorkflowAtomicityIntegrationTest
```

In IntelliJ, run/debug the same classes. Start with
`authorReviewerEditorAndManagerCompleteWorkflowThroughHttp`: it uses real JWT authentication,
HTTP validation/authorization and MySQL transactions to create, submit, privately edit, replace
and publish. The test also verifies that client-supplied identity cannot grant draft access and
that ordinary authors do not receive self-approval actions (in separate cases).

Tutorial impact: Creating/submitting an SOP, Reviewing/suggesting edits, Approving/self-approving,
and Rejecting/cancelling need official guides when the corresponding UI is delivered. Backend
API tests are not a substitute for verifying those guides against actual user screens.
