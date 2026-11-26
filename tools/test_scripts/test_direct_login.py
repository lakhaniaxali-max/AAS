import subprocess
import xml.etree.ElementTree as ET
import time

ADB = r"C:\Users\spectre\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# 1. Tap password
subprocess.run([ADB, "shell", "input", "tap", "540", "1301"])
time.sleep(0.5)

# 2. Delete all existing characters
subprocess.run([ADB, "shell", "input", "keyevent"] + ["67"] * 30)
time.sleep(0.5)

# 3. Type kashish123
subprocess.run([ADB, "shell", "input", "text", "kashish123"])
time.sleep(0.5)

# 4. Tap Login button (106+974)//2=540, (1480+1622)//2=1551
subprocess.run([ADB, "shell", "input", "tap", "540", "1551"])
time.sleep(3)

# 5. Dump screen
subprocess.run([ADB, "shell", "uiautomator", "dump", "/sdcard/after_login.xml"])
subprocess.run([ADB, "pull", "/sdcard/after_login.xml", "after_login.xml"])
with open(r"C:\Users\spectre\.gemini\antigravity\brain\1e2d0744-9037-44af-af4a-d73bade10235\screen_after_kashish_login.png", "wb") as f:
    subprocess.run([ADB, "exec-out", "screencap", "-p"], stdout=f)

tree = ET.parse("after_login.xml")
print("After login screen texts:")
for n in tree.getroot().iter("node"):
    t = n.attrib.get("text")
    if t:
        print("  •", t)
