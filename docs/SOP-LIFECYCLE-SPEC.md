# SOP lifecycle, permissions, and verification specification

Status: **Core architecture decisions confirmed; remaining proposals identified below.**

Prepared: September 16, 2026.

This document records the agreed product direction and proposes precise behavior for the
first implementation milestone. **Confirmed** means explicitly agreed in the architecture
discussion. **Proposed** means an implementation recommendation requiring review. Outstanding
choices are collected in section 10. Existing MVP behavior is not automatically a requirement.

## 1. Product purpose and boundaries

**Confirmed:** SOP Gatekeeper is the authoritative home for an organization's SOPs, revisions,
approvals, and history. Word and Google Docs are intake channels, not continuing sources of truth.
Clients retain ownership of their content and must have usable export paths.

The first milestone establishes one reliable author-to-publication workflow, permission
enforcement, audit history, durable notification events, and repeatable verification.
Document imports, full comparison UX, billing integration, and production email delivery are
subsequent increments. The initial model must accommodate them.

**Confirmed:** Each client has a separate application deployment, database, and private document
storage with client-specific credentials. Maintain one codebase and a common tested release
artifact, deployed with client-specific configuration; do not create client-specific source branches.
An application deployment must not hold credentials for another client's database or storage.
Deployment, migration, backup, restore, and notification configuration must preserve this boundary.
Separate deployments do not imply dedicated physical hardware or immunity to a shared provider outage.
Organization settings and subscription records remain scoped to the client deployment. A shared
application that routes requests among client databases is not the selected architecture.

## 2. Concepts and invariants

| Concept | Meaning |
| --- | --- |
| Organization | Ownership and access boundary for client data; future subscription holder. |
| Business process | Process to which an SOP belongs; identifies its accountable process owner. |
| SOP | Stable identity spanning every revision, with an explicit current-published revision reference. |
| Revision | Content, metadata, original version label, lifecycle state, and provenance for one version. |
| Change request | Review workflow retaining submitted snapshots and identifying exactly one current review candidate. |
| Review candidate | Immutable submitted snapshot; reviewer edits create a successor candidate within the same request. |
| Approval decision | Actor, exact candidate identity, decision, time, comments, and any self-approval exception. |
| Historical position | Ordering within an SOP's history, independent of upload time and version label. |
| Audit event | Durable record of a business action and its actor. |
| Notification delivery | A recipient/channel-specific attempt to communicate a business event. |

Required invariants:

- Every SOP belongs to a business process and an organization. Permissions for a process owner
  apply to the relevant process, not to every SOP in the organization.
- Revision IDs remain stable. A document's original version label is preserved separately.
- At most one revision is the current published revision of an SOP. A new unpublished SOP has none.
- Published content and its approval evidence are immutable. Changes create a new revision.
- New authored SOPs require approval before publication, as do revisions of existing SOPs.
- Historical intake does not move the current-published reference.
- Upload timestamps, supplied historical dates, attestation timestamps, and actual approval
  timestamps are distinct. Never manufacture a Gatekeeper approval for an imported document.
- The authenticated actor comes from the server's security context, not a caller-supplied user ID.
- Client-side controls support usability; the backend enforces every permission and transition.
- Approvals apply only to the exact candidate reviewed. Superseding a candidate preserves its
  decisions as history but never carries their approval authority onto changed content.

## 3. Permission and approval policy

### Confirmed approval routing

| Submitter | Normal reviewer | Explicit self-approval |
| --- | --- | --- |
| Ordinary author | Relevant process owner | Not allowed |
| Process owner's direct manager | Relevant process owner | Not allowed solely because they are the manager |
| Relevant process owner | Their direct manager or an administrator | Allowed |
| Administrator | Relevant process owner | Allowed |

Reporting authority does not replace process expertise. An administrator's submission normally
routes to the process owner; self-approval is an explicit exception, not an automatic consequence
of administrative permissions.

