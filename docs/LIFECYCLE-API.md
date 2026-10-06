# Governed lifecycle API (development contract)

These endpoints are implemented and tested and enabled by the local `backend-lifecycle` command.
The development Angular workspace uses them. The production API flag still defaults to off and
the production frontend keeps its previous routes until a coordinated cutover. Enabling the API
disables the legacy SOP/change-request controllers; it does not migrate MVP content or send email.

The API uses `/api/lifecycle`, bearer authentication and the explicitly configured client.
`LIFECYCLE_API_ENABLED=true` enables it on backend startup. Client configuration, process owners
and optional reporting lines must be configured first; no seed IDs are interpreted as authority.
Automated API tests enable it only against `sop_gatekeeper_test` with isolated fixtures.
Use the [local walkthrough](WORKFLOW-WALKTHROUGH.md) for the practice configuration and UI scenarios.

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
| `GET /processes` | Configured client processes and owner names for draft creation. |
| `GET /requests` | Latest authorized work summaries; current permission checks filter candidates. |
| `GET /notifications` | Latest 100 in-app notification records addressed to the caller. |
| `GET /documents/{documentId}/history` | Published revision history in its stored order; no unpublished candidates. |
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
| `POST /requests/{requestId}/revise` | Original author creates a linked new draft from a rejected candidate, retaining contributor evidence. |
| `POST /requests/{requestId}/reassign` | Administrator refreshes stale routing using current governance, with a mandatory reason. |

Mutation bodies carry a client-generated UUID `commandId`. Retry an uncertain request with the
same ID and identical body. Do not reuse it for changed input or another actor. Responses to
retries describe the original operation, not necessarily the resource's latest state; GET the
request/copy before further edits. New draft/copy/save results contain `requestId`, `copyId`,
and copy `version`. Candidate transitions return `candidateId`; cancellation returns request
`version`. Successful operations currently return HTTP 200.

Revise uses `requestVersion`, `commandId` and returns the new request/copy. Reassign uses
`candidateId`, `requestVersion`, `reason`, `commandId` and returns the unchanged candidate ID;
GET the request for its incremented version. Callers cannot supply replacement reviewer IDs.

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

V106 links new attempts to their rejected source request and revision. Only the original author
can revise. Its first submission inherits every source participant; changing the submitter cannot
turn an earlier contributor into an independent reviewer. The old request, candidate and decision
remain intact. If publication has changed since the rejected attempt's base, automatic resubmission
is blocked pending an explicit reconciliation workflow rather than silently adopting the new base.

Reassignment is limited to administrators and an IN_REVIEW request with changed routing. It
archives prior assignments, stores old/new routing fingerprints and the new owner/manager snapshot,
records the reason, and queues notifications to new reviewers and the author. Participants remain
excluded from independent approval. Administrators may read a submitted request with stale routing
to perform this recovery; this does not grant access to another author's private draft/copy or
permission to approve the candidate. It exposes REASSIGN, not an automatic approval action.
No-op reassignment and reassignment of closed work are rejected. All changes roll back together
if audit/outbox persistence fails; same-command retries do not create extra history or notifications.

Governance administration, explicit reconciliation against a newer publication,
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

## Standard template and administration

`details` remains a string in API envelopes, containing JSON for `template: "gatekeeper-sop"`,
`schemaVersion: 2`, text `scope` and `references`, and `steps` with stable UUID `id` plus text
`who`, `what`, `where`, and `notes`. Optional `subgroupId` must belong to the process and department.
Drafts can be incomplete. Submission requires title, purpose, and 1–200 complete Who/What/Where
steps. All text fields in template content are limited to 20,000 characters. Legacy text can be
saved but must be converted before submission; existing published snapshots remain unchanged.
The server replaces `context` with authoritative hierarchy/owner names at submission.

`GET /processes` now returns hierarchy IDs/names; `GET /subgroups` returns process-linked options.
`GET /admin/setup` returns configured organization, users, processes, and the latest 100 configuration
audit entries. `PUT /admin/client` accepts `orgId` and a required `reason`, establishing the client
once. `PUT /admin/processes/{id}/ownership` accepts `ownerId`, nullable `managerId`, nullable
`expectedVersion` (null for first assignment), nullable `expectedManagerId` (the selected owner's
current reporting line), and required `reason`. Stale values return 409 without partial changes.
These configuration mutations use expected-state checks rather than workflow command IDs; after
an uncertain response, reload setup before retrying. They are admin-only on both client and server.
The old `/admin/**` and `/process-owners/**` endpoints also require the ADMIN role.

V107 adds `governance_configuration_audit`. Ownership/reporting changes and their actor, reason,
before/after values, and time commit together. Changing a reporting line affects every process
owned by that person. Existing in-review work requires explicit routing recalculation if stale.
