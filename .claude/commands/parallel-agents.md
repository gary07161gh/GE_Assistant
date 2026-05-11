---
description: Dispatch parallel agents for independent tasks
argument-hint: [task-description]
---

Invoke the Skill tool to load the "superpowers-dispatching-parallel-agents" skill. Use when facing 2+ independent tasks that can be worked on without shared state or sequential dependencies.

Follow the pattern:

1. **Identify Independent Domains** — Group failures/tasks by what's broken (different files, subsystems)
2. **Create Focused Agent Tasks** — Each agent gets specific scope, clear goal, constraints, expected output
3. **Dispatch in Parallel** — Use Task tool, one per independent domain
4. **Review and Integrate** — Read each summary, verify fixes don't conflict, run full test suite

Each agent prompt must be: focused, self-contained, and specific about expected output.