**Confirmed:** A self-approval must be identified in history and logged. Notifications go to
designated approvers, the process owner's direct manager, and organization administrators.
Deduplicate recipients per channel. A manager who also holds an administrator role has the
administrator exception; display and record which authority was used.

**Confirmed:** Require a nonblank reason for self-approval. Treat any submitter, author, or content editor
of the submitted revision as a participant who must use the self-approval exception if eligible;
otherwise they cannot approve their own contribution. Capture participant identities server-side.
This prevents changing the submitter from disguising self-approval.

Only the relevant process owner or an administrator of this client may see or access self-approval
controls and the self-approval workflow. Other users must not receive the workflow as an available
action; direct API attempts are denied even if the UI is bypassed. An owner of a different process
does not qualify. Visibility of the resulting audit record and notifications follows their separate
read permissions; restricting the action does not conceal its occurrence from designated recipients.
Maintain participant IDs with each submitted snapshot; do not let clients erase or replace them.

Either an eligible direct manager or an eligible administrator may approve a process owner's
submission; both approvals are not required. A participant cannot use normal approval to bypass
the self-approval policy. Concurrent decisions must result in only one final publication.

### Proposed action matrix

All actions are organization-scoped. Read access to sensitive process content may require a
finer visibility policy before production; organization membership is not necessarily sufficient.

| Action | Ordinary author | Relevant process owner | Owner's manager | Administrator |
| --- | --- | --- | --- | --- |
| Read published SOPs | Within allowed scope | Within allowed scope | Within allowed scope | Within organization |
| Create SOP or revision draft | Within allowed processes | Yes | Within allowed processes | Yes |
| Edit, submit, or cancel a draft | Own draft | Own draft | Own draft | Own draft; no silent override |
| Review submitted content | When explicitly entitled | When routed | When routed | When routed |
| Suggest edits and submit a revised candidate | No, unless separately an eligible assigned reviewer | When assigned to review | When assigned to review | When assigned to review |
| Approve or reject | No | Per routing rules | Per routing rules | Per routing rules |
| Self-approve | No | Yes, explicit exception | No, unless also owner/admin | Yes, explicit exception |
| Attest imported current SOP | No | Yes, relevant process | No, unless also owner/admin | Yes |
| Import historical revisions | No | Yes, relevant process | No, unless also owner/admin | Yes |
| Maintain users, roles, and manager relationships | No | No | No | Yes |

Administrators must not be able to silently rewrite published content or approval history.
Exceptional administrative correction/reassignment needs a separate audited operation if added.

### Confirmed reviewer-editing workflow

An eligible assigned approver may select **Suggest edits** to create an editable copy of the
submitted candidate. The existing submitted snapshot is never edited in place. The working copy
is private to the editing step and does not replace the candidate being reviewed until the editor
selects **Submit revised candidate**.

Submission preserves the author's candidate as a superseded snapshot and makes the revised
snapshot the sole current review candidate in the same change request. Record the editor, changes,
reason, predecessor candidate, and time. Preserve every submitted candidate for authorized
comparison and audit. Do not require or record a rejection solely to incorporate reviewer edits.
Rejection continues to mean that the proposed change should not proceed.

Notify the original author when a revised candidate is submitted. Their acceptance is not required
by default. Route the new candidate to eligible reviewers and retain earlier approval decisions
as historical decisions applicable only to their original candidate.

The editor becomes a participant in the revised candidate. Preserve the original author's and
other contributors' participant IDs across the candidate lineage; copying or changing the submitter
must not erase participation. Re-evaluate routing and reviewer eligibility after each submission:

- A process owner who edits routes the candidate to their eligible manager or an administrator,
  or uses the explicit self-approval exception with its required reason and notifications.
- An administrator who edits normally routes the candidate to the process owner, or uses the
  explicit self-approval exception.
- A manager who edits cannot approve their contribution unless separately eligible for the
  process-owner or administrator self-approval exception. Route to an eligible independent reviewer
  under the existing policy; a nominal reviewer who is also a participant is not independent.

If routing leaves no eligible independent reviewer, expose only an authorized explicit self-approval
path or an explained block. Never silently treat a participant's decision as normal approval.

