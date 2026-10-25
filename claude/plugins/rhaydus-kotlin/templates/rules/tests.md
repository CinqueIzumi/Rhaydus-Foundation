---
paths:
  - "**/commonTest/**"
  - "**/*Test/**"
---

# Tests

- Test code is written only by `rhaydus-kotlin:test-writer`, from a brief following the project's brief doc.
- Structure: `foundation/docs/code-style.md` § Test Class Organization and § Unit Test Structure; the
  review-only test rules are in the app's code-style doc.
- A test earns its place only when input decides output: a mapper, formatter, collector, a `resolve` with a
  real branch, or a fixture list that must cover every variant. Never assert a value you can only get by
  copying a literal out of production code (a style table, a constructor default).
- Every test passes all of these checks, whether it is being written or audited:
  - **Behaviour, not implementation.** If a refactor that keeps behaviour unchanged would break the test, it
    is in the way. Assert observable output, not call order or `verify` on an internal seam.
  - **It would be missed.** Delete a test if removing it leaves release confidence unchanged, such as one on
    a trivial pass-through, a getter or a data-class copy.
  - **Not a copy.** If two tests drive the same path under different names or near-identical fixtures, keep
    one. Parameterise only where the code branches on the inputs.
  - **Keep what guards production harm,** even if the test is plain: auth, migrations, sync merges.
  - **Fast.** Use virtual time (`runTest`, test dispatchers), never a real `delay`, real dispatcher or
    wall-clock wait.
- Test sources are gated by ktlint and detekt like main sources.
- Never write `!!`; unwrap with `shouldNotBeNull()`.

## Project lines

<!-- The app's own test rules: fakes and mocking library, device suite, allowed test dependencies. -->
