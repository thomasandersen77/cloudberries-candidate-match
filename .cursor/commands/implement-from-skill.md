# Implement from project skill

You are implementing work described in this repository’s **Agent Skill** (implementation spec / plan).

**Before writing code:**

1. Read the skill the user names in their message (default: they will say something like “skill `my-feature`” or paste the path). If they gave no name, ask once: “Which skill under `.cursor/skills/<name>/` should I follow?”
2. Open and read **`SKILL.md`** fully. If the skill links to `reference.md`, `examples.md`, or `PLAN.md`, read those too when they are part of the plan.
3. Align with project rules: `.cursor/rules/` and existing patterns in the codebase.

**Then:**

4. Turn the plan into a **short ordered task list** (phases, files to touch, tests).
5. **Implement** the next chunk the user asked for—or the **whole plan** if they said “implement everything” / “full implementation.” Prefer small commits or logical steps; run tests/build when the project uses them.
6. **Summarize** what changed, what remains, and how to verify.

**Constraints:** Follow Cloudberries layering (controller → service → port/adapter), do not leak secrets, match existing style. If the plan conflicts with repo rules, follow the repo and say what you adjusted.
