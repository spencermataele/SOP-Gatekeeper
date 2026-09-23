# Lifecycle data model and migration plan

Status: implementation design; seed replacement/tutorial policy approved, September 17, 2026.

This implements the direction in [SOP-LIFECYCLE-SPEC.md](SOP-LIFECYCLE-SPEC.md).
The additive schema increments are implemented in V103-V106 and verified in the isolated test
database. The MVP tables and application behavior remain intact. Data conversion and cutover
remain future work; the preservation decisions below apply before changing client records.

## Implemented foundation

### Rejected attempts and reassignment (V106)

Rejected work now creates a new author-owned draft linked to the exact rejected request/revision.
Initial submission copies participant evidence from that source; subsequent reviewer replacement
continues to inherit it. Rejected snapshots/decisions are never reopened or overwritten. A changed
publication base blocks automatic resubmission until explicit reconciliation is implemented.

Administrators can refresh stale IN_REVIEW routing with a reason. The service calculates current
eligible reviewers, archives old assignments, stores before/after routing fingerprints and the new
owner/manager snapshot, increments the request version, and writes audit/outbox records atomically.
This is recovery of invalidated routing, not arbitrary reviewer delegation. Old reviewer copies
remain readable to their editor but cannot be submitted unless current eligibility/version checks pass.
The new API actions, recovery visibility and remaining limitations are in LIFECYCLE-API.md.

### Editing and API increment (V105)

The architect approved client-wide published access and draft creation for any process,
with author-only editing and private reviewer copies. Create/new revision, optimistic copy saves,
assigned-reviewer copy creation, rejection and cancellation commands are now implemented with
audit and replay protection. Rejection closes editable copies, retains candidate/decision evidence
and queues the author's notification; cancellation applies only to an author's DRAFT.

The authenticated, default-disabled `/api/lifecycle` controller exposes these operations plus
submission/approval and authorized reads. See [LIFECYCLE-API.md](LIFECYCLE-API.md) for the contract,
access boundaries, feature flag and remaining cutover work. Existing UI/MVP endpoints remain
separate. Explicit client/owner setup is still required; no deployment data has been converted.

### Transactional workflow increment (V104)

`LifecycleWorkflowService` now implements internal submission, assigned-reviewer candidate
replacement and atomic approve-and-publish commands. Actor identity comes from Spring Security;
authority comes from current database users, explicit process ownership and direct-manager data.
The new client configuration is a required singleton. No default client/owner mapping is seeded.
An absent mapping, mismatched client/process, or product-managed guide blocks these commands.

Each command locks the document before the request, checks expected versions and candidate,
and records its fingerprint/result. Same-actor/same-input retries return the stored result;
changed input or another actor cannot reuse that command. Replay uses a locking/current read
so simultaneous retries see the committed result even under MySQL repeatable-read isolation.

Submission freezes the copy, captures author/editor/submitter identities, inherits participants
on replacement, and creates candidate-specific assignments. It never carries approval authority
forward. Publication checks the base revision and writes approval, history position, current
pointer, request completion, audit, queued notification recipients and retry result atomically.

Assignments carry a fingerprint of owner, owner-assignment version, manager and administrator
membership. Changes to those facts conservatively block pending commands until an administrator
performs V106's audited reassignment. This includes an administrator added to the directory; the implementation
does not silently reinterpret an existing assignment. Shared locks keep those facts stable during
the transaction. Future governance administration must validate reporting-line cycles and use
compatible locking. Direct SQL configuration is for fixtures, not the future administrator UI.

V104 supports approved publication evidence only. Rejection decisions, imported attestations,
delivery attempts/provider callbacks, notification workers and report endpoints remain future
migrations/features. Outbox recipients are deduplicated by event/user/channel for IN_APP and EMAIL;
QUEUED is persistence, not evidence of availability in the old UI or actual email delivery.

At the V104 stage, no HTTP endpoint was exposed. V105 adds default-disabled endpoints and the
draft/edit/reject/cancel operations described above; the old MVP workflow is not redirected yet.
V106 adds audited reassignment; the UI still needs implementation. The V104 tests create working-copy
fixtures directly; V105 API tests exercise creation/editing through real authenticated requests.

