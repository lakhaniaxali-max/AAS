import subprocess
import xml.etree.ElementTree as ET
import time
import os
import sys

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

user = sys.argv[1] if len(sys.argv) > 1 else "admin"
pwd = sys.argv[2] if len(sys.argv) > 2 else "admin123"

print(f"Logging in with user: {user}...")
# Tap username
adb(["shell", "input", "tap", "540", "948"])
time.sleep(0.3)
adb(["shell", "input", "text", user])
time.sleep(0.3)

# Tap password
adb(["shell", "input", "tap", "540", "1196"])
time.sleep(0.3)
adb(["shell", "input", "text", pwd])
time.sleep(0.3)

# Tap login
adb(["shell", "input", "tap", "540", "1446"])
time.sleep(3)

# Tap Profile Tab
adb(["shell", "input", "tap", "945", "2250"])
time.sleep(2.5)

texts, png = capture(f"profile_view_{user}")
print(f"Captured: {png}")
print("Elements on profile:")
for t in texts:
    print("  •", t)
