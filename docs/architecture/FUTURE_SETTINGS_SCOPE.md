# Settings Scope

FieldFlow now implements a Settings destination for Appearance.

Implemented behavior:

- Settings navigation from the Dashboard top app bar;
- System, Light, and Dark appearance modes;
- persisted appearance preference through an app-layer SharedPreferences adapter;
- observable Domain preference port and use cases;
- immediate root-theme application through `FieldFlowTheme`.

## Remaining Future Settings

| Setting | Future behavior | Layers and modules involved | Required owner participation |
| --- | --- | --- | --- |
| Accessibility and text-display preferences | Offer app-level display preferences such as denser text, larger content, or reduced decorative motion after the team defines supported behavior. | Future Settings presentation, `:core:designsystem` typography/component behavior, `:app` application wiring, future preference persistence. | Linh for UI/design system, Thắng for app integration, Lĩnh for persistence if stored. |
| Synchronization preferences | Configure future sync behavior only after a real sync contract exists. | Future Settings presentation, future Domain sync policies/use cases, future Data sync adapters, `:app` scheduler/composition wiring, future persistence. | Linh for UI, Huy for Domain rules, Lĩnh for Data/sync implementation, Thắng for app integration. |
| Notification preferences | Configure future reminders or export/sync notifications only after notification behavior is approved. | Future Settings presentation, `:app` Android notification permission and scheduling integration, future Domain notification policy if needed, future persistence. | Linh for UI, Thắng for Android/app integration, Huy for policy if needed, Lĩnh for persistence if stored. |
| Demo-data mode | Let a future demo build switch between deterministic fake data and production adapters without feature modules constructing repositories. | Future Settings presentation, `:app` composition root, `:data` fake and production adapters, possibly Domain repository contracts if the mode affects policy. | Linh for UI, Thắng for composition root, Lĩnh for adapters, Huy if Domain contracts change. |
| Local-data reset | Provide a guarded reset of local data only after real local storage exists. | Future Settings presentation, future Domain reset use case or policy, `:data` and `:core:database` storage implementation, `:app` lifecycle/integration. | Linh for confirmation UI, Huy for Domain policy, Lĩnh for database/data reset, Thắng for app integration. |
| Application information | Show static app information such as architecture name, development state, and future version/build metadata. | Future Settings presentation and possibly `:app` for version/build metadata lookup. No Domain, Data, or persistence is needed unless the team adds real metadata contracts. | Linh for UI/copy, Thắng if app-module metadata is exposed. |

## Current Non-Goals

- Do not add notification preferences, synchronization preferences, account/profile settings, local-data reset, or fake setting values without approved product behavior.
- Do not expose repository selection, architecture modes, or demo-data mode in product UI.
- Do not route Dashboard About FieldFlow through Settings. The About dialog is local Dashboard presentation.
