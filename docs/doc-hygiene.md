# Doc hygiene

Rules that keep documentation from growing. The `doc-guard.sh` hook (`rhaydus-kotlin` plugin) enforces them at
write time and the `rhaydus.doc-budgets` Gradle plugin enforces them in CI. Both read the `docGuard` object in
the project's `.claude/rhaydus.json`, so one config is the single predicate source.

- **Doc types.** Reference docs hold present-tense rules and facts only, with no history or justification.
  Working docs hold state (a `## Now` block), checkboxes and one line per decision. KDoc follows the project's
  documentation standard. Agent memory keeps only what the code does not make obvious.
- **Rewrite, never append.** Rewrite the entry to the current truth and delete the sentence it replaces. Never
  add a change note ("previously...", "that replaced..."). One fact lives in one place; link to it elsewhere.
  A reference line that must stay historical is marked `<!-- history-ok -->`.
- **Plans are temporary.** A working-docs plan directory is deleted when the plan finishes, so code, KDoc,
  permanent config comments, permanent docs and agent memory never cite one, its steps or its decision
  numbers. The reason for a change goes in the pull request description.
- **Entry lengths.** A decision is one line; an index line is one sentence; a `## Now` block has a line limit.
- **Budgets.** Every markdown file has a size budget in the budgets file (`<glob> <limit>`, first match wins;
  units `B`, `KB` = 1024 bytes, `L` = lines; a `## Now <limit>` line budgets the `## Now` block of each tracker
  listed in the active file). A file over budget may shrink but never grow, measured against the merge-base
  with the default branch. Changing a budget is a deliberate, reviewed diff, never a way past the gate.
- **Edit markdown with Edit / Write only**, never shell redirection, heredocs or scripts, so the write-time hook
  sees every change.

## Config

`.claude/rhaydus.json`, key `docGuard`. Paths are project-relative; an entry ending in `/` is a prefix.

| Key | Meaning |
|---|---|
| `budgetsFile` | the budgets file |
| `activeFile` | file listing the trackers whose `## Now` block is budgeted, one path per line |
| `referenceDocs` | prefixes where history lines are denied |
| `workingDocs` | prefixes of working docs and plans; comments may not cite them, and `## Decisions` bullets stay one line |
| `permanentDocs` | paths that may not cite plans, steps or decision numbers |
| `decisionPattern`, `stepPattern` | ERE for decision numbers and plan steps (defaults `\bD[0-9]{1,2}\b`, `\bS[0-9]{1,2}-[0-9A-Z]`) |
| `routing` | `[{path, hint}]`: the hint shown when a file at that path breaks its budget |
| `defaultHint` | hint for paths no routing entry matches |

A missing config file or `docGuard` key turns both the hook and the gate off. The Gradle plugin takes the
default branch for the ratchet as `docBudgets { defaultBranch = "main" }`.
