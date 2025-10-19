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
