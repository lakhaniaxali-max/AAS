import subprocess
import xml.etree.ElementTree as ET
import time
import os

ADB = r"C:\Users\spectre\AppData\Local\Android\Sdk\platform-tools\adb.exe"
BRAIN = r"C:\Users\spectre\.gemini\antigravity\brain\1e2d0744-9037-44af-af4a-d73bade10235"

def adb(args):
    return subprocess.run([ADB] + args, capture_output=True, text=True, timeout=15)

def capture(name):
    xml_p = os.path.join(BRAIN, f"{name}.xml")
    png_p = os.path.join(BRAIN, f"{name}.png")
    adb(["shell", "uiautomator", "dump", "/sdcard/dump.xml"])
    adb(["pull", "/sdcard/dump.xml", xml_p])
    with open(png_p, "wb") as f:
        subprocess.run([ADB, "exec-out", "screencap", "-p"], stdout=f, timeout=15)
    tree = ET.parse(xml_p)
    elements = [n.attrib.get("text", "").strip() for n in tree.getroot().iter("node") if n.attrib.get("text", "").strip()]
    return elements, png_p

print("Step 1: Fixing password on screen (deleting trailing character)...")
adb(["shell", "input", "tap", "950", "1196"])
time.sleep(0.3)
adb(["shell", "input", "keyevent", "67"]) # backspace
time.sleep(0.3)

print("Step 2: Tapping Login button...")
adb(["shell", "input", "tap", "540", "1446"])
time.sleep(2.5)

print("Step 3: Tapping Profile tab in bottom navigation...")
adb(["shell", "input", "tap", "945", "2250"])
time.sleep(2.5)

texts, png = capture("screen_profile_client_final")
print(f"Captured Client Profile -> {png}")
print("Profile Texts found:")
for t in texts:
    print("  •", t)
