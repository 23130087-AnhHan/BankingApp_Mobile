# Android WiFi Debugger

## One-click use

1. Connect the Android phone to the Windows PC by USB.
2. Unlock the phone and enable USB debugging.
3. Double-click `Demo.bat`.
4. Accept the USB debugging authorization dialog when Android shows it.
5. Wait for `READY TO UNPLUG USB`.
6. Unplug USB. Keep the console open while using Android Studio.

The tool locates Android SDK Platform-Tools automatically, switches the authorized USB device to classic ADB TCP/IP mode, discovers its active IPv4 address, verifies the Wi-Fi transport and monitors it. Logs are written to `logs\adb.log`.

## Network requirements

For classic TCP/IP mode, the PC must be able to reach the phone on TCP port 5555. This works on normal Wi-Fi and most hotspot arrangements. Some guest Wi-Fi networks enable client isolation and intentionally block device-to-device traffic.

Android 11+ Wireless Debugging uses a pairing code the first time a PC is paired. Android requires the code to be confirmed by a person; no safe script can bypass that platform security prompt. Once paired, this tool discovers and reconnects the advertised endpoint automatically.

## Security

ADB provides powerful access to the phone. Use this tool only on trusted private networks. Disable Wireless debugging or restart the phone when finished if the device should no longer accept network debugging connections.

