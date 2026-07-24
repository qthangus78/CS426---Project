# FieldFlow Manual Test Checklist

Use a configured Android emulator or device. The current reference target is the existing Medium Phone API 33 emulator when available.

## Launch

- [ ] Install the debug build.
- [ ] Launch FieldFlow.
- [ ] Confirm the Dashboard appears without a crash.
- [ ] Confirm the FieldFlow title appears.

## Dashboard

- [ ] Confirm the first visible Dashboard section is a polished FieldFlow brand area.
- [ ] Confirm Dashboard typography has a clear hierarchy: app bar, product name, section titles, card titles, metadata, and button text.
- [ ] Confirm `Architecture Bootstrap` appears.
- [ ] Confirm the bootstrap badge is visually secondary to the FieldFlow product title.
- [ ] Tap the Dashboard information action.
- [ ] Confirm the About FieldFlow dialog opens.
- [ ] Confirm the About FieldFlow dialog closes normally.
- [ ] Confirm the Continue inspection card shows the current in-progress sample inspection, `Computer Lab I.44`.
- [ ] Confirm the hero item is derived from the current summaries, preferring in-progress work.
- [ ] Confirm the Continue inspection hero looks like the primary action area and the `Resume` action is visually prominent.
- [ ] Tap `Resume` in the Continue inspection card.
- [ ] Confirm the Inspection screen opens for `Computer Lab I.44`.
- [ ] Press Back and confirm the Dashboard returns.
- [ ] Confirm `Inspection overview` appears.
- [ ] Confirm the overview values match all current inspection data: 3 total, 1 in progress, and 1 sync pending with the current sample repository.
- [ ] Confirm overview metric tiles look like polished KPI cards and do not introduce unsupported metrics.
- [ ] Confirm Quick actions shows Assets, Templates, Issues, and Reports as compact clickable cards.
- [ ] Confirm quick action cards look clickable and balanced, with readable title and supporting text.
- [ ] Confirm the All filter shows `Computer Lab I.44`, `Projector P-204`, and `Laboratory A2 Safety Check`.
- [ ] Confirm the In progress filter shows `Computer Lab I.44`.
- [ ] Confirm the Not started filter shows `Projector P-204`.
- [ ] Confirm the Sync pending filter shows `Laboratory A2 Safety Check`.
- [ ] Confirm the filters change only the visible inspection list.
- [ ] Confirm the filtered empty state remains readable in preview or alternate data.
- [ ] Confirm at least one inspection card appears.
- [ ] Confirm each card shows a status label and item progress.
- [ ] Confirm inspection cards use refined surfaces, spacing, progress styling, and navigation affordance.
- [ ] Confirm full inspection cards are clickable, not just the `Open` label.
- [ ] Confirm long inspection titles remain readable and do not cover the status or progress.
- [ ] Confirm the no-inspection empty state remains readable in preview or alternate data.
- [ ] Confirm portrait layout on Medium Phone API 33 looks polished and easy to scan.

## Inspection Navigation

- [ ] Tap `Computer Lab I.44`.
- [ ] Confirm the Inspection screen appears.
- [ ] Confirm the selected inspection title appears.
- [ ] Confirm Back returns to Dashboard.
- [ ] Open each visible inspection card and confirm it opens the matching Inspection screen.

## Placeholder Navigation

- [ ] Open Assets and press Back.
- [ ] Open Templates and press Back.
- [ ] Open Issues and press Back.
- [ ] Confirm Assets, Templates, and Issues still present honest placeholder boundaries.

## Reports

- [ ] Open Reports.
- [ ] Confirm the polished `Inspection reports` placeholder appears.
- [ ] Confirm Reports typography, empty-state composition, and future-capability cards look finished as a placeholder.
- [ ] Confirm future capabilities are presented as future work, not real reports.
- [ ] Confirm no fake report data appears.
- [ ] Press Back and confirm the Dashboard returns.

## Stability

- [ ] Repeat Dashboard-to-Inspection navigation twice.
- [ ] Repeat Quick access navigation across all placeholders.
- [ ] Repeat Dashboard-to-Reports navigation twice.

## Theme And Responsiveness

- [ ] Confirm Dashboard and Reports look correct in light mode.
- [ ] Confirm Dashboard and Reports look correct in dark mode.
- [ ] Confirm Dashboard and Reports remain usable in portrait.
- [ ] Rotate to landscape and confirm the Dashboard does not crash.
- [ ] Rotate to landscape and confirm Reports does not crash.
- [ ] Test a narrow screen width and confirm Dashboard filters, cards, and Reports text remain readable.
- [ ] Return to portrait and confirm the Dashboard remains readable.
- [ ] Return to portrait and confirm Reports remains readable.
- [ ] Confirm the empty Dashboard state is covered by the Dashboard presenter test and Compose preview.
- [ ] Increase font scaling where practical and confirm the Dashboard remains usable.
- [ ] Increase font scaling where practical and confirm Reports remains usable.
- [ ] Trigger normal activity recreation where practical.
- [ ] Confirm the app remains responsive and no crash dialog appears.
