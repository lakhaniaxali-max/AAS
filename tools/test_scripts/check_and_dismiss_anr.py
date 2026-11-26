import subprocess
import xml.etree.ElementTree as ET
import time

ADB = r"C:\Users\spectre\AppData\Local\Android\Sdk\platform-tools\adb.exe"

subprocess.run([ADB, "shell", "uiautomator", "dump", "/sdcard/dump.xml"])
subprocess.run([ADB, "pull", "/sdcard/dump.xml", "dump.xml"])
tree = ET.parse("dump.xml")

for n in tree.getroot().iter("node"):
    t = n.attrib.get("text", "")
    rid = n.attrib.get("resource-id", "")
    b = n.attrib.get("bounds", "")
    if t or rid:
        print(f"[{rid}] '{t}' @ {b}")
        if "close app" in t.lower() or "wait" in t.lower() or "ok" in t.lower():
            # Tap it
            import re
            m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', b)
            if m:
                cx = (int(m.group(1)) + int(m.group(3))) // 2
                cy = (int(m.group(2)) + int(m.group(4))) // 2
                print(f"Tapping dismiss button at ({cx}, {cy})...")
                subprocess.run([ADB, "shell", "input", "tap", str(cx), str(cy)])
