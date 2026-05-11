---
description: Run interactive CLI tools via tmux detached sessions
argument-hint: [command-description]
---

Invoke the Skill tool to load the "superpowers-lab-using-tmux-for-interactive-commands" skill. Use tmux for controlling interactive sessions (vim, git rebase -i, REPLs, etc.) that require real-time input/output through a real terminal.

Core pattern:
1. **Create detached session** — `tmux new-session -d -s <name> <command>`
2. **Wait briefly** — `sleep 0.3` for initialization
3. **Send input** — `tmux send-keys -t <name> 'text' Enter`
4. **Capture output** — `tmux capture-pane -t <name> -p`
5. **Repeat** steps 3-4 as needed
6. **Terminate** — `tmux kill-session -t <name>`

Special key names: Enter, Escape, C-c, Up, Down, Left, Right, Space, BSpace