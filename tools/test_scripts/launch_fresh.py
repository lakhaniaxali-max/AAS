import subprocess
import xml.etree.ElementTree as ET
import time
import os

ADB = r"C:\Users\spectre\AppData\Local\Android\Sdk\platform-tools\adb.exe"

subprocess.run([ADB, "shell", "am", "force-stop", "com.accurateaccounts.app"])
time.sleep(1)
subprocess.run([ADB, "shell", "am", "start", "-n", "com.accurateaccounts.app/.ui.landing.LandingActivity"])
time.sleep(2.5)

subprocess.run([ADB, "shell", "uiautomator", "dump", "/sdcard/dump.xml"])
subprocess.run([ADB, "pull", "/sdcard/dump.xml", "dump.xml"])
tree = ET.parse("dump.xml")

print("Nodes on screen after fresh start:")
for n in tree.getroot().iter("node"):
    t = n.attrib.get("text", "")
    rid = n.attrib.get("resource-id", "")
    if t:
        print(f"[{rid}] -> {t}")
