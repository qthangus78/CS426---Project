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
- [ ] Open Settings from the Dashboard top app bar.

## Navigation Destinations

- [ ] Open Dashboard from app launch.
- [ ] Open Inspection from an inspection summary.
- [ ] Open Assets from quick access and return with Back.
- [ ] Open Locations from quick access and return with Back.
- [ ] Open Templates from quick access and return with Back.
- [ ] Open Issues from quick access and return with Back.
- [ ] Open Reports from quick access and return with Back.
- [ ] Repeat destination navigation and confirm no duplicate screens, lost Back behavior, blank screens, crash, or ANR.

## Asset Flow

- [ ] Confirm Assets loads a Room-backed asset list.
- [ ] Search Assets by name, code, and location; confirm clear-search restores the full list.
- [ ] Confirm a no-search-results state is shown separately from the no-assets state.
- [ ] Confirm the empty state is understandable when no assets exist.
- [ ] Add an asset with a name, optional code, and location.
- [ ] Confirm asset validation rejects a blank name and missing/stale location.
- [ ] Confirm duplicate asset code validation is understandable.
- [ ] Open Asset details and confirm name, code, location, and next-inspection information are shown.
- [ ] Edit an asset and confirm the saved changes appear after Back navigation.
- [ ] Restart the app and confirm the saved asset persists.
- [ ] Confirm the Dashboard start-inspection selector includes the saved asset.
- [ ] Start an inspection from Asset details with a selected template.
- [ ] Confirm Back from Add asset, Edit asset, and Asset details returns to the expected previous screen.

## Location Flow

- [ ] Confirm Locations loads a Room-backed location list.
- [ ] Search Locations by name and parent location; confirm clear-search restores the full list.
- [ ] Confirm a no-search-results state is shown separately from the no-locations state.
- [ ] Confirm the empty state is understandable when no locations exist.
- [ ] Add a location with a valid name and optional parent location.
- [ ] Confirm location validation rejects a blank name and stale parent location.
- [ ] Open Location details and confirm name and parent information are shown.
- [ ] Edit a location and confirm the saved changes appear after Back navigation.
- [ ] Restart the app and confirm the saved location persists.
- [ ] Confirm a saved location appears in the Asset editor location selection.
- [ ] Confirm Back from Add location, Edit location, and Location details returns to the expected previous screen.
- [ ] Confirm no destructive delete action is offered for locations that could break existing asset history.

## Template Flow