### Storage increment (V103)

V103 adds documents, work items, working copies, revisions, participants and history positions.
Composite foreign keys constrain current/base revisions to their document, and candidates/copy
sources to their request. Check constraints validate states and product-guide identity. No MVP
seed replacement, client cutover, notification worker or workflow endpoint is enabled.

`RevisionSnapshotStore` only appends authored snapshots and reads them by document/revision.
It copies text in a single SQL statement conditioned on the working-copy version and editable
state. One working copy can produce at most one snapshot. Its content checksum is SHA-256 of
MySQL's UTF-8 serialization of JSON_ARRAY(title, description, details, content_schema_version).
This checksum is an integrity aid, not a digital signature or proof of approval.

Snapshot immutability currently means the new storage API has no update/delete operations and
editing a working copy leaves its stored snapshot untouched. Direct SQL with the development
account can still modify these tables. Production database grants and workflow write controls
remain necessary. The storage API is an internal primitive, not an authorized submission command:
the V104 workflow service captures participants, marks the copy submitted, routes the candidate,
and writes audit/outbox records together. Do not expose this repository as a controller.

Flyway will apply V103-V106 on the next normal backend startup wherever this build is run. The running
development server has not been restarted as part of this increment. Rehearsal against a fresh
database, conversion validation and shared-environment release remain separate from the verified
upgrade of the existing isolated test schema.

## What changes and why

The MVP uses a new `sop` row for every version. Content, current status, owner information,
and organizational metadata are mixed in that row. `change_approval` identifies a request,
but does not identify an immutable candidate. New SOP creation publishes immediately.
The owner catalog is separate from `users`, with no foreign key proving that equal IDs
represent the same person. There is no direct-manager relationship.

The replacement separates a document's identity, editable work, submitted content, and
publication evidence. An approval always identifies the exact immutable content reviewed.
Process ownership becomes an explicit relationship to an authenticated user account.

## Proposed records

Names below are proposed new tables; existing tables are retained during migration.
Use generated BIGINT identities for new records, UTC timestamps, and foreign keys with
restricted deletion for business history. Existing user/process IDs keep their current types.

| Record | Main fields and purpose |
| --- | --- |
| `client_configuration` | Singleton deployment identity, selected organization, configuration version. Validates one client per deployment; no client-database routing. |
| `process_governance` | Process ID, owner user ID, assignment version. One accountable owner per process. Preserve assignment changes in audit. |
| `user_reporting_line` | User ID, direct manager user ID, version. Reject self-links and cycles. Missing manager is explicit, not inferred from position codes. |
| `sop_document` | Stable document ID, organization, process, current revision ID (nullable), optimistic version. Title is versioned content. |
| `sop_work_item` | Request identity, document, original author, current submitting actor, base published revision (nullable for a new SOP), current candidate ID, state, version, prior rejected request ID when applicable. |
| `sop_working_copy` | Work item, editor, source candidate (nullable), content, content schema version, optimistic version, state. Mutable until submitted/cancelled; reviewer copies do not change the current candidate. |
| `sop_revision` | Immutable content snapshot, document, work item (nullable for intake), source working copy, predecessor candidate, submitter, recorded time, supplied effective date, source label, provenance, content checksum. |
| `revision_participant` | Revision, user, contribution type; source evidence. Inherit candidate-lineage participants and add authenticated author/editor/submitter. |
| `review_assignment` | Candidate, reviewer, routing authority, owner/manager/role snapshot and assignment generation. Revocation/reassignment is recorded and audited. |
| `approval_decision` | Candidate, assignment when applicable, actor, decision, authority, self-approval flag, reason, timestamp, command identity. Append-only decision evidence. |
| `publication_record` | Document, revision, prior published revision, decision or intake attestation, publication time and provenance. Distinguishes new approval, attestation, and legacy continuity. |
| `revision_history_position` | Document, revision, exact ordering key; unique document/revision and document/key. Only published, attested, or imported historical versions enter the official timeline. |
| `business_audit_event` | Actor, action, document/request/candidate, before/after references, reason, correlation and timestamp. Append-only business evidence, separate from diagnostic logs. |
| `workflow_command` | Unique command ID, actor, request fingerprint, resulting references and outcome; transactional retry protection. |
| `notification_event` | Durable outbox event with business-event identity, candidate reference, minimal versioned payload and processing lease/retry metadata. Created in the business transaction. |
| `notification_recipient` | Event, intended user and channel; unique event/user/channel. Snapshot routing recipients in the business transaction. |
| `notification_delivery` / `notification_attempt` | Recipient delivery state plus individual attempt records, provider reference, bounded retries and safe failure summaries. Read timestamp is separate. |
| `migration_record_map` / `migration_issue` | Migration batch, legacy table/ID, target references, source checksum, resolution evidence and unresolved issues. Preserve traceability and support restartable conversion. |

