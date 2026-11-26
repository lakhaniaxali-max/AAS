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

# Close keyboard first
adb(["shell", "input", "keyevent", "111"])
time.sleep(0.5)

# Dump to see exact current bounds of username and password
tree = ET.parse(capture("pre_login")[0] and os.path.join(BRAIN, "pre_login.xml"))
b_user = None
b_pass = None
b_btn = None
for n in tree.getroot().iter("node"):
    rid = n.attrib.get("resource-id", "")
    if "etUsername" in rid:
        b_user = n.attrib.get("bounds")
    elif "etPassword" in rid:
        b_pass = n.attrib.get("bounds")
    elif "btnLogin" in rid:
        b_btn = n.attrib.get("bounds")

import re
def get_c(b):
    m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', b)
    return (int(m.group(1)) + int(m.group(3))) // 2, (int(m.group(2)) + int(m.group(4))) // 2

ux, uy = get_c(b_user)
px, py = get_c(b_pass)
bx, by = get_c(b_btn)

print(f"User: ({ux},{uy}), Pass: ({px},{py}), Btn: ({bx},{by})")

# Clear username
adb(["shell", "input", "tap", str(ux), str(uy)])
time.sleep(0.3)
adb(["shell", "input", "keyevent", "--meta", "4096", "29"]) # Ctrl+A
adb(["shell", "input", "keyevent", "67"]) # DEL
adb(["shell", "input", "text", "admin"])
time.sleep(0.3)

# Clear password
adb(["shell", "input", "tap", str(px), str(py)])
time.sleep(0.3)
adb(["shell", "input", "keyevent", "--meta", "4096", "29"])
adb(["shell", "input", "keyevent", "67"])
adb(["shell", "input", "text", "admin123"])
time.sleep(0.3)

# Close keyboard
adb(["shell", "input", "keyevent", "111"])
time.sleep(0.5)

# Tap login button
adb(["shell", "input", "tap", str(bx), str(by)])
time.sleep(3)

# Now in MainActivity: Tap Profile
adb(["shell", "input", "tap", "945", "2250"])
time.sleep(2.5)

texts, png = capture("admin_profile_verified")
print("Admin Profile Screen captured:", png)
for t in texts:
    print("  •", t)
