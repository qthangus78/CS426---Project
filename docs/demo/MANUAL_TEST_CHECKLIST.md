# FieldFlow Manual Test Checklist

Use a configured Android emulator or device. The current reference target is the existing Medium Phone API 33 emulator when available.

## Launch

- [ ] Install the debug build.
- [ ] Launch FieldFlow.
- [ ] Confirm the Dashboard appears without a crash.
- [ ] Confirm the FieldFlow title appears.

## Dashboard

- [ ] Confirm `Architecture Bootstrap` appears.
- [ ] Confirm `Inspection overview` appears.
- [ ] Confirm at least one inspection card appears.
- [ ] Confirm each card shows a status label and item progress.
- [ ] Confirm Quick access shows Assets, Templates, Issues, and Reports.

## Inspection Navigation

- [ ] Tap `Computer Lab I.44`.
- [ ] Confirm the Inspection screen appears.
- [ ] Confirm the selected inspection title appears.
- [ ] Confirm Back returns to Dashboard.

## Placeholder Navigation

- [ ] Open Assets and press Back.
- [ ] Open Templates and press Back.
- [ ] Open Issues and press Back.
- [ ] Open Reports and press Back.
- [ ] Confirm each placeholder says `Not implemented yet.`

## Stability

- [ ] Repeat Dashboard-to-Inspection navigation twice.
- [ ] Repeat Quick access navigation across all placeholders.
- [ ] Rotate the device or trigger normal activity recreation where practical.
- [ ] Confirm the app remains responsive and no crash dialog appears.