## 4. Confirmed lifecycle and concurrency rules

| Action | Preconditions | Result |
| --- | --- | --- |
| Create draft | Authorized actor; process and owner identified | Editable DRAFT; no publication change |
| Submit | Authorized draft author; valid content; eligible reviewer or explicit exception path | IN_REVIEW; freeze submitted content; record review assignment |
| Suggest edits | Eligible assigned reviewer; request IN_REVIEW | Editable reviewer copy; existing candidate remains immutable |
| Submit revised candidate | Authorized reviewer-editor; valid content; unchanged current candidate and request state | Atomically supersede previous candidate, record participants/audit/event, and reroute review; request remains IN_REVIEW |
| Reject | Assigned eligible reviewer; request IN_REVIEW | REJECTED; reason recorded; requester notified |
| Revise rejection | Authorized author | New editable attempt retaining the rejected submission and decision |
| Cancel | Authorized author; DRAFT | CANCELLED; retained history; no publication change |
| Approve and publish | Assigned reviewer or explicit eligible self-approver; IN_REVIEW; unchanged base | Record approval and publish atomically; update current reference; preserve previous revision |

For the first milestone, approval and publication are one transaction. A scheduled or separately
authorized publication step is deferred. Self-approval uses the same validation and publication
transaction and cannot publish incomplete or stale content.

**Confirmed:** Allow multiple drafts based on the same published revision. On publication, lock or
conditionally update the SOP's current reference. Once one draft is published, another based on
the old revision must be reconciled and reviewed again. Do not silently overwrite the first change.
Use explicit concurrency checks for draft edits as well, so simultaneous saves cannot lose content.

Record the base revision and submitted content version. Duplicate approval requests must not
create duplicate publications or duplicate business events. Rejecting an already published,
cancelled, or rejected request must fail without changing its history.

**Confirmed:** Snapshot reviewer assignments at submission and recheck eligibility when deciding.
An owner, manager, or role change that invalidates the assignment requires audited reassignment.
A missing manager must not silently cause ordinary self-approval; route to an eligible admin.
If none exists, explain the block and the permitted exception path.

Candidate replacement and publication must use the same request-level concurrency check. If
replacement wins, approval of the previous candidate fails as stale. If publication wins, submission
of the working copy fails rather than reopening the completed request. Merely opening an editing
copy does not indefinitely lock publication. Concurrent reviewer submissions cannot silently replace
each other: the later submission must reconcile with the new candidate. A failed replacement leaves
the previous candidate, decisions, and publication reference unchanged and sends no success notice.

## 5. Intake, history, and comparisons

### Confirmed intake rules

- An administrator or relevant process owner may attest that an imported document is current.
- Intake must identify the business process, corresponding SOP identity, and process owner.
- Preserve the original Word document. For Google Docs, preserve a snapshot in a selected export
  format and retain the source reference; a Google Doc is not itself a portable original file.
- Imported current content is labeled as an **attested current baseline**, distinct from a revision
  approved within Gatekeeper. It is authoritative current content after the authorized attestation.
- Further updates use a Gatekeeper template and the approval workflow.
- Administrators and relevant process owners can import earlier versions for historical reference.

**Proposed:** Capture source, importer, original version label, supplied effective date if known,
and attestation actor/time. File-format fidelity and supported conversion formats need a later
intake design review. Parsing failure must not be disguised as a successful structured conversion.
Replacing an already-current version through intake requires explicit confirmation and audit.

### Out-of-order history

**Confirmed:** Display history by the document's position in the SOP timeline, not upload order.
Allow the importer to identify predecessor and successor versions, including insertion at either end.

**Confirmed:** Use an opaque ordering key separate from the public label and revision ID. Avoid
floating-point version arithmetic. The chosen database strategy must support repeated insertions
and concurrent uploads without duplicate positions; internal rebalancing must preserve relative order.

