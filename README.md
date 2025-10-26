# EasySpot

An Android app that allows you to turn on (or off) your hotspot remotely via Bluetooth - think Apple
Continuity, but for everyone!

## Requirements

- An Android phone
    - with Android 12 or higher
    - with Bluetooth Low Energy (BLE) advertising support
    - with [Shizuku](https://shizuku.rikka.app/) installed and running
- [The client program](./pyclient) on a computer with Bluetooth Low Energy
  or [nRF Connect Mobile](https://www.nordicsemi.com/Products/Development-tools/nRF-Connect-for-mobile)
  on another BLE-capable phone

## Setup

1. Install and set up [Shizuku](https://shizuku.rikka.app/)
2. Install the EasySpot app
   from [GitHub Releases](https://github.com/GGORG0/EasySpot/releases/latest)
3. Open the app and grant all permissions
4. Follow the instructions in [the client's README](./pyclient/README.md) to set up the client
   program

## TODO

- Android 11 and lower support
- More robust, Rust-based client with a system tray icon and WiFi auto-connection
- Android client
- More hotspot control methods for improved reliability on different OS versions
- About menu

## BLE API

This app exposes a BLE GATT service to allow remote control of the hotspot.

- Service UUID: `7baad717-1551-45e1-b852-78d20c7211ec`
- Status characteristic UUID: `47436878-5308-40f9-9c29-82c2cb87f595`
    - Properties: Write, Read, Notify
    - Values: `[0x00]` (hotspot off), `[0x01]` (hotspot on)

The app's settings (gear icon in the top right) allow you to configure the security options (
encryption, MITM protection) - it's set to encrypted (pairing required) without MITM protection by
default.

Implementation details can be found in [
`xyz.ggorg.easyspot.service.server`](./app/src/main/java/xyz/ggorg/easyspot/service/server).

## License and Credits

This project is licensed under the [GNU GPL v3](./LICENSE).

Big thanks to [supershadoe's Delta project](https://github.com/supershadoe/delta) (licensed
under [BSD-3-Clause](https://github.com/supershadoe/delta/blob/main/LICENSE))! Some code,
specifically related to hidden API hotspot control, has been adapted from there.

This project also uses:

- [Shizuku](https://github.com/RikkaApps/Shizuku)
- [HiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass)
- [HiddenApiRefine](https://github.com/RikkaApps/HiddenApiRefinePlugin)
- [Timber](https://github.com/JakeWharton/timber/)
