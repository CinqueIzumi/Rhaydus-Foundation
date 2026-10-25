# Working docs

How a project keeps multi-session work resumable. The `rhaydus-kotlin` plugin's `context-budget` hook and
`handoff` skill implement it; both read their project inputs from `.claude/rhaydus.json` under `session`.

## Files

- **Active pointer** (`session.activeFile`, default `docs/working/ACTIVE.md`): one repo-relative tracker path
  per line. Empty or missing means no active work.
- **Tracker**: a markdown file with a `## Now` block, a `## How to run a step` section, the steps with
  checkboxes, and `## Decisions` (one line each). A tracker may link step files under a `steps/` directory.

## The `## Now` block

At most 40 lines, replaced on every handoff, never appended to:

- **State:** what is done, in one or two sentences.
- **Next:** the exact next action, with repo-relative file references. One item.
- **Open questions:** anything waiting on the user, or "none".
- **Verification:** the last gate that ran and its result, or "not run" with the reason.
- **Uncommitted:** `git status --short` grouped by purpose.
- **Due when** (optional): `<step> in <tracker> - <condition>`, carried until the condition holds, then promoted
  to **Next**.

## Running a step

1. Start a fresh session and resume from the `## Now` block of the tracker the active pointer names.
2. Do the one step `## Now` names. One step per session.
3. When acceptance passes, tick the step, update `## Now`, and ask before committing.
4. Run `/handoff`, then `/clear`.

## Close-out gates

`session.closeOutGates` lists the commands or checks a step must pass before it counts as done. A failed gate
keeps the step open and moves the findings to **Next**.

## Config

```json
{
  "session": {
    "activeFile": "docs/working/ACTIVE.md",
    "contextBudget": 150000,
    "closeOutGates": ["<command or check>"]
  }
}
```

Every key is optional, and the file itself may be absent. Without `contextBudget` the hook uses 150000.
