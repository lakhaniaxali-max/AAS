import subprocess
import xml.etree.ElementTree as ET
import time
import os

ADB = r"C:\Users\spectre\AppData\Local\Android\Sdk\platform-tools\adb.exe"
BRAIN = r"C:\Users\spectre\.gemini\antigravity\brain\1e2d0744-9037-44af-af4a-d73bade10235"

def adb(args):
    return subprocess.run([ADB] + args, capture_output=True, text=True, timeout=25)

# 1. Copy APK
print("Copying APK...")
subprocess.run(["powershell", "-Command", "Copy-Item 'AndroidApp\\app\\build\\outputs\\apk\\debug\\app-debug.apk' 'AccurateAccounts.apk' -Force"])

# 2. Install APK
print("Installing APK via ADB...")
res = adb(["install", "-r", "AccurateAccounts.apk"])
print("Install result:", res.stdout)

# 3. Clear and start fresh
print("Launching app...")
adb(["shell", "pm", "clear", "com.accurateaccounts.app"])
time.sleep(1)
adb(["shell", "monkey", "-p", "com.accurateaccounts.app", "-c", "android.intent.category.LAUNCHER", "1"])
time.sleep(2.5)

# 4. On LandingActivity -> Click LOGIN (btnNavSignIn: [817,146][1048,272] -> 932, 209 or btnHeroSignIn: 539, 931)
adb(["shell", "input", "tap", "539", "931"])
time.sleep(2)

# 5. Type employee credentials (axali123 / axali123)
# Username field center: (540, 948)
adb(["shell", "input", "tap", "540", "948"])
time.sleep(0.3)
adb(["shell", "input", "text", "axali123"])
time.sleep(0.3)

# Password field center: (540, 1196)
adb(["shell", "input", "tap", "540", "1196"])
time.sleep(0.3)
adb(["shell", "input", "text", "axali123"])
time.sleep(0.3)

# Close keyboard
adb(["shell", "input", "keyevent", "111"])
time.sleep(0.5)

# Tap Login button (540, 1446)
adb(["shell", "input", "tap", "540", "1446"])
time.sleep(3)

# 6. Dump Employee Dashboard
xml_p = os.path.join(BRAIN, "employee_dashboard_now.xml")
png_p = os.path.join(BRAIN, "screen_employee_no_users.png")
adb(["shell", "uiautomator", "dump", "/sdcard/dump.xml"])
adb(["pull", "/sdcard/dump.xml", xml_p])
with open(png_p, "wb") as f:
    subprocess.run([ADB, "exec-out", "screencap", "-p"], stdout=f)

tree = ET.parse(xml_p)
texts = [n.attrib.get("text", "").strip() for n in tree.getroot().iter("node") if n.attrib.get("text", "").strip()]

print("\nEmployee Screen Captured:", png_p)
print("Screen Texts:")
for t in texts:
    print("  •", t)

has_users = any(t == "Users" for t in texts)
print("\n=======================================================")
print(f"Is 'Users' item visible in Employee navigation: {has_users}")
if not has_users:
    print("✅ SUCCESS: 'Users' is completely removed from Employee panel!")
else:
    print("❌ ERROR: 'Users' is still visible!")
print("=======================================================")
