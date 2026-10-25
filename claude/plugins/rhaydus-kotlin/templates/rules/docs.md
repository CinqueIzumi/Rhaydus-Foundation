---
paths:
  - "docs/**"
  - "**/*.md"
---

# Writing docs

- **Doc types.** Reference docs hold present-tense rules and facts only, with no history or justification.
  Working docs hold state (a `## Now` block) plus checkboxes, and one line per decision. KDoc follows the
  `document-code` skill.
- **Rewrite, never append.** Rewrite the entry to the current truth and delete the sentence it replaces. Never
  add a change note ("that replaced...", "previously..."). One fact lives in one place; link to it elsewhere.
- **Plans are temporary.** A plan directory under the working docs is deleted when the plan finishes, so code,
  KDoc, permanent config, permanent docs and agent memory never cite one, its steps or its decision numbers.
- **Why a change was made** goes in the PR description, never a reference doc.
- **Budgets.** Every file has a size budget in `docs/doc-budgets.txt`. A file over budget may shrink but never
  grow. The doc-guard hook enforces this at write time and `checkDocBudgets` in CI. Changing a budget is a
  deliberate, reviewed diff.
- **Edit markdown with Edit / Write only**, never Bash redirection, heredocs or scripts, so the write-time
  hook sees every change.

## Project lines

<!-- The app's own doc routing table, entry-length limits and doc home directories. -->
