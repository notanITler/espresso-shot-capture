# Half Decent Scale BLE Notes

Research date: 2026-06-11

Reference repository: `decentespresso/openscale`, commit `9d4ac4c31b1027a9a6a5a484e15bf59e59386ce6`.

These notes document facts found in the OpenScale source only. Anything not directly confirmed from source is listed under unknowns for hardware testing.

## BLE discovery and GATT contract

Source files:
- `include/config.h`
- `include/ble.h`

Findings:
- Advertised device name: `Decent Scale`.
- Service UUID: `fff0`.
- Read/notify characteristic UUID: `fff4`.
- Write characteristic UUID: `36f5`.
- The scale advertises the `fff0` service UUID.
- The read characteristic is created with `PROPERTY_READ | PROPERTY_NOTIFY`.
- The write characteristic is created with `PROPERTY_WRITE`.
- The firmware stops advertising after one central connects and restarts advertising after disconnect.

## Weight notifications

Source files:
- `include/ble.h`
- `include/parameter.h`
- `src/hds.ino`

Findings:
- Weight notifications are sent on characteristic `fff4`.
- BLE weight packet length is 7 bytes in the current `buildWeightPacket` implementation.
- Packet shape from `buildWeightPacket`:
  - byte 0: model byte, always `0x03` for Decent scales
  - byte 1: message type `0xCE` for weight
  - bytes 2-3: signed weight in grams multiplied by 10, big endian
  - bytes 4-5: `0x00 0x00`
  - byte 6: XOR checksum from `calculateXOR`
- Weight encoding clamps non-finite values to `0.0`, caps high values at `32767` after `grams * 10`, and caps low values at `-32768` after `grams * 10`.
- Source comments also document older/newer examples:
  - firmware v1.0/v1.1: 7-byte weight messages
  - firmware v1.2 and newer: 10-byte weight messages with timestamp examples
- The current code path uses the 7-byte `buildWeightPacket`, so Android parsing should initially support 7-byte packets and treat the 10-byte format as a hardware-verification item.
- BLE notify interval is fixed at 100 ms / 10 Hz and is not runtime-configurable over BLE.

## Write commands

Source file:
- `include/ble.h`

Findings:
- Writes are handled by `MyCallbacks::onWrite`.
- All recognized Decent Scale BLE commands start with byte `0x03`.
- Tare command:
  - command type byte: `0x0F`
  - requires at least 7 bytes
  - checksum must validate
  - examples from source comments:
    - `03 0F 00 00 00 00 0C`
    - `03 0F B9 00 00 00 B5`
  - source comments also document `03 0F 00 00 00 01 0D` as a newer tare form that leaves heartbeat behavior unchanged.
- LED/display/power command type: `0x0A`.
  - `data[2] == 0x00`: LED/display off
  - `data[2] == 0x01`: LED/display on
  - `data[2] == 0x02`: power off
  - `data[2] == 0x03`: low-power control
  - `data[2] == 0x04`: soft-sleep control
  - `data[2] == 0x03`, `data[3] == 0xFF`, `data[4] == 0xFF`, `data[5] == 0x00`, `data[6] == 0x0A`: heartbeat update
- Timer command type: `0x0B`.
  - `data[2] == 0x03`: timer start
  - `data[2] == 0x00`: timer stop
  - `data[2] == 0x02`: timer zero/reset
- Other command types present in source:
  - `0x1A`: calibration
  - `0x1B`: start WiFi OTA
  - `0x1C`: buzzer control when compiled with buzzer support
  - `0x1D`: sample settings
  - `0x1E`: menu/about/debug display control
  - `0x1F`: reset
  - `0x20`: USB weight output
  - `0x21`: BLE gyro response when gyro support is compiled
  - `0x22`: voltage/battery response request
  - `0x25`: ADS1232 debug streaming over BLE

## Other notifications

Source file:
- `include/ble.h`

Findings:
- Voltage response is sent on `fff4` as a 7-byte packet with type `0x22`.
- Heartbeat response is sent on `fff4` as a 7-byte packet with type `0x0A`.
- Button notifications are sent on `fff4` as 7-byte packets with type `0xAA`.
- Power-off notifications are sent on `fff4` as 7-byte packets with type `0x2A`.
- LED response is sent on `fff4` as a 7-byte packet with type `0x0A`; this packet includes current weight, battery/charging byte, and firmware version bytes.
- ADS1232 debug notifications are sent on `fff4` as 41-byte packets when debug mode is enabled.

## WiFi notes relevant to BLE work

Source file:
- `README.md`

Findings:
- The README says the BLE interface is unauthenticated.
- The README says WiFi status is on-demand only and mirrors the Bluetooth side, where clients request battery with command `0x22`.
- WiFi weight snapshots use `{ "grams": 25.66, "ms": 12345 }`, but this is WiFi/WebSocket, not BLE.

## Unknowns to verify with physical scale

- Whether the shipped scale firmware emits 7-byte weight packets, 10-byte timestamped packets, or both.
- Whether Android sees UUIDs as short UUIDs expanded to `0000fff0-0000-1000-8000-00805f9b34fb`, `0000fff4-0000-1000-8000-00805f9b34fb`, and `000036f5-0000-1000-8000-00805f9b34fb`.
- Whether the advertised name is always exactly `Decent Scale` for the arriving unit.
- Whether notification subscription requires writing the standard CCCD descriptor and whether Android reports descriptor UUIDs in the expected way.
- Whether the `fff4` read value is useful before notifications are enabled.
- Whether command writes require write-with-response or write-without-response on Android.
- Whether the scale requires periodic heartbeat writes during an active Android connection.
- Whether tare should use the newer heartbeat-preserving form `03 0F 00 00 00 01 0D` for our app.
- Whether negative weights are emitted as signed two's-complement values in bytes 2-3 exactly as `encodeWeight` implies.

## Proposed first hardware smoke test

1. Scan for nearby BLE devices and log advertised names plus service UUIDs.
2. Confirm a device named `Decent Scale` advertising service `fff0`.
3. Connect and discover GATT services/characteristics.
4. Confirm service `fff0`, notify/read characteristic `fff4`, and write characteristic `36f5`.
5. Enable notifications on `fff4`.
6. Log raw notification packets as hex while the scale is empty, then while adding/removing a small weight.
7. Decode 7-byte `0x03 0xCE` packets as signed big-endian tenths of a gram and compare against the physical display.
8. Check whether any 10-byte `0x03 0xCE` packets appear and record their timestamp bytes if present.
9. Send tare only after passive reads work; start with the source-documented tare packet and verify the display returns to zero.
10. Record whether the connection remains stable for at least 30 seconds without heartbeat writes.
