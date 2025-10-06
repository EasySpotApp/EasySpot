import bleak
import asyncio
import sys

SERVICE_UUID = "7baad717-1551-45e1-b852-78d20c7211ec"
CHARACTERISTIC_UUID = "47436878-5308-40f9-9c29-82c2cb87f595"


async def main():
    if len(sys.argv) == 1:
        print("Usage: python3 client.py value")
        return

    data = sys.argv[1]

    try:
        data = int(data)
    except ValueError:
        if data.lower() in ["true", "yes", "on", "y", "false", "no", "off", "n"]:
            data = int(data.lower() in ["true", "yes", "on", "y"])
        else:
            print("Value must be an integer or a boolean.")
            return

    print("Scanning for devices...")

    device = await bleak.BleakScanner.find_device_by_filter(
        lambda dev, adv: True, service_uuids=[SERVICE_UUID]
    )

    if not device:
        print("No device found.")
        return

    print(f"Found device: {device.address}")

    async with bleak.BleakClient(device) as client:
        print("Connected to device.")
        await client.write_gatt_char(CHARACTERISTIC_UUID, [data], response=True)
        print(f"Wrote value: {data}")


if __name__ == "__main__":
    asyncio.run(main())
