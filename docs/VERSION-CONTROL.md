# Branches and reviewed changes

`main` is the default branch and reviewed application baseline. New work starts from current
`main` on a short-lived `codex/<feature>` branch and returns through a pull request. Commit
messages and pull-request descriptions should explain the resulting behavior and its verification.

Required checks on `main` are **Backend tests and package** and **Frontend tests and production
build**. Keep the proposed branch current with `main`, resolve review conversations, and check
the passing results before merging. Architecture and workflow acceptance remain an explicit
discussion with the architect. A second GitHub approver is not required while this is a solo-owned
repository; GitHub does not allow an author to approve their own pull request.

## Production branch during the transition

On September 23, 2026, both Railway application services were confirmed to deploy from
`working_branch`. That branch is retained at the existing MVP baseline (`b85cad1`) as a temporary
production reference. Do not merge new development into it, delete it, or repoint Railway as part
of ordinary branch cleanup. `main` may contain reviewed development features that have not yet
been enabled or validated for production. Production rollout requires the configuration and
deployment work described in the README and lifecycle walkthrough.

This separation lets GitHub use the conventional `main` name without triggering an incidental
production rollout. After an approved deployment cutover, retire the temporary production branch.

## Working locally with IntelliJ and Codex

Both tools currently use the same checkout at `D:\SOP Gatekeeper`. A branch change in IntelliJ
also changes the files Codex sees. Coordinate branch changes while development or tests are
running. For simultaneous work on different branches, use separate Git worktrees.

After merging, start the next development branch from `origin/main`. Push it with an upstream
so IntelliJ shows whether commits need to be pushed or pulled. Keep credentials, local database
files, downloaded runtimes, logs, and coverage artifacts out of Git; the ignored `.local` directory
contains local practice credentials and runtime data.

Older setup/testing branches can remain as historical references until their work is merged and
any external dependencies are checked. They are not alternative main application branches.
