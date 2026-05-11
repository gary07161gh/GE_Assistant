# GE Plugin

## Quick Start

```bash
ao quick-start        # Repair or inspect the repo seed
bd ready              # See unblocked issues when beads is enabled
/rpi "objective"      # Run discovery, implementation, validation
```

## Session Protocol

```bash
# Start
ao status             # Check AgentOps state
bd ready              # Find available work

# End
git add .
git commit -m "..."
git push              # NEVER stop before pushing
```

## JIT Loading

| Working On | Load |
|------------|------|
| Research | .agents/research/ |
| Implementation | Check existing patterns first |
| Debugging | .agents/learnings/ |


## AgentOps Knowledge Flywheel

Knowledge compounds automatically across sessions:

- **MEMORY.md** is auto-loaded by your AI coding tool every session
- **Session hooks** extract learnings, update MEMORY.md, and prune stale knowledge
- **Skills** invoke flywheel commands at the right moments (no manual ao commands needed)

Verify the flywheel any time:

```bash
ao flywheel status    # escape velocity check
ao status             # current knowledge inventory
```
