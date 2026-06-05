# Decisions

This file records major architectural and product decisions made so far.

## ADR-001 Android Only

The MVP targets Android only. This keeps platform scope narrow while the capture engine, scale lifecycle, and export contract are being developed.

## ADR-002 Half Decent Scale 1.1 Only For MVP

The MVP targets the Half Decent Scale 1.1 only. Supporting one known scale first reduces BLE protocol uncertainty and avoids building a generic scale abstraction before hardware behavior is verified.

## ADR-003 Flow Time Instead Of Brew Time

The app prioritizes flow time rather than total brew time. Flow time starts when meaningful weight flow is detected and better matches the measurement the engine can infer from scale data.

## ADR-004 Fully Automatic Start Detection

Shot capture starts automatically from scale samples. The MVP avoids manual start as the primary path so the shot record is based on detected coffee flow rather than user reaction time.

## ADR-005 Post-Target Recording Window

Recording continues after target yield is reached. This captures drift and overshoot, which are important for understanding final yield and post-target behavior.

## ADR-006 StartPolicy Separate From Engine

Start detection lives in `StartPolicy` instead of being embedded directly in `ShotCaptureEngine`. This keeps the rule independently testable and lets the engine remain focused on state transitions.

## ADR-007 StopPolicy Separate From Engine

Target reached and completion logic lives in `StopPolicy`. This makes the target/post-target window behavior explicit, reusable, and easy to test without full engine setup.

## ADR-008 JSON Export As Canonical External Contract

JSON export is the canonical external data contract. The root includes `schemaVersion` and `shot`, and null values are included so downstream tools can rely on stable field presence.

## ADR-009 No Room Before Engine Completion

Room persistence is deferred until the capture engine and `ShotDraft` lifecycle are stable. This avoids locking early engine assumptions into a database schema.

## ADR-010 No BLE Before Hardware Availability

BLE integration is deferred until hardware is available for validation. The core capture engine is being built and tested with plain Kotlin inputs first.
