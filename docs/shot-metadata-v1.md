# Shot Metadata V1

## Product Goal

Shot capture remains objective and fast: connect a scale, record the shot, and save its measured data immediately. Subjective and setup metadata is optional and is added after the shot has been saved.

This metadata gives each saved shot useful context for comparison. Over time, it can support bean-specific learning, such as identifying which yield and grind settings produced the best-rated shots for a particular coffee.

## V1 Fields

All metadata fields are nullable and optional. None is required to save a shot.

- `rating`: integer from 1 to 5
- `tasteDirection`: `sour`, `balanced`, or `bitter`; `null` means not judged yet
- `grindSetting`: decimal number, such as `8.1` or `9.0`
- `beanName`: free-text bean name
- `notes`: short free-text notes

Invalid values should be rejected when entered, but absent metadata is always valid.

## Capture And Editing UX

1. Capture and save the objective shot data immediately.
2. Offer metadata editing in the selected shot detail or a post-save edit area.
3. Allow metadata to be added, changed, or cleared later.

Metadata entry must never block capture, delay saving, or become a prerequisite for starting another shot.

## Implementation Notes

- Keep the existing shot capture JSON contract stable where possible.
- Add metadata fields rather than replacing or renaming existing shot fields.
- Use nullable or optional values so existing saved shots remain readable.
- Store `grindSetting` in a way that preserves user-entered decimal precision; avoid lossy floating-point display surprises.
- Avoid migrations or parsing assumptions that would break shots saved before metadata existed.
- Preserve current capture, history, selected-detail, and raw JSON behavior.
- Keep metadata concerns outside BLE and `ShotCaptureEngine` logic.

## Future Extensions

These are intentionally deferred beyond V1:

- Structured bean database
- Roaster information
- Roast date
- Grinder profiles
- Best-shot comparison per bean
- Recommendation engine for yield and grind adjustments

## V1 Non-Goals

- No full bean database
- No recommendations
- No charts
- No cloud sync
- No mandatory metadata
- No BLE or capture-engine changes

V1 establishes only the smallest optional metadata set needed to annotate saved shots. Structured coffee management and automated learning remain future product work.
