---
description: Request a code review subagent to catch issues before merging
---

Invoke the Skill tool to load the "superpowers-requesting-code-review" skill. Dispatch a code reviewer subagent to catch issues before they cascade.

Follow the pattern:

1. **Get Git SHAs** — `BASE_SHA=$(git rev-parse HEAD~1)` and `HEAD_SHA=$(git rev-parse HEAD)`
2. **Dispatch code reviewer subagent** — Use Task tool with template providing brief description, plan/requirements, base SHA, and head SHA
3. **Act on feedback**:
   - Fix Critical issues immediately
   - Fix Important issues before proceeding
   - Note Minor issues for later
   - Push back with technical reasoning if reviewer is wrong

Core principle: Review early, review often. Review after each task in subagent-driven development, after completing major features, and before merge to main.