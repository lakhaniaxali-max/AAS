import subprocess
import xml.etree.ElementTree as ET
import time
import os

ADB = r"C:\Users\spectre\AppData\Local\Android\Sdk\platform-tools\adb.exe"

def adb(args):
    return subprocess.run([ADB] + args, capture_output=True, text=True, timeout=15)

# Clear username
adb(["shell", "input", "tap", "540", "1053"])
time.sleep(0.3)
for _ in range(30):
    adb(["shell", "input", "keyevent", "67"])
adb(["shell", "input", "text", "admin"])
time.sleep(0.3)

# Clear password
adb(["shell", "input", "tap", "540", "1301"])
time.sleep(0.3)
for _ in range(30):
    adb(["shell", "input", "keyevent", "67"])
adb(["shell", "input", "text", "admin123"])
time.sleep(0.3)

# Hide keyboard
adb(["shell", "input", "keyevent", "111"])
time.sleep(0.5)

# Tap Login (540, 1498)
adb(["shell", "input", "tap", "540", "1498"])
time.sleep(3)

# Tap Profile Tab (945, 2250)
adb(["shell", "input", "tap", "945", "2250"])
time.sleep(2.5)

# Dump screen
subprocess.run([ADB, "shell", "uiautomator", "dump", "/sdcard/dump.xml"])
subprocess.run([ADB, "pull", "/sdcard/dump.xml", "admin_final.xml"])
with open(r"C:\Users\spectre\.gemini\antigravity\brain\1e2d0744-9037-44af-af4a-d73bade10235\admin_profile_success.png", "wb") as f:
    subprocess.run([ADB, "exec-out", "screencap", "-p"], stdout=f)

tree = ET.parse("admin_final.xml")
print("Profile Screen Texts:")
for n in tree.getroot().iter("node"):
    t = n.attrib.get("text")
    if t:
        print("  •", t)
