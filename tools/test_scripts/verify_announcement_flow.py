import subprocess, time, re, os, json
import xml.etree.ElementTree as ET

ADB = r'C:\Users\spectre\AppData\Local\Android\Sdk\platform-tools\adb.exe'

def adb_shell(cmd):
    p = subprocess.run([ADB, 'shell'] + cmd.split(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    return p.stdout.strip()

def tap(x, y):
    print(f'Tapping ({x}, {y})')
    adb_shell(f'input tap {x} {y}')
    time.sleep(1)

def type_text(text):
    t = text.replace(' ', '%s')
    adb_shell(f'input text {t}')
    time.sleep(0.5)

def dump_ui():
    adb_shell('uiautomator dump /data/local/tmp/dump.xml')
    subprocess.run([ADB, 'pull', '/data/local/tmp/dump.xml', 'dump.xml'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    return ET.parse('dump.xml').getroot()

def find_node(root, res_id=None, text_contains=None):
    for node in root.iter('node'):
        r = node.attrib.get('resource-id', '')
        t = node.attrib.get('text', '')
        if res_id and res_id in r:
            return node
        if text_contains and text_contains.lower() in t.lower():
            return node
    return None

def get_center(node):
    b = node.attrib.get('bounds', '')
    m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', b)
    if m:
        x1, y1, x2, y2 = map(int, m.groups())
        return (x1 + x2) // 2, (y1 + y2) // 2
    return None

def capture_screen(filename):
    adb_shell(f'screencap -p /data/local/tmp/{filename}')
    subprocess.run([ADB, 'pull', f'/data/local/tmp/{filename}', filename], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    print(f'Captured screenshot: {filename}')

print('--- Step 1: Tap LOGIN TO YOUR PORTAL ---')
root = dump_ui()
hero_btn = find_node(root, res_id='btnHeroSignIn') or find_node(root, res_id='btnNavSignIn')
if hero_btn:
    c = get_center(hero_btn)
    tap(c[0], c[1])
    time.sleep(1.5)
else:
    print('Hero sign in not found, checking if already on login...')

print('--- Step 2: Enter Admin Credentials ---')
root = dump_ui()
u_field = find_node(root, res_id='etUsername')
p_field = find_node(root, res_id='etPassword')
b_login = find_node(root, res_id='btnLogin')

if u_field and p_field and b_login:
    c_u = get_center(u_field)
    tap(c_u[0], c_u[1])
    type_text('admin')
    
    c_p = get_center(p_field)
    tap(c_p[0], c_p[1])
    type_text('admin123')
    
    c_b = get_center(b_login)
    tap(c_b[0], c_b[1])
    print('Tapped Login, waiting for Admin Dashboard...')
    time.sleep(4)
else:
    print('Login fields not found!')

print('--- Step 3: Check Admin Dashboard for Announcement FAB ---')
root = dump_ui()
fab = find_node(root, res_id='fabAddAnnouncement')
if fab:
    bounds = fab.attrib.get('bounds')
    text = fab.attrib.get('text')
    print(f'SUCCESS: Found Announcement FAB on Admin Dashboard! Text="{text}", Bounds={bounds}')
    capture_screen('admin_dash_fab.png')
    
    print('--- Step 4: Click Announcement FAB to open Creation Dialog ---')
    c_fab = get_center(fab)
    tap(c_fab[0], c_fab[1])
    time.sleep(1.5)
    
    dialog_root = dump_ui()
    t_field = find_node(dialog_root, res_id='etAnnouncementTitle')
    m_field = find_node(dialog_root, res_id='etAnnouncementMessage')
    d_field = find_node(dialog_root, res_id='etAnnouncementDate')
    a_field = find_node(dialog_root, res_id='etAnnouncementAuthor')
    btn_post = find_node(dialog_root, res_id='btnPostAnnouncement')
    
    print('Dialog inputs detected:')
    print(f' - Title field: {t_field is not None}')
    print(f' - Message field: {m_field is not None}')
    print(f' - Date field (default): {d_field.attrib.get("text") if d_field else None}')
    print(f' - Author field (default): {a_field.attrib.get("text") if a_field else None}')
    print(f' - Post Button: {btn_post is not None}')
    
    if t_field and m_field and btn_post:
        c_t = get_center(t_field)
        tap(c_t[0], c_t[1])
        type_text('Company-Wide_Tax_Notice')
        
        c_m = get_center(m_field)
        tap(c_m[0], c_m[1])
        type_text('All_audit_files_and_GST_reports_must_be_uploaded_by_this_Friday.')
        
        capture_screen('announcement_dialog_filled.png')
        
        c_post = get_center(btn_post)
        print('Tapping Post Announcement...')
        tap(c_post[0], c_post[1])
        time.sleep(3)
        
        capture_screen('admin_after_announcement_posted.png')
        print('Announcement posted successfully!')
else:
    print('Announcement FAB not found on current screen!')
    capture_screen('error_screen.png')
