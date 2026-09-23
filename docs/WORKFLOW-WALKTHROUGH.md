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
ownership, and four versioned product guides. Repeating it preserves unchanged guides and
appends a revision when a guide's content changes. It does not delete or rewrite MVP seed data.
It refuses to replace existing demo accounts when their credential file is missing.

## First walkthrough: author → owner edits → manager approves

1. Sign in as `demo.author`. Read **Create and submit your first SOP** in Published SOPs.
2. Choose **New SOP**, select **Practice workflows**, and enter a title, purpose, and procedure.
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

The UI includes a plain-text template, complete-text side-by-side comparison, request activity,
and each user's notification record. Email is queued but not sent; this is not a delivery-status
report. Published history is client-wide; candidate history remains restricted to authorized
request participants. Word-level highlighting, rich templates, document intake, historical
imports, notification delivery/reporting, administration UI cutover, and stale-base reconciliation
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
