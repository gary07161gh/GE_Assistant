---
description: Use MCP servers on-demand via the mcp CLI tool
argument-hint: [server-command]
---

Invoke the Skill tool to load the "superpowers-lab-mcp-cli" skill. Use the `mcp` CLI tool to dynamically discover and invoke MCP server capabilities without pre-configuring them as permanent integrations.

Workflow:
1. **Discover tools** — `mcp tools <server-command>`
2. **Discover resources** — `mcp resources <server-command>`
3. **Discover prompts** — `mcp prompts <server-command>`
4. **Call tools** — `mcp call <tool> --params '<json>' <server-command>`
5. **Read resources** — `mcp read-resource <uri> <server-command>`
6. **Use aliases** — `mcp alias add <name> <server-command>` for repeated use