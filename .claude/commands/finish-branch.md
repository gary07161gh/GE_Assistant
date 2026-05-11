---
description: Complete a development branch — merge, PR, keep, or discard
---

Invoke the Skill tool to load the "superpowers-finishing-a-development-branch" skill. Follow the process:

1. **Verify Tests** — Run full test suite first. If tests fail, fix before proceeding.
2. **Detect Environment** — Determine if normal repo, worktree, or detached HEAD.
3. **Determine Base Branch** — Find merge-base (main/master).
4. **Present Options** — Show structured options (merge/PR/keep/discard).
5. **Execute Choice** — Handle the selected workflow.
6. **Cleanup Workspace** — Clean up if merging or discarding.

Iron Law: Always verify tests pass before offering completion options. Never delete work without typed confirmation.