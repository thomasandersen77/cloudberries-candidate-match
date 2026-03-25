# Create Skill (orchestrated)

You are helping create a **Cursor Agent Skill** for this repository (or the user’s personal skills if they say so).

**Follow the bundled create-skill workflow** (structure, YAML frontmatter, description rules, phases Discovery → Design → Implementation → Verification). Treat the user’s message as the product brief: what the agent should do, when it should apply, and any constraints.

**Do this now:**

1. If anything critical is missing (scope, project vs `~/.cursor/skills/`, trigger scenarios), ask briefly—otherwise infer from context.
2. Propose a kebab-case skill `name` and a third-person `description` with WHAT + WHEN + trigger terms.
3. Write `SKILL.md` (concise body; optional `reference.md` / `examples.md` / `scripts/` only if needed).
4. If the user wants a **slash-command shortcut** for the same workflow, say they can add a companion file under `.cursor/commands/` and what to name it—or offer to add it.

**Do not** put YAML frontmatter in `.cursor/commands/` files (commands are plain markdown prompts). Skills use frontmatter in `SKILL.md` only.