- [ ] Confirm Templates loads a Room-backed template list.
- [ ] Search Templates by title or visible metadata; confirm clear-search restores the full list.
- [ ] Confirm a no-search-results state is shown separately from the no-templates state.
- [ ] Confirm the empty state is understandable when no templates exist.
- [ ] Open Template details and confirm version, recurrence, sections, checklist items, required, critical, weight, and answer type information are shown.
- [ ] Add a template with an initial section and checklist item.
- [ ] Confirm template creation offers only supported answer types for the current runtime.
- [ ] Confirm template validation rejects a blank name, blank initial section/item, invalid recurrence, and invalid weight.
- [ ] Edit existing template metadata and confirm existing sections/items remain visible after save.
- [ ] Restart the app and confirm the saved template persists.
- [ ] Confirm the Dashboard start-inspection selector includes the saved template.
- [ ] Start an inspection from Template details with a selected asset.
- [ ] Confirm Back from Add template, Edit template, and Template details returns to the expected previous screen.

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
- [ ] Open Issues and confirm the issue created from inspection completion appears.
- [ ] Confirm Issues list loading, empty, content, and retry/error behavior where testable.
- [ ] Apply All, Active, Resolved, and Critical filters and confirm the visible list changes without changing issue data.
- [ ] Open Issue details and confirm title, description, severity, status, asset, inspection, created date, and updated date are shown.
- [ ] Confirm only valid next status transitions are offered.
- [ ] Move an issue from Open to In progress, then to Resolved, then to Closed, and confirm each change persists.
- [ ] Confirm invalid transitions are unavailable or rejected with an understandable message.
- [ ] Restart the app and confirm the issue status is retained.
- [ ] Confirm stale or missing issue detail navigation shows a recoverable state instead of crashing.
- [ ] Confirm Back from Issues and Issue details returns to the expected previous screen.

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
- [ ] Confirm completed inspections appear as report candidates.
- [ ] Search Reports by inspection title, status, export filename, format, or generated date; confirm clear-search restores the full list.
- [ ] Confirm a no-search-results state is shown separately from the no-reports state.
- [ ] Confirm incomplete inspections do not appear as report candidates.
- [ ] Open Report details and confirm real asset, template, completion time, score, checklist, evidence-reference, and issue data are shown.
- [ ] Export JSON and confirm the export completes without duplicate export actions while saving.
- [ ] Open the JSON file and confirm it contains valid report content.
- [ ] Export PDF and confirm the export completes without duplicate export actions while saving.
- [ ] Open the PDF and confirm the report title, metadata, checklist summary, issue summary, evidence references, and page content are readable.
- [ ] Use long notes or issue text where available and confirm PDF content flows across pages without clipped text.
- [ ] Confirm a successful export appears in Export history.
- [ ] Restart the app and confirm export history is retained.
- [ ] Use Open report from a history entry or recent export.
- [ ] Use Share report from a history entry or recent export.
- [ ] Confirm export cancellation/failure behavior where testable does not create a successful history row.
- [ ] Confirm stale or missing exported files show an understandable error instead of a crash.

## Back Navigation

- [ ] Press Back from Inspection and return to Dashboard.
- [ ] Press Back from Assets, Locations, Templates, Issues, Reports, Settings, and their detail/editor screens and return to the expected previous screen.
- [ ] Repeat Back navigation after several destination switches.

## Settings And Appearance

- [ ] Open Settings.
- [ ] Confirm Use system setting, Light, and Dark are shown with the correct selected state.
- [ ] Switch to Light and confirm the app theme changes immediately.
- [ ] Switch to Dark and confirm the app theme changes immediately.
- [ ] Switch to Use system setting and confirm the app follows the OS appearance.
- [ ] Restart the app and confirm the selected appearance preference is retained.
- [ ] Confirm Back from Settings returns to Dashboard.
- [ ] Confirm Settings does not expose developer controls, repository selection, backend settings, or fake toggles.

## Display Modes

- [ ] Confirm Dashboard and Inspection work in light mode.
- [ ] Confirm Assets, Locations, Templates, Issues, Reports, and Settings work in light mode.
- [ ] Confirm Dashboard and Inspection work in dark mode.
- [ ] Confirm Assets, Locations, Templates, Issues, Reports, and Settings work in dark mode.
- [ ] Confirm Dashboard and Inspection work in portrait.
- [ ] Confirm Assets, Locations, Templates, Issues, Reports, and Settings work in portrait.
- [ ] Confirm Dashboard and Inspection work in landscape.
- [ ] Confirm Assets, Locations, Templates, Issues, Reports, and Settings work in landscape.
- [ ] Confirm narrow display widths keep text and buttons readable.
- [ ] Increase system font size and confirm key screens remain usable.

## Stability

- [ ] Confirm no crash dialog appears during the full manual pass.
- [ ] Confirm no ANR appears during the full manual pass.
- [ ] Confirm no fake/demo data is presented as production backend data.
- [ ] Confirm no duplicate asset/template records appear after app restart.
- [ ] Confirm no duplicate report-history records appear after app restart.
- [ ] Confirm stale or removed asset/template/issue/report detail links show a recoverable not-found state instead of crashing.
- [ ] Confirm no seminar, architecture, Room, DAO, repository, or developer wording appears in product UI.
- [ ] Confirm repeated Assets/Templates/Issues/Reports navigation does not create duplicate screens, blank screens, crash, or ANR.