Do not duplicate submitted content into a separate candidate-content table: a candidate is
a `sop_revision` referenced by a work item. Whether it is current, superseded, rejected, or
published is established by workflow and publication records. Content is never overwritten
to change its display state. A working copy is not a published historical version.

Structured content initially preserves the MVP title, description and details exactly.
Use a versioned content representation; later templates can add stable section IDs. Do not
pretend a legacy free-text document already contains reliably identified sections. Retain
original metadata in the migration archive even when authoritative process mappings change.

Imported file references, attestation records, template definitions and subscription
entitlements will be added in later additive migrations. Reserve provenance and schema-version
fields now; do not add pretend billing or email-provider integrations. File bytes belong in
private client storage, with database metadata and authorized download access.

## Integrity and authorization boundaries

- The document's current revision must belong to that document. Enforce composite foreign
  keys such as `(document_id, revision_id)`, not only a foreign key to any revision.
- Use analogous composite keys for a request's candidate and an assignment/decision's
  candidate. A candidate must belong to the request it is replacing; reject cross-request copies.
- New/revised workflow rows reference valid users and a governed process. The configured
  organization must match the process hierarchy. Missing or conflicting legacy mappings block
  activation of that document, not silent guessing.
- Only `sop_document.current_revision_id` determines the current version. There are no
  independently editable active flags on every revision. Publication history remains immutable.
- Track participants within the change attempt and successor candidates. Do not mark every
  author of every earlier published revision as a participant in all future change requests.
  Conversely, a resubmission of rejected work must preserve its actual contributor evidence.
- Resolve actors from authentication, roles from authoritative current data, and process
  ownership from the process assignment. Client-supplied participant/approver IDs cannot grant access.
- Owner and manager snapshots explain routing, but current eligibility is rechecked before
  editing, replacing a candidate or deciding. Invalidated assignments require audited reassignment.
- Hard deletion of referenced users or processes is restricted. Deactivation must preserve
  history and revoke authority. Business history is not editable through generic CRUD endpoints.
- Database constraints establish structural integrity; service authorization and transaction
  checks establish workflow correctness. An append-only Java API alone is not protection
  against a database administrator. Production grants and backup controls need separate validation.

General published read/draft-create visibility is now confirmed as client-wide. Private draft
and reviewer-copy access follows the author/editor rules; the API checks these independently
of UI action visibility. Broader collaboration and administrative draft override remain deferred.

## Transaction and concurrency design

Every command carries a unique command ID and expected versions. Persist the command result
with a request fingerprint and actor in the same transaction. A retry by the same actor with
the same command/payload returns the prior result; reusing the ID for different input conflicts.
Recheck read authorization before returning saved results. Database uniqueness handles races.

For publication and candidate replacement, acquire locks in the same order: document,
work item, then working copy/assignment where needed. Recheck expected candidate, base current
revision, request state, participant set and authorization while holding the locks.

Publication atomically writes the decision, publication record, history placement, current
pointer, completed request state, audit and outbox recipients. Any failure rolls all of these
back. No provider call occurs inside this transaction.

Candidate replacement atomically inserts a frozen revision and inherited participants,
updates the current candidate, records the old candidate's supersession relationship,
creates fresh assignments and queues author/reviewer notifications. Prior decisions remain
attached to the old candidate. Merely opening a reviewer copy takes no lasting publication lock.

If publication wins a race, a later replacement fails without reopening the request. If
replacement wins, a decision on the old candidate fails. Two drafts based on the same published
version cannot both silently publish. Draft saves use optimistic versions and return an
explainable conflict instead of last-write-wins behavior.