Validate that neighbors belong to the same SOP and organization, are in the requested order, and
still define the chosen insertion gap. If another import changes the gap, request reconfirmation
rather than silently guessing. Conflicting dates, repeated labels, or uncertain ordering require
an explicit resolution; retain original labels instead of inventing new source version numbers.
Draft work remains clearly marked and distinct from attested/published historical versions.

### Comparison experience

**Confirmed:** Users can compare any two revisions they are authorized to read. During review,
default to the proposed revision versus its current published counterpart. From history, default
the second pane to the current published revision. A first-ever draft has no published counterpart;
show that state explicitly.

**Confirmed:** Aligned sections, highlighted additions/deletions, optional synchronized scrolling,
changes-only view, and metadata differences. Stable section identities help detect moves.
For an imported baseline, show the original snapshot beside the structured draft; claim precise
highlighted differences only where extraction is reliable. Clearly distinguish draft, historical,
attested-current, published-current, and self-approved states.

## 6. Audit, notifications, and reporting

**Confirmed:** Notifications support in-app delivery and email, with room for other channels.
Record a durable event in the same database transaction as the business action. Delivery workers
process it asynchronously. Email outages do not roll back approvals or lose the queued event.

Maintain separate records for:

1. **Business audit:** actor, organization, SOP/revision, action, transition, time, reason, and exception.
2. **Delivery history:** event, intended recipient, channel, attempt times, status, retry count, and error.

**Confirmed normal recipients:** submission -> assigned reviewers; rejection -> requester;
publication -> requester and process owner. Self-approval additionally follows the confirmed
recipient rule in section 3. Subscriber notifications and preference controls come later;
preferences must not silently suppress mandatory governance notifications.
Submitting a revised review candidate also notifies the original author and its newly assigned
reviewers. Include the candidate reference so recipients can identify and compare the exact content.

The in-app notification report must support filtering by time, SOP, event, recipient, channel,
and status. Distinguish queued, attempting, provider-accepted, failed, delivered, and bounced.
Delivered/bounced states require provider evidence; provider acceptance does not establish delivery
or readership. In-app availability is likewise distinct from read status.

**Confirmed:** Admins see organization-wide reports; process owners see reports for their processes;
other users see their own notifications. Authorized retries are audited. Retry transient failures
with bounded backoff; surface permanent failures. Use deduplication keys and provider idempotency
where available. Do not promise exactly-once external email delivery across every failure mode.
Validate provider callbacks and ensure retries cannot cross organization boundaries.

Diagnostic logs are separate from audit records. Include request/event correlation identifiers
and useful error context, but no passwords, tokens, or complete SOP bodies. Use readable console
output in development and structured production output. Retention periods remain to be agreed.

## 7. Storage, ownership, and subscription foundations

**Confirmed isolation:** separate application, database, storage, and credentials per client.
**Confirmed storage implementation:** relational database for structured SOPs, workflow, permissions,
and metadata; private object storage for imported originals, attachments, and snapshots. Authorize every file
download through the same organization and content-access rules as the SOP. Physical storage
layout does not substitute for application authorization.

**Confirmed commercial direction:** tiered organization subscriptions, with initial foundations
and placeholders rather than immediate payment processing. Proposed initial records: plan key,
subscription state, feature entitlements, and usage measurements. Manually assign plans first.
Subscription entitlements never grant user permissions.

Exports should include usable content, original files where retained, version metadata, and
approval/audit history within the requester's authority. Billing restrictions must not turn client
content into a hostage. Pricing, quotas, retention, closure, and deletion rules are later decisions.

## 8. Acceptance scenarios

These are planned checks, not claims of implemented or passing behavior.

