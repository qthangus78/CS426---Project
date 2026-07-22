# FieldFlow Manual Test Checklist

Use a configured Android emulator or device. The current reference target is the existing Medium Phone API 33 emulator when available.

## Launch

- [ ] Install the debug build.
- [ ] Launch FieldFlow.
- [ ] Confirm the Dashboard appears without a crash.
- [ ] Confirm the FieldFlow title appears.

## Dashboard

- [ ] Confirm the first visible Dashboard section is a polished FieldFlow brand area.
- [ ] Confirm `Architecture Bootstrap` appears.
- [ ] Confirm the bootstrap badge is visually secondary to the FieldFlow product title.
- [ ] Confirm the Continue inspection card shows the current in-progress sample inspection, `Computer Lab I.44`.
- [ ] Tap `Resume` in the Continue inspection card.
- [ ] Confirm the Inspection screen opens for `Computer Lab I.44`.
- [ ] Press Back and confirm the Dashboard returns.
- [ ] Confirm `Inspection overview` appears.
- [ ] Confirm the overview values match all current inspection data: 3 total, 1 in progress, and 1 sync pending with the current sample repository.
- [ ] Confirm Quick actions shows Assets, Templates, Issues, and Reports as compact clickable cards.
- [ ] Confirm the All filter shows `Computer Lab I.44`, `Projector P-204`, and `Laboratory A2 Safety Check`.
- [ ] Confirm the In progress filter shows `Computer Lab I.44`.
- [ ] Confirm the Not started filter shows `Projector P-204`.
- [ ] Confirm the Sync pending filter shows `Laboratory A2 Safety Check`.
- [ ] Confirm a filtered empty state is covered by Dashboard tests and remains readable in preview or alternate data.
- [ ] Confirm at least one inspection card appears.
- [ ] Confirm each card shows a status label and item progress.
- [ ] Confirm long inspection titles remain readable and do not cover the status or progress.
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
- [ ] Open Reports and press Back.
- [ ] Confirm each placeholder says `Not implemented yet.`

## Stability

- [ ] Repeat Dashboard-to-Inspection navigation twice.
- [ ] Repeat Quick access navigation across all placeholders.
- [ ] Rotate to landscape and confirm the Dashboard does not crash.
- [ ] Return to portrait and confirm the Dashboard remains readable.
- [ ] Confirm the empty Dashboard state is covered by the Dashboard presenter test and Compose preview.
- [ ] Increase font scaling where practical and confirm the Dashboard remains usable.
- [ ] Trigger normal activity recreation where practical.
- [ ] Confirm the app remains responsive and no crash dialog appears.
