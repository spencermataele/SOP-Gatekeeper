# Suggestions and alignment

## Entry and authority

All authenticated client users may submit suggestions for a governed process. The hierarchy selector must end at a process. Title, problem, proposed change, and benefit are required. Related publication, cost estimate, and ROI estimate are optional. Estimates are narrative and should include amounts, currency, timeframe, and assumptions.

Only the current process owner makes the suggestion decision. The submitter, current owner, owner's manager, and administrators can see the discussion. Manager/admin oversight does not grant normal decision authority or permission to agree on the submitter's behalf. Independent alignment review is available to the manager/admin when they are not the owner.

## Decision and concern are separate

The decision is pending, accepted, or declined. A decline requires a controlled reason and a written explanation. It enters `DECLINED_AWAITING_RESPONSE`; it never closes automatically. The submitter agrees to next steps or challenges. Challenges enter `ALIGNMENT_UNRESOLVED`, notify participants in-app, and remain open after an independent review until further owner/submitter action.

The concern is undetermined, none, training, defect, or both. Confirmed defects cannot be removed by later reclassification. The UI separates unresolved defects, other open suggestions/training, unresolved alignment, and closed records.

The initial implementation keeps one accountable owner and one evolving action/training plan per suggestion, preserving every prior plan in communication history. Multiple independently assigned corrective tasks are a future extension.

## Resolution

An action plan requires an effectiveness review date. Temporary containment is not closure. Closure proposals require an assessed concern, plan, evidence, and an effectiveness review date no later than today. Updating the plan or recording containment invalidates a pending closure proposal. Only the submitter may explicitly agree to resolution. They can challenge instead; owner or submitter can reopen a closed concern. No elapsed-time or read-based auto-closure exists.

## Revision entry

New SOP creation is unchanged. For a revision, the current process owner can start directly with a required rationale. Every other user, including admins and managers, needs an accepted suggestion applicable to that process and, when specified, that SOP. A challenged, declined, closed, or pending suggestion cannot authorize a new revision. The server enforces these rules independently of the UI. Historical work already started before V110 is not retroactively blocked.

The entry form links one accepted suggestion per new revision. A suggestion can lead to multiple revisions; the database link supports many-to-many relationships for a future multi-link editor. All existing publication approval and participant rules remain in force. Publication never automatically closes a linked concern.

## Communication and concurrency

Each command records actor, message, timestamp, action details, and in-app recipients in the same transaction. Recipients include submitter, owner, owner's manager where configured, and admins. `AVAILABLE_IN_APP` means the message is accessible, not read, acknowledged, or agreed. Agreement/challenge are explicit versioned actions. Email delivery is not enabled. Meeting summaries can be recorded in-app, but this does not independently verify that a meeting occurred.

Mutations use optimistic versions and command UUIDs. Identical retries do not duplicate commands; changed payloads cannot reuse a command ID. Review actions require the current record version. Normal request logs retain correlation IDs and status without logging suggestion text or credentials.

## API

- `GET/POST /api/lifecycle/suggestions`: authorized inbox / create.
- `GET /api/lifecycle/suggestions/{uuid}`: detail, communication record, recipients, linked revisions, role capabilities.
- `POST /api/lifecycle/suggestions/{uuid}/actions`: versioned response, decision, corrective action, agreement, or reopening.
- `GET /api/lifecycle/suggestions/eligible?documentId={id}`: accepted suggestion identifiers/titles applicable to revision entry.
- `POST /api/lifecycle/documents/{id}/drafts`: now requires `rationale`; `suggestionId` is required for non-owners.
- `GET /api/lifecycle/notifications`: includes suggestion notifications with `suggestionId` as well as existing SOP request notifications.

Migration V110 is additive. Restart the lifecycle backend to apply it before using the new UI.

## Change management (V111)

Acceptance now creates a separate `managed_change`, and V111 backfills planned records for previously accepted suggestions. The accepted view links to implementation, shows the implementation owner, dates, and saved ticket references. `NONE` remains the historical storage value and is displayed as **N/A**; `UNDETERMINED` is unchanged.

Change records support planned, in-progress, ready-for-validation, implemented, effectiveness-verified, blocked, and cancelled states. Forward progression is one stage at a time; resuming blocked/cancelled work returns to planned or in-progress. Implementation/verification requires evidence. Changed implementation plans invalidate a verified status until revalidated. Each update requires a reason, version, and retry-safe command UUID.

The process owner and assigned implementation owner may update a change; only the process owner may reassign implementation. Linked submitters, managers, and admins have oversight access. Accepted suggestions from the same process may be linked, and their SOP revision links are shown. The originating suggestion remains linked. Each newly accepted suggestion initially has its own change record; additional linking does not merge or delete those records.

Ticket references include system, ticket number, and absolute HTTP(S) URL. Ticket numbers are navigation links. Credentials in URLs and unsafe schemes are rejected. All ticket entry is manual; there is no external API call, ticket creation, credential configuration, webhook processing, or synchronization yet. `GET /api/lifecycle/changes`, `GET /api/lifecycle/changes/{uuid}`, and `PUT /api/lifecycle/changes/{uuid}` provide the initial contract. Status updates never alter suggestion agreement, defect closure, or publication approval.

Activity/communication logs in suggestions, SOP reviews, change tracking, and Workflow setup are collapsed by default, use smaller text, and scroll internally. The underlying audit records remain intact.
