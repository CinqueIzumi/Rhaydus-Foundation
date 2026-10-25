---
paths:
  - "**/presentation/**"
  - "core/component/**"
  - "core/designsystem/**"
---

# Compose UI and the design system

- Before designing or modifying a UI surface, consult `foundation/docs/design-system-foundations.md` (theme
  plumbing, layout primitives, shared component catalog, editorial role contract) and the app's design-system
  doc.
- **Maintenance rule (enforced by review).** Any change that introduces, retires, or alters a foundation,
  component, or pattern in the design system updates its home in the same change. A localized tweak to one
  screen that does not change the system needs no update.
- Every user-facing string follows the app's voice doc (enforced by review).
- The stateless render is side-effect free: platform effects leave through a `UiEvent` collected at the screen
  top.
- Check `foundation/docs/CAPABILITIES.md` before hand-rolling a component, modifier or util.

## Project lines

<!-- The app's own UI rules: component home, token usage, copy doc, pattern docs. -->
