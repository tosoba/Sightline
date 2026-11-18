# 16: Convert core:domain to KMP

**Spec:** `.scratch/kmp-ios-migration/specs/module-architecture.md`

**What to build:** `core:domain` (repository interfaces) becomes a multiplatform module in `commonMain` on top of `core:model`, with the Android app consuming it unchanged.

**Blocked by:** 15 (model KMP)

**Status:** ready-for-agent

- [ ] `core:domain` interfaces compile in `commonMain` against `core:model`.
- [ ] Android consumes `core:domain` unchanged.
- [ ] Android compiles and tests pass.
