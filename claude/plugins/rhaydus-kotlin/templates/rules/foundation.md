---
paths:
  - "foundation/**"
---

# Working in the foundation submodule

- Work on a branch inside `foundation/`, never on a detached HEAD.
- Changes are generic and name no consuming app (`foundation/CLAUDE.md`).
- Use this project's `rhaydus-kotlin` agents there, not the agents the foundation's own `CLAUDE.md` names.
- Build with `foundation/gradlew -p foundation <task>`; the Gradle hook rewrites a bare `./gradlew` to the
  project root.
- Commit in the foundation first, then record the pointer in the app with `git add foundation`.
- Push the foundation commit before any app commit that points at it (`git push --recurse-submodules=check`
  refuses otherwise).
- The app merges to its main branch only with the pointer on a tagged foundation commit.
- The plugin installs from `foundation/` through the project-scoped `rhaydus` marketplace in
  `.claude/settings.json`. Claude Code runs a copy cached per plugin version, so an edit to its agents, hooks
  or skills applies only after `claude plugin uninstall rhaydus-kotlin@rhaydus --scope project`, the matching
  `install`, and `/reload-plugins`; `claude plugin update` is a no-op while the version is unchanged.

## Project lines

<!-- Anything else specific to how this app works inside the submodule. -->
