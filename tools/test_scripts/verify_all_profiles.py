import subprocess
import xml.etree.ElementTree as ET
import os
import time
import re

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
    return xml_p, png_p

def find_bounds(xml_p, matcher):
    tree = ET.parse(xml_p)
    for n in tree.getroot().iter("node"):
        txt = n.attrib.get("text", "")
        rid = n.attrib.get("resource-id", "")
        if matcher.lower() in txt.lower() or matcher.lower() in rid.lower():
            return n.attrib.get("bounds", "")
    return None

def tap_bounds(bounds):
    m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', bounds)
    if m:
        cx = (int(m.group(1)) + int(m.group(3))) // 2
        cy = (int(m.group(2)) + int(m.group(4))) // 2
        adb(["shell", "input", "tap", str(cx), str(cy)])
        time.sleep(1)
        return True
    return False

def test_profile(username, password, role_name):
    print(f"\n==================================================================")
    print(f"VERIFYING PROFILE: {role_name} ({username})")
    print(f"==================================================================")
    
    # 1. Clear app and launch fresh
    adb(["shell", "pm", "clear", "com.accurateaccounts.app"])
    time.sleep(1)
    adb(["shell", "monkey", "-p", "com.accurateaccounts.app", "-c", "android.intent.category.LAUNCHER", "1"])
    time.sleep(2)
    
    # 2. On Landing Screen - Tap LOGIN
    x_land, _ = dump(f"landing_{username}")
    b_login = find_bounds(x_land, "btnNavSignIn")
    if not b_login:
        b_login = find_bounds(x_land, "btnHeroSignIn")
    tap_bounds(b_login)
    time.sleep(1.5)
    
    # 3. On Login Screen
    x_login, _ = dump(f"login_{username}")
    b_user = find_bounds(x_login, "etUsername")
    b_pass = find_bounds(x_login, "etPassword")
    b_btn = find_bounds(x_login, "btnLogin")
    
    if not (b_user and b_pass and b_btn):
        print("ERROR: Could not find login fields on screen!")
        return False
        
    tap_bounds(b_user)
    adb(["shell", "input", "text", username])
    time.sleep(0.5)
    
    tap_bounds(b_pass)
    adb(["shell", "input", "text", password])
    time.sleep(0.5)
    
    tap_bounds(b_btn)
    time.sleep(3)
    
    # 4. On Main Screen - Tap Profile tab
    x_main, _ = dump(f"main_{username}")
    b_prof = find_bounds(x_main, "Profile")
    if not b_prof:
        b_prof = find_bounds(x_main, "nav_profile")
    if b_prof:
        tap_bounds(b_prof)
    else:
        adb(["shell", "input", "tap", "945", "2250"])
    time.sleep(2)
    
    # 5. Capture Profile Screen
    prof_xml, prof_png = dump(f"screen_profile_{role_name.lower()}")
    print(f"Profile screenshot captured: {prof_png}")
    
    tree = ET.parse(prof_xml)
    texts = [n.attrib.get("text", "").strip() for n in tree.getroot().iter("node") if n.attrib.get("text", "").strip()]
    
    print("\nProfile UI Elements:")
    for t in texts:
        print(f"  • {t}")
        
    has_db = any("database" in t.lower() for t in texts)
    print(f"\n[VERIFICATION CHECKS]")
    print(f"  • Database status row absent: {not has_db} (MUST BE TRUE)")
    if has_db:
        print("  ❌ FAIL: 'Database' row found on profile!")
    else:
        print("  ✅ PASS: 'Database' row completely removed!")
        
    return not has_db

if __name__ == "__main__":
    admin_ok = test_profile("admin", "admin123", "Admin")
    emp_ok = test_profile("axali123", "axali123", "Employee")
    client_ok = test_profile("kashish", "kashish123", "Client")
    
    print("\n==================================================================")
    print("FINAL RESULTS:")
    print(f"  Admin Profile:    {'✅ PASSED' if admin_ok else '❌ FAILED'}")
    print(f"  Employee Profile: {'✅ PASSED' if emp_ok else '❌ FAILED'}")
    print(f"  Client Profile:   {'✅ PASSED' if client_ok else '❌ FAILED'}")
    print("==================================================================")
