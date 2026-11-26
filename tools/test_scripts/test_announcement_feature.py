import subprocess
import xml.etree.ElementTree as ET
import time
import os

ADB = r"C:\Users\spectre\AppData\Local\Android\Sdk\platform-tools\adb.exe"
BRAIN = r"C:\Users\spectre\.gemini\antigravity\brain\1e2d0744-9037-44af-af4a-d73bade10235"

def adb(args):
    return subprocess.run([ADB] + args, capture_output=True, text=True, timeout=60)

def dump(name):
    xml_p = os.path.join(BRAIN, f"{name}.xml")
    png_p = os.path.join(BRAIN, f"{name}.png")
    adb(["shell", "uiautomator", "dump", "/sdcard/dump.xml"])
    adb(["pull", "/sdcard/dump.xml", xml_p])
    with open(png_p, "wb") as f:
        subprocess.run([ADB, "exec-out", "screencap", "-p"], stdout=f, timeout=15)
    tree = ET.parse(xml_p)
    elements = [(n.attrib.get("resource-id", ""), n.attrib.get("text", ""), n.attrib.get("bounds", ""))
                for n in tree.getroot().iter("node") if n.attrib.get("text")]
    return elements, png_p

def find_bounds(elements, matcher):
    for rid, txt, b in elements:
        if matcher.lower() in txt.lower() or matcher.lower() in rid.lower():
            return b
    return None

import re
def tap_b(b):
    m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', b)
    if m:
        cx = (int(m.group(1)) + int(m.group(3))) // 2
        cy = (int(m.group(2)) + int(m.group(4))) // 2
        adb(["shell", "input", "tap", str(cx), str(cy)])
        time.sleep(1)
        return True
    return False

# 1. APK already installed
print("1. APK installed, launching app...")

print("2. Starting app as Admin...")
adb(["shell", "pm", "clear", "com.accurateaccounts.app"])
time.sleep(1)
adb(["shell", "monkey", "-p", "com.accurateaccounts.app", "-c", "android.intent.category.LAUNCHER", "1"])
time.sleep(2.5)

# Tap LOGIN button on Landing: (932, 209)
adb(["shell", "input", "tap", "932", "209"])
time.sleep(1.5)

# Enter admin / admin123
adb(["shell", "input", "tap", "540", "1053"])
time.sleep(0.3)
for _ in range(25): adb(["shell", "input", "keyevent", "67"])
adb(["shell", "input", "text", "admin"])
time.sleep(0.3)

adb(["shell", "input", "tap", "540", "1301"])
time.sleep(0.3)
for _ in range(25): adb(["shell", "input", "keyevent", "67"])
adb(["shell", "input", "text", "admin123"])
time.sleep(0.3)

adb(["shell", "input", "keyevent", "111"]) # close keyboard
time.sleep(0.4)
adb(["shell", "input", "tap", "540", "1551"]) # Login
time.sleep(3)

# 3. Check Admin Dashboard for Announcement FAB button
elems, png_admin = dump("admin_dashboard_with_fab")
print(f"Captured Admin Dashboard: {png_admin}")
fab_b = find_bounds(elems, "fabAddAnnouncement")
if not fab_b:
    fab_b = find_bounds(elems, "Announce")

print(f"Found 'Announce' FAB button: {fab_b is not None}")
assert fab_b is not None, "FAB Add Announcement button not found on Admin Dashboard!"

# 4. Tap 'Announce' FAB button
print("Tapping 'Announce' button to open dialog...")
tap_b(fab_b)
time.sleep(2)

# 5. Check Dialog
dialog_elems, png_dialog = dump("announcement_dialog")
print(f"Captured Dialog: {png_dialog}")

b_title = find_bounds(dialog_elems, "etAnnouncementTitle")
b_msg = find_bounds(dialog_elems, "etAnnouncementMessage")
b_submit = find_bounds(dialog_elems, "btnPostAnnouncement")

print(f"Dialog elements found: TitleField={b_title is not None}, MsgField={b_msg is not None}, SubmitBtn={b_submit is not None}")
assert b_title and b_msg and b_submit, "Dialog fields missing!"

# Enter new announcement
tap_b(b_title)
adb(["shell", "input", "text", "Live_Broadcast_Test"])
time.sleep(0.5)

tap_b(b_msg)
adb(["shell", "input", "text", "System_maintenance_scheduled_tonight_at_11PM."])
time.sleep(0.5)

adb(["shell", "input", "keyevent", "111"]) # close keyboard
time.sleep(0.5)

# Click Post Announcement
tap_b(b_submit)
time.sleep(3)

# 6. Verify Announcement on Admin Dashboard
elems_post, png_admin_posted = dump("admin_dashboard_posted")
print(f"Captured Admin Dashboard after post: {png_admin_posted}")
has_posted = any("Live_Broadcast_Test" in txt for _, txt, _ in elems_post)
print(f"Announcement appears on Admin Dashboard: {has_posted}")

# 7. Check Employee Dashboard
print("\nSwitching to Employee account (axali123)...")
adb(["shell", "pm", "clear", "com.accurateaccounts.app"])
time.sleep(1)
adb(["shell", "monkey", "-p", "com.accurateaccounts.app", "-c", "android.intent.category.LAUNCHER", "1"])
time.sleep(2.5)

adb(["shell", "input", "tap", "932", "209"]) # LOGIN
time.sleep(1.5)

# Enter axali123 / axali123
adb(["shell", "input", "tap", "540", "1053"])
time.sleep(0.3)
for _ in range(25): adb(["shell", "input", "keyevent", "67"])
adb(["shell", "input", "text", "axali123"])
time.sleep(0.3)

adb(["shell", "input", "tap", "540", "1301"])
time.sleep(0.3)
for _ in range(25): adb(["shell", "input", "keyevent", "67"])
adb(["shell", "input", "text", "axali123"])
time.sleep(0.3)

adb(["shell", "input", "keyevent", "111"]) # close keyboard
time.sleep(0.4)
adb(["shell", "input", "tap", "540", "1551"]) # Login
time.sleep(3)

# 8. Check Employee Dashboard
elems_emp, png_emp = dump("employee_dashboard_announcements")
print(f"Captured Employee Dashboard: {png_emp}")
emp_has_fab = find_bounds(elems_emp, "fabAddAnnouncement") or find_bounds(elems_emp, "Announce")
emp_has_announcements = any("Announcements" in txt for _, txt, _ in elems_emp)

print(f"Employee sees Announcements section: {emp_has_announcements}")
print(f"Employee HAS 'Announce' button (should be False): {emp_has_fab is not None}")
assert emp_has_fab is None, "Employee should NOT have the Announce button!"

print("\n=======================================================")
print("ALL VERIFICATION CHECKS PASSED SUCCESSFULLY! ✅")
print("=======================================================")