| ID | Scenario and expected evidence |
| --- | --- |
| A01 | An ordinary author creates a new SOP: it remains unpublished until the relevant process owner approves. |
| A02 | A manager submits a change: it routes to the process owner; manager status alone cannot approve it. |
| A03 | The process owner submits: either an eligible manager or admin can approve without waiting for both; simultaneous decisions publish only once. |
| A04 | An admin submits: the process owner is the normal reviewer; admin self-approval is explicit. |
| A05 | An ordinary author calls the self-approval API directly: denied, with no publication or approval event. |
| A06 | An owner/admin self-approves: exception and reason recorded; designated approvers, manager, and admins notified once per channel. |
| A07 | A user changes another author's draft, submits it, or cancels it by guessing its ID: denied under the proposed action matrix. |
| A08 | A reviewer attempts an in-place edit of frozen submitted content or rejects an already published request: denied; use the authorized copy-and-submit workflow for reviewer edits. |
| A09 | Two edits race: the second stale save receives a conflict rather than overwriting the first. |
| A10 | Two revisions based on the same current version are approved: only one publishes; the stale request requires reconciliation. |
| A11 | Approval is retried: only one publication and one logical business event exist. |
| A12 | A database failure interrupts publication: approval, current pointer, audit event, and outbox changes roll back together. |
| A13 | Email delivery fails after publication: publication stays valid; report shows retry/failure and eventual provider status. |
| A14 | Same person qualifies as manager and admin: one intended notification per event/channel, not two. |
| A15 | Current baseline is imported by the relevant owner/admin: attestation is visible; no fabricated Gatekeeper approval. |
| A16 | Historical versions arrive C, A, then B: display A, B, C; original labels and IDs remain unchanged; current publication does not move. |
| A17 | Invalid/cross-SOP neighbors or a concurrently changed insertion gap: import placement is rejected or reconfirmed. |
| A18 | Compare any two permitted versions: labels, metadata changes, and available content are shown; defaults follow section 5. |
| A19 | Two client deployments contain overlapping local IDs: a token from client A is rejected by B; A's database/storage credentials cannot access B's resources; requests, jobs, exports, and notifications remain within the configured client. |
| A20 | Owner/manager changes during review: invalid old assignments cannot publish; reassignment is visible and audited. |
| A21 | A published version is exported and reopened: content and associated version/history metadata remain usable. |
| A22 | Workflow forms and comparison controls work by keyboard; labels and errors are accessible. |
| A23 | Ordinary authors, managers without another qualifying role, and unrelated process owners cannot see self-approval actions; direct API attempts are denied. Authorized audit/notification recipients can still see that self-approval occurred. |
| A24 | A participant tries normal approval after another person submits: denied; captured participant IDs persist. An eligible owner/admin must use explicit self-approval with a nonblank reason. |
| A25 | Self-approval with a missing or blank reason: rejected with no publication, approval decision, or success notification. |
| A26 | An eligible assigned reviewer selects Suggest edits: an editable copy is created; the submitted snapshot and current candidate remain unchanged until submission. |
| A27 | A reviewer submits a revised candidate: both snapshots remain in the same request; the original is superseded, not rejected; the author is notified without an author-acceptance gate. |
| A28 | A candidate changes: prior approvals remain tied to the old snapshot and cannot authorize publication of the new one; reviewers are rerouted and notified. |
| A29 | An owner/admin/manager edits: participant IDs accumulate across the lineage, independent approval routing is re-evaluated, and only eligible owners/admins may use explicit self-approval. |
| A30 | Candidate replacement races publication or another replacement: one transition wins, stale operations fail, and the current candidate/publication cannot silently change underneath a decision. |
| A31 | Candidate replacement fails mid-transaction: previous candidate and decisions remain valid for that snapshot; no revised-candidate success notification is queued. |

## 9. Verification and delivery discipline

**Confirmed:** Testing and its evidence are part of development, visible in both console and IDE.

- Unit tests: lifecycle rules, routing, eligibility, ordering, recipient calculation, and exceptions.
- MySQL integration tests: migrations, transactions, constraints, concurrent edits/publications,
  and durable event creation. Use an isolated test database, never the development or client database.
- API tests: authentication, object-level authorization, validation, contracts, and safe error responses.
- Deployment isolation tests: separate client credentials, token trust, storage access, job routing,
  and backup/restore targets, using at least two isolated test client deployments.
