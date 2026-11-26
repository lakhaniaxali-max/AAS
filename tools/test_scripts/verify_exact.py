import subprocess
import xml.etree.ElementTree as ET
import time
import os

ADB = r"C:\Users\spectre\AppData\Local\Android\Sdk\platform-tools\adb.exe"
BRAIN = r"C:\Users\spectre\.gemini\antigravity\brain\1e2d0744-9037-44af-af4a-d73bade10235"

def adb(args):
    return subprocess.run([ADB] + args, capture_output=True, text=True)

# Tap etPassword (540, 1301)
adb(["shell", "input", "tap", "540", "1301"])
time.sleep(0.3)
for _ in range(25):
    adb(["shell", "input", "keyevent", "67"])
adb(["shell", "input", "text", "axali123"])
time.sleep(0.3)

# Hide keyboard
adb(["shell", "input", "keyevent", "111"])
time.sleep(0.5)

# Tap btnLogin (540, 1551)
adb(["shell", "input", "tap", "540", "1551"])
time.sleep(3)

# Dump Employee Screen
xml_p = os.path.join(BRAIN, "screen_employee_logged_in.xml")
png_p = os.path.join(BRAIN, "screen_employee_no_users_final.png")
adb(["shell", "uiautomator", "dump", "/sdcard/dump.xml"])
adb(["pull", "/sdcard/dump.xml", xml_p])
with open(png_p, "wb") as f:
    subprocess.run([ADB, "exec-out", "screencap", "-p"], stdout=f)

tree = ET.parse(xml_p)
texts = [n.attrib.get("text", "").strip() for n in tree.getroot().iter("node") if n.attrib.get("text", "").strip()]

print("\n--- EMPLOYEE DASHBOARD TEXTS ---")
for t in texts:
    print("  •", t)

# Look for bottom navigation elements
bottom_tabs = [t for t in texts if t in ["Dashboard", "Tasks", "Users", "Documents", "Profile"]]
print("\nBottom Navigation items present:", bottom_tabs)
assert "Users" not in bottom_tabs, "Users tab is still present!"
print("\n>>> VERIFICATION SUCCESS: Users is NOT present on the Employee panel navigation bar! <<<")
