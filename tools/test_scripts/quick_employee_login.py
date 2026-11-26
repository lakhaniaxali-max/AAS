import subprocess
import xml.etree.ElementTree as ET
import time
import os

ADB = r"C:\Users\spectre\AppData\Local\Android\Sdk\platform-tools\adb.exe"
BRAIN = r"C:\Users\spectre\.gemini\antigravity\brain\1e2d0744-9037-44af-af4a-d73bade10235"

def adb(args):
    return subprocess.run([ADB] + args, capture_output=True, text=True)

# 1. Tap LOGIN (btnNavSignIn): (932, 209)
adb(["shell", "input", "tap", "932", "209"])
time.sleep(2)

# 2. Tap username field: (540, 948)
adb(["shell", "input", "tap", "540", "948"])
time.sleep(0.3)
# Clear field
for _ in range(25):
    adb(["shell", "input", "keyevent", "67"])
adb(["shell", "input", "text", "axali123"])
time.sleep(0.3)

# 3. Tap password field: (540, 1196)
adb(["shell", "input", "tap", "540", "1196"])
time.sleep(0.3)
for _ in range(25):
    adb(["shell", "input", "keyevent", "67"])
adb(["shell", "input", "text", "axali123"])
time.sleep(0.3)

# 4. Hide keyboard
adb(["shell", "input", "keyevent", "111"])
time.sleep(0.5)

# 5. Tap Login button: (540, 1446)
adb(["shell", "input", "tap", "540", "1446"])
time.sleep(3)

# 6. Dump Employee Dashboard
xml_p = os.path.join(BRAIN, "employee_verified.xml")
png_p = os.path.join(BRAIN, "screen_employee_verified.png")
adb(["shell", "uiautomator", "dump", "/sdcard/dump.xml"])
adb(["pull", "/sdcard/dump.xml", xml_p])
with open(png_p, "wb") as f:
    subprocess.run([ADB, "exec-out", "screencap", "-p"], stdout=f)

tree = ET.parse(xml_p)
texts = [n.attrib.get("text", "").strip() for n in tree.getroot().iter("node") if n.attrib.get("text", "").strip()]

print("\n--- EMPLOYEE DASHBOARD SCREEN TEXTS ---")
for t in texts:
    print("  •", t)

has_users = "Users" in texts
print(f"\nResult: 'Users' item in navigation: {has_users}")
if not has_users:
    print("SUCCESS: Users tab is REMOVED from Employee panel!")