Normal routing follows the latest candidate's authenticated submitter/editor and the approved
owner/manager/admin rules, excluding all participants from independent approval. If no eligible
independent reviewer remains, expose only a permitted explicit self-approval or a block.
Self-approval is a distinct command requiring a nonblank reason, with recorded authority and
deduplicated owner-manager/admin/designated-reviewer notification recipients.

## Historical ordering

Use an exact `DECIMAL(65,0)` integer ordering key with widely spaced initial positions.
Calculate an integer midpoint for insertion, never floating-point version arithmetic.
When a gap is exhausted, rebalance positions under the document lock while preserving order.
With a unique key, perform rebalance through a reserved temporary range in the same transaction;
detect capacity/range exhaustion before writes and fail atomically. Test the implementation
against real MySQL uniqueness constraints, not only an in-memory list.

The API accepts predecessor/successor revision IDs and expected document version, not raw keys.
Confirm that both belong to the document and are still adjacent; a concurrent insertion returns
a conflict for reconfirmation. Keys are internal and may change; revision IDs and source labels
do not. Effective dates are useful evidence, not an automatic tie-breaker for disputed order.
Historical intake does not change the current publication pointer.

## Preserve existing data

### Approved seed replacement policy (supersedes legacy seed continuity below)

The architect does not require preservation of the eight original MVP sample SOPs. Replace
verified, unmodified samples with product-managed tutorial SOPs in a new migration/release step.
Do not edit V101/V102. Identify original samples by an explicit seed manifest and content/metadata
fingerprints, not IDs or titles alone. A modified sample or one referenced by user work must be
preserved and flagged for review; never cascade-delete client changes or approvals.

The three inconsistencies below do not need individual correction if their records are verified
as unchanged, unreferenced disposable samples. Their replacement guides will use a valid Getting
Started process and explicit product-documentation provenance. No database cleanup has run yet.

Give official guides stable product keys and immutable release revisions. A client-owned copy
retains its source guide reference but has its own lifecycle. Updates to official guides cannot
overwrite copies. Guide content ships alongside supported features and is verified against the UI.
Future schema work must distinguish official read-only guides from client-owned operational SOPs
in both backend write authorization and display; tutorial publication is not a client self-approval.

The preservation rules below continue to apply to user-created/customized records and any
non-seed installation data discovered during migration. LEGACY_CURRENT is a proposed fallback
for that data, not a requirement to retain the disposable MVP samples.

The read-only local inventory on September 17 found 8 SOPs (all ACTIVE), 2 processes and 5 users;
there are no change requests, approvals or notifications in this local database. This is not
an inventory of Railway or any other installation.

