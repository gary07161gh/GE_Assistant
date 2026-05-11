---
description: Find semantic duplicate functions in the codebase
argument-hint: [directory-or-file]
---

Invoke the Skill tool to load the "superpowers-lab-finding-duplicate-functions" skill. Use two-phase approach: classical extraction followed by LLM-powered intent clustering.

Phases:
1. **Extract function catalog** — Scan codebase to extract all function signatures and implementations
2. **Categorize by domain** — Use haiku subagent to categorize functions by domain/purpose
3. **Split into categories** — Prepare individual category files for analysis
4. **Find duplicates per category** — Use opus subagent per category to detect semantic duplicates
5. **Generate report** — Produce prioritized markdown report grouped by confidence level
6. **Human review & consolidate** — Verify survivors have tests, update callers, delete duplicates

Focus on high-risk zones: utils/, helpers/, lib/, validation code, string/date formatting, error handling.