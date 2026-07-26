# FieldFlow Manual Test Checklist

Use Android Studio with a local emulator or device selected by the repository owner. Do not treat this checklist as already executed by CI.

## Launch

- [ ] Install or run the debug build from Android Studio.
- [ ] Launch FieldFlow.
- [ ] Confirm the Dashboard appears without a crash or ANR.
- [ ] Confirm the app uses the FieldFlow title and not starter template text.

## Dashboard

- [ ] Confirm overview metrics render clearly.
- [ ] Confirm the continue-inspection card shows the in-progress sample inspection.
- [ ] Tap the Dashboard information action and confirm the About dialog opens and closes.
- [ ] Confirm the All, In progress, Not started, and Sync pending filters update the visible list only.
- [ ] Confirm every inspection card shows a title, status, progress, and navigation affordance.
- [ ] Confirm long titles and metadata remain readable.

## Inspection Workflow

- [ ] Open `Computer Lab I.44`.
- [ ] Confirm the selected inspection title and status are shown.
- [ ] Change checklist answers between Pass, Fail, Not Applicable, and measured values where available.
- [ ] Add and edit item notes.
- [ ] Add an evidence reference label where the UI offers evidence entry.
- [ ] Save the draft and confirm the screen remains usable.
- [ ] Move to review and confirm answered, unanswered, and evidence states are represented honestly.
- [ ] Attempt completion with missing required answers and confirm validation blocks completion.
- [ ] Mark a critical item as Fail without evidence and confirm validation blocks completion.
- [ ] Add evidence for the critical failure and confirm completion is allowed after all required answers are valid.
- [ ] Confirm the completed screen shows a score and issue creation summary.
- [ ] Press Back and confirm navigation returns to Dashboard.

## Feature Destinations

- [ ] Open Assets and confirm it is an honest placeholder, then return.
- [ ] Open Templates and confirm it is an honest placeholder, then return.
- [ ] Open Issues and confirm it is an honest placeholder, then return.
- [ ] Open Reports and confirm report history/export are not shown as completed runtime features, then return.

## Runtime Limits To Confirm

- [ ] Confirm no UI claims that Assets, Templates, Issues, or Reports are production-complete.
- [ ] Confirm no fake report history, fake export progress, or fake downloaded file is shown.
- [ ] Confirm evidence entry is presented as a reference/demo behavior unless a real picker is manually wired.
- [ ] Save a draft, restart the app process, and confirm Room-backed draft recovery manually.
- [ ] Confirm sync-pending sample state remains visible after process restart.
- [ ] Confirm no UI exposes a Demo/Room repository selector.

## Navigation And Stability

- [ ] Repeat Dashboard-to-Inspection navigation twice.
- [ ] Navigate repeatedly between Dashboard, Assets, Templates, Issues, and Reports.
- [ ] Use Back from every destination.
- [ ] Rotate portrait to landscape and back.
- [ ] Confirm no duplicate destination, blank screen, crash, or ANR appears.

## Theme And Accessibility

- [ ] Confirm light mode renders all checked screens clearly.
- [ ] Confirm dark mode renders all checked screens clearly.
- [ ] Confirm a narrow display keeps buttons, filters, labels, and cards readable.
- [ ] Increase system font size and confirm key controls remain usable.
- [ ] Confirm common touch targets are reachable and not clipped.
