import subprocess
import xml.etree.ElementTree as ET
import time
import os

ADB = r"C:\Users\spectre\AppData\Local\Android\Sdk\platform-tools\adb.exe"
BRAIN = r"C:\Users\spectre\.gemini\antigravity\brain\1e2d0744-9037-44af-af4a-d73bade10235"

# Tap LOGIN button on Landing: (539, 931)
subprocess.run([ADB, "shell", "input", "tap", "539", "931"])
time.sleep(1.5)

# Tap username field
subprocess.run([ADB, "shell", "input", "tap", "540", "948"])
time.sleep(0.3)
subprocess.run([ADB, "shell", "input", "text", "axali123"])
time.sleep(0.3)

# Tap password field
subprocess.run([ADB, "shell", "input", "tap", "540", "1196"])
time.sleep(0.3)
subprocess.run([ADB, "shell", "input", "text", "axali123"])
time.sleep(0.3)

# Close keyboard
subprocess.run([ADB, "shell", "input", "keyevent", "111"])
time.sleep(0.5)

# Tap login button
subprocess.run([ADB, "shell", "input", "tap", "540", "1446"])
time.sleep(3)

# Dump Employee Dashboard
xml_p = os.path.join(BRAIN, "employee_logged_in.xml")
png_p = os.path.join(BRAIN, "screen_employee_verified_no_users.png")
subprocess.run([ADB, "shell", "uiautomator", "dump", "/sdcard/dump.xml"])
subprocess.run([ADB, "pull", "/sdcard/dump.xml", xml_p])
with open(png_p, "wb") as f:
    subprocess.run([ADB, "exec-out", "screencap", "-p"], stdout=f)

tree = ET.parse(xml_p)
texts = [n.attrib.get("text", "").strip() for n in tree.getroot().iter("node") if n.attrib.get("text", "").strip()]

print("\nScreen Texts on Employee Dashboard:")
for t in texts:
    print("  •", t)

bottom_tabs = [t for t in texts if t in ["Dashboard", "Tasks", "Users", "Documents", "Profile"]]
print("\nBottom Navigation items found:", bottom_tabs)
assert "Users" not in bottom_tabs, "Users tab is still present!"
print("SUCCESS: Confirmed Users tab is NOT present on employee dashboard!")
