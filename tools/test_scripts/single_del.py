import subprocess
import xml.etree.ElementTree as ET
import time

ADB = r"C:\Users\spectre\AppData\Local\Android\Sdk\platform-tools\adb.exe"

subprocess.run([ADB, "shell", "input", "keyevent", "67"])
time.sleep(0.5)

subprocess.run([ADB, "shell", "uiautomator", "dump", "/sdcard/dump.xml"])
subprocess.run([ADB, "pull", "/sdcard/dump.xml", "dump.xml"])

tree = ET.parse("dump.xml")
for n in tree.getroot().iter("node"):
    if "etPassword" in n.attrib.get("resource-id", ""):
        print("etPassword text is now:", n.attrib.get("text"))
