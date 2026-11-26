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

print('Step 1: Navigating to Login...')
tap(932, 209) # top right LOGIN button
time.sleep(1.5)

root = dump_ui()
u_node = find_node(root, res_id='etUsername')
p_node = find_node(root, res_id='etPassword')
b_node = find_node(root, res_id='btnLogin')

if u_node and p_node and b_node:
    cu = get_center(u_node)
    cp = get_center(p_node)
    cb = get_center(b_node)
    
    print('Entering Admin credentials...')
    tap(cu[0], cu[1])
    type_text('admin')
    
    tap(cp[0], cp[1])
    type_text('admin123')
    
    tap(cb[0], cb[1])
    print('Waiting for Admin Dashboard...')
    time.sleep(4)

root = dump_ui()
fab = find_node(root, res_id='fabAddAnnouncement')
print('Admin Dashboard FAB detected:', fab is not None)
if fab:
    print('FAB bounds:', fab.attrib.get('bounds'))
    print('FAB text:', fab.attrib.get('text'))
    capture_screen('admin_dashboard_with_announcement_fab.png')
    
    cfab = get_center(fab)
    print('Clicking Announcement FAB at', cfab)
    tap(cfab[0], cfab[1])
    time.sleep(1.5)
    
    d_root = dump_ui()
    t_field = find_node(d_root, res_id='etAnnouncementTitle')
    m_field = find_node(d_root, res_id='etAnnouncementMessage')
    d_field = find_node(d_root, res_id='etAnnouncementDate')
    a_field = find_node(d_root, res_id='etAnnouncementAuthor')
    btn_post = find_node(d_root, res_id='btnPostAnnouncement')
    
    print('Dialog fields:', {
        'title': t_field is not None,
        'message': m_field is not None,
        'date': d_field.attrib.get('text') if d_field else None,
        'author': a_field.attrib.get('text') if a_field else None,
        'btn_post': btn_post is not None
    })
    
    if t_field and m_field and btn_post:
        ct = get_center(t_field)
        tap(ct[0], ct[1])
        type_text('Annual_Financial_Closing')
        
        cm = get_center(m_field)
        tap(cm[0], cm[1])
        type_text('Please_reconcile_all_invoices_and_tax_filings_by_the_30th.')
        
        capture_screen('admin_announcement_dialog.png')
        
        cpost = get_center(btn_post)
        print('Posting announcement...')
        tap(cpost[0], cpost[1])
        time.sleep(3)
        
        capture_screen('admin_dashboard_posted.png')
        print('Admin announcement created!')

print('Verification script finished.')
