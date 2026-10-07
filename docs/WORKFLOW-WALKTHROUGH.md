# Local workflow review

Open http://localhost:4200/sops. The current development UI uses the governed API. If an old
session shows a permission error, log out and sign in again. Credentials are stored only in
the ignored `.local/workflow-demo.json` file. All four practice users share its generated
password; do not use these accounts in a client deployment.

| Account | Practice responsibility |
| --- | --- |
| `demo.author` | Create and submit a procedure |
| `demo.owner` | Process owner for **Practice workflows**; review ordinary authors and manager submissions |
| `demo.manager` | Owner's manager; independently review the owner's contributions |
| `demo.admin` | Administrator; approve eligible owner submissions and recover stale routing |

## Start locally

Run these commands from the repository root, using separate terminals for the long-running
backend and frontend:

```powershell
./scripts/dev.ps1 database
./scripts/dev.ps1 backend-lifecycle
# Once the backend is ready, in another terminal:
./scripts/prepare-workflow-demo.ps1
./scripts/dev.ps1 frontend
```

Do not start a second copy of a service that is already running. The demo initializer is
restricted to the local `woven` database on port 3307. It creates practice users, process
ownership, and six versioned product guides. Repeating it preserves unchanged guides and
appends a revision when a guide's content changes. It does not delete or rewrite MVP seed data.
It refuses to replace existing demo accounts when their credential file is missing.

## First walkthrough: author → owner edits → manager approves

1. Sign in as `demo.author`. Read **Create and submit your first SOP** in Published SOPs.
2. Choose **New SOP**, follow the group → department → family selectors to **Practice workflows**,
   and enter a title and purpose. Add steps with **Who**, **What**, and **Where**; move them to test numbering.
3. Choose **Create draft**. Edit a step, then **Save copy**. Verify **Submit for approval** is
   unavailable while edits are unsaved. Submit the saved copy.
4. Log out and sign in as `demo.owner`. Open **My work & reviews** and select the submission.
5. Choose **Edit a private copy**, change a step, and **Save copy**. Enter a change reason and
   choose **Submit revised candidate**. Compare the earlier and revised candidates.
6. The owner is now a contributor. Ordinary approval is unavailable; self-approval requires
   a reason. For independent review, log in as `demo.manager` and open the assigned request.
7. Select the current candidate in **Compare to**, then **Approve & publish**. Verify the SOP
   appears in Published SOPs and that Activity includes the decision.
8. Return as `demo.author`, open the published SOP, and **Start revision**. The current publication
   stays unchanged while this new draft is edited and reviewed.

## Additional scenarios

- **Reject and revise:** owner enters a rejection reason; the original author selects
  **Revise rejected submission**. The old decision and contributor identities remain preserved.
- **Self-approval:** owner creates and submits a separate SOP; **Self-approve & publish** requires
  a nonblank reason. Check the notification records as owner, manager, and administrator.
- **Manager authors:** submit as manager and verify owner review rather than manager self-approval.
- **Conflict:** open the same draft in two browser windows, save one, then try saving the other.
  The second save fails with a conflict message and preserves its unsaved text for reconciliation.
- **History:** publish a second revision, reopen the SOP, and select any two published versions.
- **Privacy:** an unrelated user cannot browse another author's private draft, even as administrator.

## What this preview does and does not demonstrate

The UI includes a structured Who/What/Where template, consistently formatted side-by-side comparison, request activity,
and each user's notification record. Email is queued but not sent; this is not a delivery-status
report. Published history is client-wide; candidate history remains restricted to authorized
request participants. Word-level highlighting, document intake, historical
imports, notification delivery/reporting, and stale-base reconciliation
are later increments. Lists currently show the latest 100 eligible work candidates/notifications;
pagination is still needed before larger-client use.

The production frontend remains on its previous routes (`lifecycleEnabled: false`). A governed
deployment must enable that frontend setting and `LIFECYCLE_API_ENABLED=true` together, provision
its real client/process configuration, and complete the remaining security/deployment review.
When the backend flag is on, legacy SOP and change-request controllers are disabled.

Tutorial source is `docs/tutorials/workflow-guides.json`. Update the relevant guides whenever
user-facing workflow behavior changes, rerun the local initializer, and verify the published
guide and its preserved history. Production guide installation is not implemented by this local
practice initializer.

## Organization setup and format conversion

Sign in as `demo.admin` and open **Admin**. The hierarchy screens manage organizations,
groups, departments, process families, processes, and subgroups. **Workflow setup** connects
processes to actual user accounts and their direct managers. Every ownership change requires
a reason and appears in configuration activity. A manager change applies to every process
owned by that user; active reviews may need **Recalculate approvers** afterward.

