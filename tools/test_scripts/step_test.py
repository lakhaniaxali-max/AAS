import subprocess
import xml.etree.ElementTree as ET
import time
import os

ADB = r"C:\Users\spectre\AppData\Local\Android\Sdk\platform-tools\adb.exe"
BRAIN = r"C:\Users\spectre\.gemini\antigravity\brain\1e2d0744-9037-44af-af4a-d73bade10235"

def adb(args):
    return subprocess.run([ADB] + args, capture_output=True, text=True, timeout=20)

def dump(name):
    xml_p = os.path.join(BRAIN, f"{name}.xml")
    png_p = os.path.join(BRAIN, f"{name}.png")
    adb(["shell", "uiautomator", "dump", "/sdcard/window_dump.xml"])
    adb(["pull", "/sdcard/window_dump.xml", xml_p])
    with open(png_p, "wb") as f:
        subprocess.run([ADB, "exec-out", "screencap", "-p"], stdout=f, timeout=15)
    tree = ET.parse(xml_p)
    elements = [(n.attrib.get("resource-id", ""), n.attrib.get("text", ""), n.attrib.get("bounds", ""))
                for n in tree.getroot().iter("node") if n.attrib.get("text")]
    return elements, png_p

# Tap btnHeroSignIn: (539, 931)
adb(["shell", "input", "tap", "539", "931"])
time.sleep(2)

elems, png = dump("step1")
print(f"Captured screen -> {png}")
for rid, txt, b in elems:
    print(f"[{rid}] '{txt}' @ {b}")
