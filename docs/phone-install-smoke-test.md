# Phone Install Smoke Test

This runbook covers installing the debug build on a physical Android phone and confirming the current root screen opens.

## Prerequisites

- Android Studio or Android platform tools are installed.
- A USB cable that supports data transfer is available.
- The phone has enough battery and is unlocked during setup.

## Enable Developer Options And USB Debugging

1. Open the phone's Settings app.
2. Go to About phone.
3. Tap Build number seven times until Developer Options are enabled.
4. Go back to Settings and open Developer Options.
5. Turn on USB debugging.

## Connect The Phone

1. Connect the phone to the computer with USB.
2. Keep the phone unlocked.
3. If the phone asks whether to allow USB debugging, choose Allow.
4. If the phone asks for a USB mode, choose File transfer or a similar data-transfer mode.

## Install The Debug Build

From the repository root, run:

```powershell
.\gradlew.bat installDebug
```

Wait for the install task to complete successfully.

## Launch The App

1. Open the app drawer on the phone.
2. Launch Espresso Shot Capture.

## Expected Result

The app opens to the Shot History screen and shows the empty state:

```text
No saved shots
```

## Troubleshooting Device Detection

If the phone is not detected:

- Unplug and reconnect the USB cable.
- Confirm the phone is unlocked.
- Confirm USB debugging is enabled.
- Accept the USB debugging prompt on the phone.
- Change the USB mode to File transfer or data transfer.
- Try another USB port or cable.
- Restart the adb server:

```powershell
adb kill-server
adb start-server
adb devices
```

If `adb devices` shows the phone as `unauthorized`, disconnect and reconnect the phone, then accept the debugging prompt.
