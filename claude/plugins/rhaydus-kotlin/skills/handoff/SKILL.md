---
name: handoff
description: Rewrite the active tracker's `## Now` block so the next session can resume from it after /clear, then ask before committing.
disable-model-invocation: true
---

# Handoff

Close out this session so a fresh one can pick up from the tracker alone. The next session starts from
the `## Now` block of the active tracker, so that block must carry everything it needs. The convention is
in the foundation's `docs/working-docs.md`.

## Project inputs

Read `$CLAUDE_PROJECT_DIR/.claude/rhaydus.json` with `jq` (a missing file or key means the default):

- `session.activeFile`: the pointer file listing active trackers. Default `docs/working/ACTIVE.md`.
- `session.closeOutGates`: a list of commands or checks that must pass before a step counts as done. Default:
  none.

## Procedure

1. **Resolve the tracker.** Read the active file (one repo-relative path per line). If it lists one file, use
   it. If it lists several and this session touched more than one of them, ask the user which to hand off.
   If it is missing or empty, tell the user and stop.

2. **Run the close-out gates** from `session.closeOutGates`, when this session changed files they cover. Run
   each command that has not passed this session after the last relevant edit; apply each check to the
   changed files (`git diff --name-only HEAD` plus untracked files). With no gates configured, skip this step
   and write "no gates configured" under **Verification**.

   Any finding or failed gate means the step is not done: **State** must not call it done, and the findings
   go under **Next**. Write the gate results under **Verification**.

3. **Rewrite its `## Now` block** (everything from `## Now` up to the next `## ` heading) to this shape,
   **at most 40 lines**:

   ```markdown
   ## Now

   - **State:** what is done, in one or two sentences. Name finished steps/sections, not the journey.
   - **Next:** the exact next action, with repo-relative file references (the step file and section).
     One item, not a list of options.
   - **Open questions:** anything waiting on the user, or "none".
   - **Verification:** the last gate that ran and its result, or "not run" with the reason.
   - **Uncommitted:** a summary of `git status --short` (step 5).
   ```

   If the old block has a **Due when** line (`- **Due when:** <step> in <tracker> - <condition>`), carry it
   into the new block unchanged. Once the condition holds (check the named step files' checkboxes), drop the
   line, make that step the **Next** item, and start the final message with `Due: <step>.`

   Replace the old block; do not append to it. Drop history the next session does not need. Decisions
   belong under the tracker's `## Decisions`, not in `## Now`.

4. **Audit agent memory if a plan step finished.** If `git status --short` shows a deleted step file under a
   plan's `steps/` directory, or this session deleted one itself, read each `.claude/agent-memory/*/MEMORY.md`
   index. Delete the memories that only served that step and keep only durable, non-obvious lessons. Keep
   each index line in sync with the file it points to. Report what was pruned in the final message.

5. **Summarise uncommitted changes.** Run `git status --short` and group the paths by purpose (for example
   "hook scripts", "tracker docs") rather than listing every file. Write that under **Uncommitted**.

6. **Ask whether to commit.** Propose a commit message and commit only if the user says yes; never commit
   unasked.

7. **End with exactly:** `Handoff written. Type /clear, then ask the next session to resume from the Now block.`
