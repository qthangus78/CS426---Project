# Prompt Examples (v0.2)

Token-efficient prompt patterns for FieldFlow. `CLAUDE.md` auto-loads the repo
rules, so you never need to reference `CLAUDE.md` or `AGENTS.md` in a prompt.
Naming the owner module and the done condition is usually enough.

## What Actually Saves Tokens

1. Name the **owner module** - stops the agent from reading across modules. Biggest lever in a multi-module repo.
2. Name the **specific file** when you know it - "edit `ObserveInspectionUseCase.kt`" beats "fix inspection logic" (no grep hunt).
3. State **done when** + verify scope - avoids a full build when `:domain:test` is enough.

Mentioning `CONTEXT.md` section "<X> work" is an optional extra: it points the
agent at the right read-set immediately. Skip it for small, obvious tasks.

## 1. Small task, known file - go direct

```
Rename getUserName to getUsername in ObserveInspectionUseCase.kt and its callers.
```

No read-set, no docs. File + action = cheapest possible prompt.

## 2. Domain work

```
Goal: add ObserveInspectionSummary use case returning total pass/fail per inspection
Owner module: :domain
Done when: :domain:test green, tests for empty and mixed pass/fail
```

## 3. Data work

```
Goal: make FakeInspectionRepository also return one IN_PROGRESS inspection
Owner module: :data
Done when: :data:testDebugUnitTest green
```

## 4. Feature UI

```
Goal: show a pass/fail/pending status badge on each Dashboard row
Owner module: :feature:dashboard
Constraint: reuse an existing core/designsystem component, don't create a new one if one exists
Done when: :feature:dashboard:testDebugUnitTest green
```

## 5. Cross-module / ambiguous - split roles, plan first

```
Goal: add a flow to mark an inspection "completed" from the Inspection screen
This is cross-module (feature -> domain -> data). Use planner/implementer/verifier.
Planner reads CONTEXT.md + MODULE_GRAPH.md and proposes the contract before code.
Stop for my review of the plan before implementing.
```

Allow wider reads here, but keep control by forcing a stop at the plan.

## 6. Investigation / Q&A - no edits

```
Explain the data flow from Dashboard to the rendered inspection list. Answer only, don't edit.
Owner: feature/dashboard + domain
```

## When To Use The Full Template

The 8-line template in `CONTEXT.md` (Goal / Context / Owner / Allowed /
Forbidden / Expected / Constraints / Verification / Done when) is for
cross-module or easy-to-derail work. Everyday tasks need only 3-4 lines like the
examples above.

## Operating Tips

- New task -> new session. Old history is re-billed every turn; use `/clear` when switching work.
- Don't restate rules already in the docs (e.g. "don't let domain import Compose") - they are already loaded.
- Large/unclear -> ask for planner/implementer/verifier so investigation context stays out of the main thread.
- Small/clear -> go direct, don't ask the agent to read the whole read-set.