- Frontend tests: forms, state visibility, comparison behavior, accessibility, and failure states.
- End-to-end tests: author submission, reviewer decision, publication, history, and notifications.
- CI: repeatable build and appropriate checks for each pull request, with accessible test reports.

Use a fake notification provider for normal automated tests. Controlled delivery tests must use
explicit test recipients and credentials; no automated test sends to client addresses. Include
failure injection for retries and duplicate callbacks. Document deterministic test personas and data.

Provide named IntelliJ run/debug configurations and command-line equivalents, individual test
selection, console logs, and reports. Avoid arbitrary coverage targets or tests that merely duplicate
implementation details. Treat the inherited failing frontend test as a requirement to resolve, not
something to hide or weaken just to make CI green.

Every functional increment includes a high-level explanation, acceptance IDs covered, test results,
a short architect-run scenario, learning notes where useful, and remaining limitations.

Delivery sequence:

1. Architect reviews this specification and resolves the foundational choices below.
2. Separate repository cleanup: remove capstone-evaluator comments, preserve useful technical
   explanations, and replace the evaluator README with product/developer documentation.
3. Establish test isolation, repeatable fixtures, IntelliJ configurations, logs, and CI.
4. Review schema/migration design, including preservation of existing MVP records.
5. Deliver the core lifecycle, permissions, audit, and notification-outbox workflow with tests.
6. Add version browsing/comparison and notification reporting, then document intake and other integrations.

Do not automatically publish migrated drafts, manufacture historical approvals, or rewrite prior
Flyway migrations to introduce new behavior. Review a data-preservation and rollback plan before
applying schema changes to any shared environment.

## 10. Architect decision record

Confirmed in the architect's review:

1. **Client isolation:** separate application deployment, database, storage, and credentials per
   client; one common codebase and release process. This replaces the original shared-service proposal.
2. **Self-approval:** only the relevant process owner or client administrator may access or see
   the workflow; a reason is required. Maintain participant IDs with submissions to prevent
   inappropriate normal approvals or disguised self-approvals.
3. **Approval routing:** either the process owner's direct manager or an eligible administrator
   may approve the owner's changes; both are not required. A manager's submission routes to the owner.
4. **Lifecycle:** approve-and-publish is atomic; preserve rejected submissions; multiple drafts
   use stale-base conflict detection. Apply reviewer eligibility checks and audited reassignment.
   Withdrawal while IN_REVIEW remains deferred.
5. **Historical ordering:** internal ordering keys preserve source labels and stable revision IDs;
   out-of-order intake never silently changes current publication.
6. **Comparison experience:** aligned sections, highlighted changes, optional synchronized scrolling,
   changes-only view, metadata differences, and the documented version-selection defaults. Imported
   snapshots remain available alongside structured drafts, with extraction limitations made clear.
7. **Audit, notifications, and reporting:** separate business audit and delivery history; durable
   transactional events; asynchronous delivery with retries and deduplication; the stated recipient
   rules, report visibility, delivery statuses, and diagnostic logging practices are approved.
8. **Storage:** a relational database for structured records and private object storage for originals,
   attachments, and snapshots, isolated per client and protected by content-access authorization.
9. **Reviewer editing:** an assigned approver edits a copy and submits a successor candidate within
   the same request. Preserve every submitted snapshot; supersede rather than artificially reject
   earlier candidates. Prior approvals do not carry forward. Editing adds participation and triggers
   fresh routing/self-approval checks. Notify the author without requiring acceptance by default;
   serialize candidate replacement against publication and other reviewer submissions.

The full proposed action matrix has not been explicitly approved as a whole. General pre-submission
draft editing and read scope remain proposals; the assigned-reviewer copy-and-submit workflow above
is confirmed. General coauthoring/delegation is deferred, not reviewer editing. Participant tracking
applies to every submitted candidate regardless of whether broader collaboration is introduced later.

Later design decisions: detailed process-level read visibility, source document conversion and
template structure, retention/deletion periods, provider selection, export formats, subscription
packaging, and quota behavior. Resolve each before implementing its affected feature.