The preflight flags process metadata conflicts in SOP IDs **1, 5 and 6**. SOP 1 references
process 2 (How To's) while storing Initial Setup and family 1. IDs 5 and 6 differ in their
stored parent-process metadata (4 and 5 respectively, while process 2 has no parent). Equal owner/user IDs and names exist for all four legacy
owner records, but are only mapping candidates, not proof of identity.

Proposed migration rules:

1. Retain the old tables and a verified backup. Never rewrite V101/V102 or sanitize source
   rows to make conversion pass. Record explicit mapping resolutions in a migration manifest.
2. Establish one configured client organization, explicit owner-to-user mappings and process
   owners. Do not invent direct managers. Multiple organizations or contradictory ownership
   require review before activation; coincident IDs or similar names cannot authorize users.
3. Reconstruct document families from validated predecessor links AND original/proposed request
   links. Detect cycles, disconnected/conflicting links, cross-process/org edges and multiple
   current candidates. Do not group unrelated SOPs by title or decimal-looking version label.
4. Preserve every source row, label, timestamp and content value. Store checksums and a
   source-to-target map. Ambiguous records remain in migration review until resolved.
5. Existing unambiguous ACTIVE content may retain its current status as `LEGACY_CURRENT`,
   clearly labeled as migrated MVP content with unverified approval provenance. This is a
   proposed continuity policy, not a fabricated approval or an import attestation. Require
   owner/admin attestation to promote that provenance later; future changes use the new workflow.
6. DRAFT and CANCELLED records remain unpublished. Preserve rejected attempts. Preserve legacy
   decisions as legacy evidence; because the old system did not freeze candidates, those decisions
   must not be upgraded into verified approval of a newly constructed snapshot.
7. Legacy IN_REVIEW work requires resubmission after participant and assignment review. An
   APPROVED request without consistent publication evidence is a migration issue, not permission
   to publish automatically. Uncertain contributor history cannot authorize normal self-approval.
8. Never replay old notifications as new email. Preserve original in-app notification/read
   records in the legacy archive with links from migration/report views.

The SQL preflight is a diagnostic inventory, not a complete migration validator. The converter
must also validate graph cycles, every row mapping, all workflow states, checksums, timestamps,
unique current pointers and exact record counts using adversarial fixtures.

## Implementation, rehearsal and rollback

1. Review this design and the legacy continuity policy. Resolve local mapping conflicts explicitly.
2. Add new tables through new Flyway migrations. Prototype and verify only in the isolated test
   schema first. Keep the new workflow unavailable until conversion and authorization are ready.
3. Build a restartable converter with dry-run reports and a reviewed mapping manifest. Use a
   migration batch identity plus unique source-map keys; verify source checksums on restart.
4. Rehearse on a separate restored database. Test both fresh schema creation and upgrade from
   V101/V102, with legacy draft, rejected, in-review, conflicting and cyclic fixtures. Validate
   counts/checksums, untouched old tables, and rollback with injected conversion failures.
5. Before any shared-environment cutover, stop old writers, take and test a consistent backup,
   run conversion/validation, and review the report. No background dual-write period.
6. Enable the new services and UI together. Disable or replace old create/update/publish routes
   so they cannot bypass the governed workflow. Keep authorized legacy archive access available.

Before the new workflow accepts writes, rollback can restore the old application against the
untouched old tables after verification. MySQL DDL is not undone by rolling back a conversion
transaction; failed schema changes need a tested forward repair or backup restore.

After new writes are accepted, switching back to old tables would hide new work. Stop writes,
preserve the full new database, and choose a tested forward fix or an explicitly reviewed
reconciliation/recovery plan. Do not silently restore an older backup and discard new submissions.
Retiring old tables and selecting retention periods are later approvals.

## Verification and architect walkthrough

Run the diagnostic from the IntelliJ PowerShell terminal at the repository root:

```powershell
./scripts/migration-preflight.ps1
```

It targets only local `woven` on port 3307, uses the existing local settings without printing
credentials, executes a read-only consistent-snapshot transaction, and rolls it back.
It prints counts and conflicting IDs, without SOP bodies or email addresses. Compare the three
flagged SOPs with the process assignments in the UI; do not edit them just to silence the report.

Upcoming executable acceptance coverage:

| Area | Required evidence |
| --- | --- |
| Migration | Old rows unchanged; exact content round-trip; deterministic maps; ambiguous records blocked; restart/rehearsal/restore verified. |
| Integrity | Cross-document pointers rejected; missing users/process owners blocked; submitted content cannot be edited. |
| Routing | Owner/manager/admin combinations, participant lineage, role changes and explicit self-approval (A01-A06, A20, A23-A25, A29). |
| Transactions | Actual concurrent MySQL connections for publication/replacement races, idempotent retries and rollback injection (A12, A30-A31). |
| Review copies | Frozen original, successor selection, exact-candidate decisions and author notifications (A26-A29). |
| Ordering | Repeated insertion, gap exhaustion/rebalance, stale neighbors and unchanged current pointer (A16-A17). |
| Outbox | Commit/rollback coupling, recipient deduplication, worker crash/retry and provider-state distinction (A13-A14). |

These are planned tests, not claims that the lifecycle is implemented. The preflight has been
executed successfully against local development data. No schema or business records were changed.

## Decisions for this review

- Approve the identity/working-copy/immutable-revision model and additive migration sequence.
- Known unmodified and unreferenced MVP samples may be replaced by official guides (approved).
  Confirm owner-account mappings before converting any user-created content.
- If customized/non-seed records are discovered, review the LEGACY_CURRENT fallback and
  resubmission plan for that actual data before cutover.

No additional provider, pricing or document-format choices are needed for this increment.
