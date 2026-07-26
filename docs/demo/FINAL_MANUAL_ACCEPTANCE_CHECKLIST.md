# FieldFlow Final Manual Acceptance Checklist

This checklist is for repository-owner runtime confirmation in Android Studio. It is derived from the proposal and the current implementation. Each item requires manual owner confirmation; CI and unit tests do not prove these runtime behaviors.

## Setup

- [ ] Confirm Gradle sync completes in Android Studio.
- [ ] Select a local emulator or physical device.
- [ ] Install/run the debug app from Android Studio.
- [ ] Confirm the app launches to Dashboard without a crash, ANR, or blank first screen.

## Dashboard

- [ ] Confirm Dashboard shows FieldFlow branding, overview metrics, a continue-inspection card, quick access destinations, filters, and inspection summaries.
- [ ] Confirm Dashboard summaries show statuses for in-progress, not-started, and sync-pending sample inspections.
- [ ] Confirm status filters update the visible list without changing the underlying inspection data.
- [ ] Confirm the About dialog opens and closes.

## Navigation Destinations

- [ ] Open Dashboard from app launch.
- [ ] Open Inspection from an inspection summary.
- [ ] Open Assets from quick access and return with Back.
- [ ] Open Templates from quick access and return with Back.
- [ ] Open Issues from quick access and return with Back.
- [ ] Open Reports from quick access and return with Back.
- [ ] Repeat destination navigation and confirm no duplicate screens, lost Back behavior, blank screens, crash, or ANR.

## Asset Flow

- [ ] Confirm Assets is navigable.
- [ ] Confirm Assets is clearly presented as a placeholder in the current runtime app.
- [ ] Confirm the UI does not claim asset creation/editing is production-complete.

## Template Flow

- [ ] Confirm Templates is navigable.
- [ ] Confirm Templates is clearly presented as a placeholder in the current runtime app.
- [ ] Confirm the UI does not claim template authoring is production-complete.

## Inspection Start, Edit, Save, Review, Complete

- [ ] Open `Computer Lab I.44`.
- [ ] Confirm checklist sections and items render.
- [ ] Change answers for required and optional items.
- [ ] Enter measured values where available.
- [ ] Mark an item Not Applicable and confirm score/progress behavior remains coherent.
- [ ] Add or edit notes.
- [ ] Save draft and confirm the screen remains stable.
- [ ] Move to review.
- [ ] Confirm review shows validation status and item states.
- [ ] Complete a valid inspection.
- [ ] Confirm completed state shows score and issue-creation summary.
- [ ] Press Back and confirm Dashboard returns.

## Validation Failures

- [ ] Leave a required item unanswered and confirm completion is blocked.
- [ ] Fail a critical item without evidence and confirm completion is blocked.
- [ ] Add evidence for the critical failure and confirm validation no longer reports missing critical evidence.
- [ ] Confirm validation messages are understandable and do not claim hidden or unavailable behavior.

## Scoring

- [ ] Confirm completed score is shown as a percentage.
- [ ] Confirm Not Applicable items do not behave like failures.
- [ ] Confirm score changes after changing pass/fail/measured answers.

## Evidence

- [ ] Confirm evidence entry can be represented in the inspection workflow as the implemented evidence-reference behavior.
- [ ] Confirm the runtime app does not claim real camera, gallery, upload, or file-storage behavior unless the owner wires and tests that adapter manually.
- [ ] Confirm critical-failure evidence validation behaves correctly.

## Maintenance Issues

- [ ] Complete an inspection with a failed critical item that has evidence.
- [ ] Confirm the completed screen reports maintenance issue creation.
- [ ] Open Issues and confirm the current runtime UI remains an honest placeholder.
- [ ] Do not claim issue-list lifecycle management is runtime-complete unless it is separately wired and manually verified.

## Draft Recovery

- [ ] Save an inspection draft.
- [ ] Reopen the same inspection without killing the process and confirm changes are retained.
- [ ] Restart the app process and manually confirm the Room-backed runtime restores the saved draft.
- [ ] Confirm no product UI exposes a Demo/Room repository selector.

## Offline Behavior And Pending Sync

- [ ] Confirm the app launches and works without requiring an active backend login.
- [ ] Confirm sync-pending sample status is visible on Dashboard.
- [ ] Confirm no UI claims real cloud synchronization is production-complete.
- [ ] Restart the app and confirm pending-sync status remains visible.

## Reports And Export

- [ ] Open Reports.
- [ ] Confirm report history is not shown as fake completed data.
- [ ] Confirm PDF/JSON export is not presented as working runtime functionality unless adapters are manually wired and tested.
- [ ] Confirm the Reports placeholder accurately describes current scope.

## Back Navigation

- [ ] Press Back from Inspection and return to Dashboard.
- [ ] Press Back from Assets, Templates, Issues, and Reports and return to Dashboard.
- [ ] Repeat Back navigation after several destination switches.

## Display Modes

- [ ] Confirm Dashboard and Inspection work in light mode.
- [ ] Confirm Dashboard and Inspection work in dark mode.
- [ ] Confirm Dashboard and Inspection work in portrait.
- [ ] Confirm Dashboard and Inspection work in landscape.
- [ ] Confirm narrow display widths keep text and buttons readable.
- [ ] Increase system font size and confirm key screens remain usable.

## Stability

- [ ] Confirm no crash dialog appears during the full manual pass.
- [ ] Confirm no ANR appears during the full manual pass.
- [ ] Confirm no fake/demo data is presented as production backend data.