Only processes in the configured client organization with an assigned owner appear in authoring.
Optional subgroups must be linked to the selected process and belong to its department.
The server freezes hierarchy names and the current process owner at submission.

For an earlier plain-text SOP, **Start revision**, then **Use standard template**. Its original
text is copied into the first What field. Split it into appropriate steps and complete Who and
Where before submitting. The old publication remains unchanged, including after the replacement
is approved. Structured comparisons identify added, edited, and moved steps by stable identifiers.

## SOP Library (reader experience)

Sign in and open **SOP Library** at `/library`. Select any hierarchy level to list every
published SOP below it. Child cards provide progressive drill-down; breadcrumb buttons move
back up. Search is scoped to the selected branch. Opening a result displays only the current
publication; **History & comparison** is an optional secondary action.

Listings use persisted hierarchy codes, process family, process, descriptive title, and a
publication count (`v1`, `v2`, etc.), independent of revision database IDs. The full named path
appears underneath. Initial codes are allocated once for existing hierarchy entries; an Admin
can edit codes, or assign codes to newly created entries, under **Workflow setup → Library
hierarchy codes**, with a reason and concurrency checks. `?` in a path means a code still needs
assignment. Codes are unique among siblings. Subgroup filtering selects SOPs explicitly tagged
with that subgroup; department selection includes the whole department.

The library requires migration V108. Restart the backend after updating this checkout so Flyway
applies the migration and the hierarchy endpoint becomes available. Authoring and review remain
under **SOP Workspace** at `/sops`; workflow, report, and Admin navigation are grouped at the bottom.

The organization browser follows Organization → Group → Department → Subgroup. Process families
and processes remain in SOP identification but are not parallel organization navigation choices.
Selecting a department includes every SOP below it, including SOPs without a subgroup.

## Required subdepartment hierarchy (V109)

The canonical path is Organization → Group → Department → Subdepartment → Process Family → Process → SOP.
Process families require `deptSubgroupId`; their department is derived from that subdepartment.
A composite database foreign key prevents mismatched family/subdepartment departments. Legacy
process/subgroup association rows are retained for migration reference; current navigation,
authoring, API process metadata, and submitted document context use the family's required parent.
Library subgroup filtering now follows this canonical path, including earlier plain-text SOPs.

Admin create/edit forms use cascading parent dropdowns. Changing any parent clears all descendants.
Workflow ownership setup uses the same selector through Process. Organization creation has no parent.

Migration V109 assigns unambiguous existing subgroup relationships, then departments' sole subgroup
where available. Remaining families go into **General (migration review)** within their original
department. `family_hierarchy_migration` records original placement and which entries needed review.
Use **Admin → Business Process Families → Edit** to select the correct subdepartment. Populated
families cannot be moved to another department using ordinary editing; that requires a separate
reviewed migration. Existing publication snapshots are preserved. Assign display codes for newly
created migration-review subdepartments through Workflow setup as needed.

## Suggestions and alignment (V110)

1. Sign in as `demo.author`. Open **Suggestions & alignment → Suggest a change** and drill down to **Practice workflows**. Submit a problem, proposed change, and expected benefit.
2. As `demo.owner`, open the suggestion, classify the concern, choose a decline reason, and explain the proposed alternative. Select **Decline proposed solution**.
3. As `demo.author`, confirm that it remains open. Choose **Challenge / request alignment** with an explanation. The record now appears under **Alignment unresolved** and, for defects, **Unresolved process defects**.
4. As `demo.manager`, record an independent alignment review. This does not force agreement or close the record.
5. As `demo.owner`, revisit the decision and accept the suggestion, or explain a revised decline. If declined again, the author must explicitly agree with the next steps before corrective follow-up.
6. Record a plan and review date. Temporary containment cannot close the concern. After verification, enter evidence and propose resolution. As `demo.author`, agree to close or challenge. A closed concern can be reopened.
7. For an accepted suggestion, open a published client SOP in **SOP Workspace → Start revision**. Non-owners must select the accepted suggestion and provide a rationale. Owners may start directly with a rationale. Publishing the revision leaves the suggestion open for effectiveness verification and agreement.

See [SUGGESTION-WORKFLOW.md](SUGGESTION-WORKFLOW.md) for rules and current scope. The new **Suggest a change and reach verified alignment** tutorial is installed by `./scripts/prepare-workflow-demo.ps1`.

Tests: `./scripts/dev.ps1 test-backend -Test SuggestionApiTest` and `./scripts/dev.ps1 test-frontend`. Latest run logs are under `.local/logs/suggestion-api-tests.log` and `.local/logs/suggestion-frontend-tests.log`.
