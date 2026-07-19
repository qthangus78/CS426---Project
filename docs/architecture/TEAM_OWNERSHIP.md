# Team Ownership

Members should work in separate feature branches and open reviewed PRs for integration. Contracts in `:domain` and `:core:navigation` must be changed through review because multiple teams build against them.

No feature may import Data implementations. App integration should happen after each large ownership milestone, not after every tiny class.

## Owners

### Thắng

Owns:

- root Gradle architecture;
- `:app`;
- `:core:navigation`;
- `:feature:inspection`;
- Circuit foundation;
- DI composition root;
- final integration.

Normally approves significant changes to:

- `settings.gradle.kts`;
- root `build.gradle.kts`;
- `gradle/libs.versions.toml`;
- `:app`;
- `:core:navigation`;
- `:feature:inspection`.

### Huy

Owns:

- `:domain`;
- Domain models;
- Use Cases;
- business rules;
- validation;
- scoring;
- lifecycle;
- Domain tests.

Normally approves significant changes to:

- `:domain`;
- Domain-facing contracts used by repositories and presenters.

### Lĩnh

Owns:

- `:data`;
- `:core:database`;
- Room;
- repositories;
- mappings;
- evidence storage adapters;
- offline-first behavior;
- fake synchronization.

Normally approves significant changes to:

- `:data`;
- `:core:database`;
- future local persistence and storage adapters.

Room and offline-first persistence are planned but not implemented in the Architecture Bootstrap.

### Linh

Owns:

- `:feature:dashboard`;
- `:feature:reports`;
- `:core:designsystem`;
- README and documentation;
- slides;
- demo preparation.

Normally approves significant changes to:

- `:feature:dashboard`;
- `:feature:reports`;
- `:core:designsystem`;
- repository documentation.

## Shared Or Assigned Later

The following modules are shared or assigned later:

- `:feature:assets`;
- `:feature:templates`;
- `:feature:issues`.

Until owners are assigned, changes to these modules should be reviewed by Thắng for architecture boundaries and by the future feature owner when assigned.

## Workflow Rules

- Work in separate feature branches.
- Keep module boundaries intact.
- Change shared contracts only through reviewed PRs.
- Do not import `FakeInspectionRepository` outside `:app` or `:data` tests.
- Do not add Room, file storage, PDF, networking, sync, or auth code until the owner milestone requires it.
- Integrate in `:app` after meaningful feature milestones, not after every tiny class.
