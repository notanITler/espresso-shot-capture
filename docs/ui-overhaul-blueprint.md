# UI Overhaul Blueprint: Capture-First MVP

## Purpose

The current MVP proves the end-to-end path for fake and real Decent Scale capture, but the working screen is too busy for real shot prep. The next UI pass should make capture the primary task, keep history useful, and move hardware/debug detail out of the main working area without removing it.

The visual direction should be a premium dark, instrument-like interface: high contrast, calm spacing, large live values, and clear action priority.

## Target Screen Structure

1. Capture screen
2. Shot history screen or section
3. Shot detail screen or section
4. Collapsed Debug / BLE section

For the MVP, this can remain one app surface if navigation is still intentionally deferred. The important change is hierarchy: capture first, history second, detail third, debug last and collapsed.

## Capture Screen Hierarchy

The capture screen should be arranged in this order:

1. Scale/source status
2. Dose / target / ratio
3. Large live shot values
4. Shot state
5. Tare + primary action
6. Recent history preview
7. Collapsed Debug / BLE

### Scale / Source Status

Show the selected source and readiness clearly:

- Fake/demo scale
- Decent Scale
- Disconnected
- Connecting
- Connected
- Receiving readings
- Error

The UI must not imply a real hardware connection when demo mode is active.

### Dose / Target / Ratio

Show the active recipe target near the top:

- Dose
- Target yield
- Ratio

Editable fields can remain compact. During a recording session, the active target should be visually stable and should represent the snapshotted target used for the shot.

### Large Live Shot Values

During prep and recording, the largest values on the screen should be the values the user needs while standing at the machine:

- Current weight
- Flow time
- Target progress
- Average flow, if available

These should be readable at a glance, with less important labels kept smaller.

### Shot State

The shot state should be explicit but not compete with live values:

- Ready
- Waiting for shot
- Recording
- Target reached
- Shot saved
- Error

State should explain what the app is waiting for or doing now.

### Tare + Primary Action

Tare is secondary but must always be easy to reach when a real scale is connected.

The primary action should be visually dominant and stable:

- Prepare capture
- Stop
- Save

Avoid button jumping. The user should not need to hunt for the next action during an actual shot.

### Recent History Preview

Keep a compact preview below the capture card:

- Newest shots first
- Small number of rows
- Useful summary only
- No raw JSON in the preview

The preview should confirm that a shot saved without pulling the user away from capture.

### Collapsed Debug / BLE

Debug and hardware discovery tools should be available but collapsed by default. The main capture workflow should not be visually dominated by scan results, raw packets, service UUIDs, or JSON.

## Capture Screen States

### Disconnected

Use when the selected real source is not connected or not ready.

Expected UI:

- Source/status indicates disconnected
- Tare disabled or hidden
- Primary capture action disabled for real source
- Demo source remains available if selected
- Debug/BLE can be expanded for troubleshooting

### Connected / Idle

Use when the selected scale source is connected and readings are available, but capture has not been prepared.

Expected UI:

- Source/status shows connected or receiving readings
- Live weight is visible
- Tare is available for real scale
- Primary action prepares capture

### Waiting For Shot

Use after the user prepares capture but before StartPolicy detects the shot.

Expected UI:

- State shows waiting for shot
- Live weight remains prominent
- Tare should generally be unavailable or visually de-emphasized once armed
- Primary action can cancel or stop, depending on final flow decision

### Recording

Use after the engine starts recording.

Expected UI:

- Live values are dominant
- Progress toward target is clear
- Target reached is visible when appropriate
- Primary action is Stop or Save, depending on final manual-stop semantics
- Buttons remain stable

### Shot Captured / Saved

Use after a shot is finalized and persisted.

Expected UI:

- Clear saved confirmation
- Compact summary of final yield and flow time
- Recent history updates below
- Return path to ready state should be calm and predictable

### Error

Use for source, connection, capture, or persistence failures.

Expected UI:

- Short clear error state
- Recovery action if available
- Debug/BLE section available for detail

## Action Priority

### Primary

Prepare capture / Stop / Save is the primary action. It should be the strongest visual action and should remain in a stable location.

### Secondary

Tare is secondary, but always easy to reach when the real Decent Scale is connected and writable. It should be disabled or hidden when unavailable.

### Debug

Debug/BLE actions are tertiary. Scan, connect diagnostics, raw packet display, service UUIDs, and parser details should be collapsed by default.

## Move Out Of Main Working Area

The following current information should move out of the main capture area:

- Full BLE scan result list
- GATT service/characteristic diagnostics
- Raw packet hex
- Advertised service UUIDs
- RSSI details for every nearby device
- Raw JSON payload
- Long selected shot debug blocks
- Low-level tare command status, unless currently sending or failed

Keep this information available in Debug / BLE or shot detail debug sections.

## History Direction

History should become useful without taking over the capture screen.

The compact history preview should show:

- Newest shots first
- Source: fake/demo or Decent Scale
- Final yield
- Flow time
- Sample count or confidence status

Raw JSON should not appear in the history preview.

## Shot Detail Direction

Shot detail should prioritize readable shot data:

- Source
- Dose
- Target yield
- Final yield
- Flow time
- Average flow
- Target reached
- Sample count
- Data confidence

Raw JSON remains secondary developer/debug information, ideally hidden behind a labeled collapsed area or placed below the readable summary in a bounded area.

## Debug / BLE Direction

Debug / BLE remains important during hardware bring-up, but it should not be the default visual mode.

Collapsed Debug / BLE should contain:

- Scan controls
- Candidate devices
- Connection state
- Latest raw packet
- Latest parsed weight
- Tare command status
- Service UUID diagnostics
- Error detail

## Implementation Phases

### Phase 1: Capture Card Layout + Collapsed Debug

Create the capture-first layout with a dominant capture card and move BLE/debug information into a collapsed section.

Goals:

- Capture becomes visually primary
- Debug is still available
- No capture logic changes
- No persistence changes

### Phase 2: Stable Action Bar / No Jumping Buttons

Make the action area predictable across states.

Goals:

- Tare remains easy to reach when real scale is connected
- Primary action stays in a stable location
- Recording controls do not jump during shot prep
- Disabled states are clear

### Phase 3: History Preview Cleanup

Make recent history compact and useful below capture.

Goals:

- Show newest useful rows only
- Keep summary fields concise
- Preserve selection behavior
- Keep raw JSON out of preview

### Phase 4: Shot Detail Cleanup

Make selected shot detail readable first, debug second.

Goals:

- Prioritize shot summary and confidence fields
- Keep raw JSON available but bounded/collapsed
- Avoid overwhelming the capture workflow

## Non-Goals For This Blueprint

- No implementation yet
- No capture logic changes
- No BLE/GATT changes
- No persistence/history behavior changes
- No removal of debug information
- No final design system
