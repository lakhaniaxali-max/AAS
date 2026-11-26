import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

def set_cell_background(cell, fill_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=120, bottom=120, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(f'<w:tcMar {nsdecls("w")}><w:top w:w="{top}" w:type="dxa"/><w:bottom w:w="{bottom}" w:type="dxa"/><w:left w:w="{left}" w:type="dxa"/><w:right w:w="{right}" w:type="dxa"/></w:tcMar>')
    tcPr.append(tcMar)

def add_heading_with_spacing(doc, text, level):
    h = doc.add_heading(text, level=level)
    h.paragraph_format.keep_with_next = True
    if level == 1:
        h.paragraph_format.space_before = Pt(14)
        h.paragraph_format.space_after = Pt(4)
        for r in h.runs:
            r.font.name = 'Arial'
            r.font.color.rgb = RGBColor(30, 58, 138) # Dark Navy
    elif level == 2:
        h.paragraph_format.space_before = Pt(10)
        h.paragraph_format.space_after = Pt(3)
        for r in h.runs:
            r.font.name = 'Arial'
            r.font.color.rgb = RGBColor(37, 99, 235) # Vibrant Blue
    return h

def add_bullet(doc, bold_prefix, text):
    p = doc.add_paragraph(style='List Bullet')
    p.paragraph_format.space_after = Pt(3)
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.line_spacing = 1.15
    r_bold = p.add_run(bold_prefix + ": ")
    r_bold.bold = True
    r_bold.font.name = 'Arial'
    r_bold.font.color.rgb = RGBColor(15, 23, 42)
    r_text = p.add_run(text)
    r_text.font.name = 'Arial'
    r_text.font.color.rgb = RGBColor(51, 65, 85)
    return p

def add_body(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(4)
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.line_spacing = 1.15
    r = p.add_run(text)
    r.font.name = 'Arial'
    r.font.color.rgb = RGBColor(51, 65, 85)
    return p

def create_report():
    doc = docx.Document()

    # Page Margins: 1 inch
    for section in doc.sections:
        section.top_margin = Inches(1)
        section.bottom_margin = Inches(1)
        section.left_margin = Inches(1)
        section.right_margin = Inches(1)

    # Title Block
    p_title = doc.add_paragraph()
    p_title.paragraph_format.space_before = Pt(10)
    p_title.paragraph_format.space_after = Pt(2)
    p_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r_title = p_title.add_run("MOBILE APPLICATION DEVELOPMENT DESIGN DOCUMENT")
    r_title.bold = True
    r_title.font.size = Pt(20)
    r_title.font.name = 'Arial'
    r_title.font.color.rgb = RGBColor(30, 58, 138)

    p_sub = doc.add_paragraph()
    p_sub.paragraph_format.space_after = Pt(16)
    p_sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r_sub = p_sub.add_run("Project: Accurate Account Services (AAS) Mobile Application")
    r_sub.font.size = Pt(13)
    r_sub.font.name = 'Arial'
    r_sub.font.color.rgb = RGBColor(71, 85, 105)

    # Document Meta Table
    meta_table = doc.add_table(rows=2, cols=4)
    meta_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    meta_data = [
        [("Project Name", True), ("Accurate Account Services", False), ("Target Platform", True), ("Android (SDK 26–34)", False)],
        [("Architecture", True), ("MVVM / RESTful Retrofit", False), ("Version", True), ("1.0.0 (Production)", False)]
    ]
    for row_idx, row in enumerate(meta_data):
        for col_idx, (text, is_label) in enumerate(row):
            cell = meta_table.cell(row_idx, col_idx)
            set_cell_margins(cell, 80, 80, 100, 100)
            p = cell.paragraphs[0]
            p.alignment = WD_ALIGN_PARAGRAPH.LEFT
            r = p.add_run(text)
            r.font.name = 'Arial'
            r.font.size = Pt(9.5)
            if is_label:
                r.bold = True
                set_cell_background(cell, "F1F5F9")
                r.font.color.rgb = RGBColor(30, 58, 138)
            else:
                set_cell_background(cell, "FFFFFF")
                r.font.color.rgb = RGBColor(51, 65, 85)

    doc.add_paragraph().paragraph_format.space_after = Pt(6)

    # 1. Introduction
    add_heading_with_spacing(doc, "1. Introduction", level=1)
    
    add_heading_with_spacing(doc, "1.1 Overview of the Project and its Goals", level=2)
    add_body(doc, "Accurate Account Services (AAS) is an enterprise financial accounting and tax compliance application engineered to bridge the communication and workflow gap between chartered accountants, staff employees, and corporate clients. The mobile application provides an agile, role-guarded interface for managing financial records, task schedules, document verification, and administrative announcements.")
    add_bullet(doc, "Workflow Digitization", "Replaces error-prone manual spreadsheets with automated task assignments, state trackers, and real-time deadline monitoring.")
    add_bullet(doc, "Secure KYC & Tax Vault", "Enables encrypted upload, viewing, and approval of financial proofs (GSTIN, PAN, Aadhar, Balance Sheets, P&L statements).")
    add_bullet(doc, "Role-Specific Portals", "Delivers personalized views for Administrators, Staff Accountants, and Corporate Clients.")

    add_heading_with_spacing(doc, "1.2 Purpose of the Mobile Application Development Design Document", level=2)
    add_body(doc, "This document establishes the architectural blueprints, technical specifications, component hierarchy, UI/UX guidelines, data synchronization strategies, and deployment lifecycle for the AAS mobile platform. It acts as an authoritative benchmark for developers, QA engineers, and stakeholders throughout development and maintenance.")

    add_heading_with_spacing(doc, "1.3 Scope and Assumptions of the Mobile Application Development Process", level=2)
    add_bullet(doc, "Target Scope", "Native Android mobile client integrated with an Express.js / PostgreSQL / MySQL REST and WebSocket backend.")
    add_bullet(doc, "Functional Modules", "Bi-directional authentication, dashboard metrics, task allocation, document status toggling, staff directory pagination, and interactive popups.")
    add_bullet(doc, "Core Assumptions", "End users possess devices running Android 8.0 (API 26) or higher with active Wi-Fi or cellular connectivity. Backend endpoints provide authenticated JSON responses secured via JWT.")

    # 2. User Interface Design
    add_heading_with_spacing(doc, "2. User Interface Design", level=1)

    add_heading_with_spacing(doc, "2.1 Wireframes and Mockups of the User Interface", level=2)
    add_body(doc, "UI layouts are structured following Material Design 3 guidelines with high-contrast hierarchical cards, bottom sheets for transient actions, and predictable navigation paths.")
    add_bullet(doc, "Public Landing & Authentication", "Clean hero banner with company highlights and modal credential inputs.")
    add_bullet(doc, "Role-Based Dashboards", "Card-based widgets showing Assigned Tasks, live Red Badges for unread tasks, Announcements, and Customer Inquiries.")
    add_bullet(doc, "Bottom Navigation Bar", "Context-aware bottom bar rendering relevant tabs per role (Dashboard, Tasks, Documents, Profile, Users).")

    add_heading_with_spacing(doc, "2.2 User Experience (UX) Design", level=2)
    add_bullet(doc, "Zero-Friction Status Updates", "Interactive modal bottom sheets allow 1-tap state toggles (Pending → In Progress → Completed) without reloading screens.")
    add_bullet(doc, "Proactive Indicators", "Pill badges with vivid red coloration (#EF4444) draw immediate attention to newly assigned work, auto-clearing upon review.")
    add_bullet(doc, "Non-Intrusive Confirmations", "Destructive actions (e.g., Logout) trigger soft-dismiss bottom sheets displaying account identity to prevent accidental sign-outs.")

    add_heading_with_spacing(doc, "2.3 Responsive Design for Different Screen Sizes", level=2)
    add_bullet(doc, "Dynamic Scaling", "Layout dimensions use density-independent pixels (dp) and scalable typography (sp).")
    add_bullet(doc, "SmoothNestedScrollView", "Custom nested scrolling components prevent scroll-locking across phones ranging from 5.0\" compact models to 6.7\" flagship displays.")

    add_heading_with_spacing(doc, "2.4 Accessibility Features for Users with Disabilities", level=2)
    add_bullet(doc, "WCAG 2.1 Contrast", "Strict contrast ratios (>4.5:1) between text (#0F172A, #FFFFFF) and background containers (#1E3A8A, #EFF6FF).")
    add_bullet(doc, "Touch Targets & Screen Readers", "All touch targets meet the minimum 48x48dp standard with meaningful contentDescription attributes on vector drawables.")

    # 3. Mobile Application Architecture
    add_heading_with_spacing(doc, "3. Mobile Application Architecture", level=1)

    add_heading_with_spacing(doc, "3.1 Architecture Patterns (e.g., MVVM, MVP)", level=2)
    add_body(doc, "The application implements the Model-View-ViewModel (MVVM) pattern combined with Repository-backed data fetching:")
    add_bullet(doc, "Model", "Strict POJO data models serialized via Gson (Task, User, Announcement, ClientDocument).")
    add_bullet(doc, "View", "Android Activities and Fragments responsible exclusively for UI rendering and capturing touch events.")
    add_bullet(doc, "Data / ViewModel Layer", "SharedPreferences session managers, Retrofit ApiService client singleton, and asynchronous Retrofit Callbacks.")

    add_heading_with_spacing(doc, "3.2 Mobile Operating System Selection (e.g., iOS, Android)", level=2)
    add_body(doc, "Android was selected as the launch platform due to its 75%+ enterprise market penetration among Indian accounting firms, ease of sideloading enterprise APKs, and comprehensive hardware compatibility across economical to high-end staff devices.")

    add_heading_with_spacing(doc, "3.3 Mobile Application Development Frameworks", level=2)
    add_body(doc, "Native Android (Java / Gradle) was chosen over hybrid solutions (React Native, Flutter) to guarantee optimal execution speeds, instant cold starts, native Material Design integration, and direct hardware-accelerated rendering.")

    add_heading_with_spacing(doc, "3.4 APIs and Integration with Server-Side Components", level=2)
    add_bullet(doc, "REST Client", "Retrofit 2.9 with OkHttp 4.9 connection pooling and HTTP logging interceptors.")
    add_bullet(doc, "Real-Time Comms", "Socket.io client handles instant notification events and bidirectional messaging.")
    add_bullet(doc, "Dynamic Host Switching", "Constants.java provides real-time fallback between Android Emulator host (10.0.2.2:3000), Local Wi-Fi (10.105.4.200:3000), and Public Tunnel endpoints.")

    # 4. Component Design
    add_heading_with_spacing(doc, "4. Component Design", level=1)

    add_heading_with_spacing(doc, "4.1 Component Hierarchy and Dependencies", level=2)
    add_bullet(doc, "Root Container", "MainActivity hosts dynamic fragment switching inside a single FragmentContainerView.")
    add_bullet(doc, "Fragment Hierarchy", "AdminDashboardFragment, EmployeeDashboardFragment, ClientDashboardFragment, TasksFragment, ManageUsersFragment, ProfileFragment.")
    add_bullet(doc, "Adapter Tier", "TaskAdapter, UserAdapter, AnnouncementAdapter, MessageAdapter bound to RecyclerViews with ViewHolder reuse.")

    add_heading_with_spacing(doc, "4.2 Component Libraries and Reusable Modules", level=2)
    add_bullet(doc, "Google Material 3", "MaterialCardView, MaterialButton, BottomSheetDialog, ExtendedFloatingActionButton.")
    add_bullet(doc, "AndroidX Libraries", "RecyclerView, SwipeRefreshLayout, ConstraintLayout, Core-KTX.")
    add_bullet(doc, "FeedbackDialogHelper", "Centralized dialog module providing reusable scrollable reviews and guest message dialogs.")

    add_heading_with_spacing(doc, "4.3 Best Practices for Designing and Implementing Components", level=2)
    add_bullet(doc, "Loose Coupling", "Fragments communicate with the parent Activity via public interface contracts.")
    add_bullet(doc, "View Inflation Optimization", "Layout hierarchies are kept flat (max 3 levels deep) to eliminate nested rendering overhead.")

    add_heading_with_spacing(doc, "4.4 State Management for Components", level=2)
    add_bullet(doc, "Local Ephemeral State", "Adapter notifyItemChanged() coordinates discrete UI updates on status and unread badge changes.")
    add_bullet(doc, "Persistent Session State", "SessionManager caches auth tokens, display names, and user role flags across app lifecycles.")

    # 5. Data Management
    add_heading_with_spacing(doc, "5. Data Management", level=1)

    add_heading_with_spacing(doc, "5.1 Local Storage and Caching", level=2)
    add_bullet(doc, "SharedPreferences (aas_session_prefs)", "Persists JWT token, user ID, role, first name, and username.")
    add_bullet(doc, "TaskPreferenceManager (aas_task_seen_prefs)", "Stores set of seen task IDs per user ID to manage unread red badges independently across logins.")

    add_heading_with_spacing(doc, "5.2 Data Retrieval and Manipulation using APIs", level=2)
    add_body(doc, "Retrofit interfaces define typed HTTP calls across all core business domains:")
    add_bullet(doc, "GET /tasks & /user/:id/tasks", "Fetches global or assignee-specific task rosters.")
    add_bullet(doc, "PUT /tasks/:id/status", "Updates task lifecycle status (Pending, In Progress, Completed).")
    add_bullet(doc, "GET /users & POST /register", "Loads directory with pagination (10 per page) and adds new staff/clients.")

    add_heading_with_spacing(doc, "5.3 Data Synchronization and Conflict Resolution", level=2)
    add_bullet(doc, "Pull-to-Refresh", "SwipeRefreshLayout integrated on all dashboard views triggers asynchronous server reconciliations.")
    add_bullet(doc, "Optimistic UI Updates", "Adapter reflects state changes instantly; on network failure, state safely rolls back to previous value.")

    add_heading_with_spacing(doc, "5.4 Client-Side Data Validation", level=2)
    add_bullet(doc, "Field Guards", "Form inputs (Task Name, Due Date, Announcements) enforce required field checks and regex validation before network transmission.")
    add_bullet(doc, "Role Authorization", "Action buttons (e.g., '+ Assign Task', '+ Add Announcement') are strictly hidden from Employee and Client interfaces.")

    # 6. Testing and Debugging
    add_heading_with_spacing(doc, "6. Testing and Debugging", level=1)

    add_heading_with_spacing(doc, "6.1 Automated Testing Frameworks", level=2)
    add_bullet(doc, "Espresso / JUnit", "Used for instrumentation testing of critical user authentication and navigation routes.")
    add_bullet(doc, "ADB Automation Scripts", "Python-based automated test scripts verify rapid regression flows, bottom sheet triggers, and screen transitions.")

    add_heading_with_spacing(doc, "6.2 Debugging Tools", level=2)
    add_bullet(doc, "Android Studio Profiler", "Monitors CPU load, memory leaks, and frame render times.")
    add_bullet(doc, "ADB Logcat & Screencap", "Real-time command-line stream logging and instant UI snapshot verification.")

    add_heading_with_spacing(doc, "6.3 Manual Testing and User Acceptance Testing (UAT)", level=2)
    add_body(doc, "Comprehensive end-to-end testing performed on Pixel 7 (API 33) emulator and physical Android hardware:")
    add_bullet(doc, "UAT Scenario 1", "Admin creates announcement → verifies immediate appearance on Staff and Client dashboards.")
    add_bullet(doc, "UAT Scenario 2", "Admin assigns task to Employee → verifies red dot on bottom nav, '1' red badge on task card, and automatic clearance after viewing.")
    add_bullet(doc, "UAT Scenario 3", "Logout click opens confirmation bottom sheet → Cancel keeps session; Confirm terminates session cleanly.")

    add_heading_with_spacing(doc, "6.4 Error and Exception Handling", level=2)
    add_bullet(doc, "HTTP Interceptors", "Handles 401 Unauthorized by clearing session and redirecting to login.")
    add_bullet(doc, "Safe Null Handling", "Defensive checks guard all JSON deserialization and date parsing routines against corrupt payloads.")

    # 7. Build and Deployment
    add_heading_with_spacing(doc, "7. Build and Deployment", level=1)

    add_heading_with_spacing(doc, "7.1 Build Tools (e.g., Xcode, Android Studio)", level=2)
    add_bullet(doc, "Gradle 8.x / 9.x Wrapper", "Standardized multi-module build toolchain using gradlew.bat assembleDebug and assembleRelease.")
    add_bullet(doc, "Android Gradle Plugin (AGP)", "Manages bytecode desugaring, ProGuard/R8 shrinking, and asset packaging.")

    add_heading_with_spacing(doc, "7.2 Deployment Strategies (e.g., App Store, Google Play)", level=2)
    add_bullet(doc, "Enterprise APK Sideloading", "Direct delivery of optimized release packages (AccurateAccounts.apk) for rapid internal deployment.")
    add_bullet(doc, "Google Play Store (Target)", "Prepared for Android App Bundle (AAB) packaging, signed with release keystore for public Play Console publishing.")

    add_heading_with_spacing(doc, "7.3 Optimization and Performance Monitoring", level=2)
    add_bullet(doc, "Binary Optimization", "R8 code shrinking and resource stripping keep final APK under 8MB.")
    add_bullet(doc, "Network Efficiency", "GZIP compression and OkHttp connection reuse minimize cellular data consumption.")

    # 8. Conclusion
    add_heading_with_spacing(doc, "8. Conclusion", level=1)

    add_heading_with_spacing(doc, "8.1 Summary of the Mobile Application Development Design Document", level=2)
    add_body(doc, "The Accurate Account Services mobile platform delivers a robust, secure, and user-friendly mobile solution tailored for financial auditing and client account management. By implementing native Android components, role-governed navigation, proactive notification badges, and a resilient data synchronization model, the application provides an enterprise-ready mobile experience.")

    add_heading_with_spacing(doc, "8.2 Next Steps for the Mobile Application Development Process", level=2)
    add_bullet(doc, "Push Notifications", "Integrate Firebase Cloud Messaging (FCM) for native background alerts on task deadlines and messages.")
    add_bullet(doc, "Offline Mode", "Introduce Room Database persistence to allow local caching and offline drafting of task updates.")
    add_bullet(doc, "Biometric Authentication", "Add Android BiometricPrompt API for fingerprint and face unlock capabilities.")

    # Save output
    output_path = os.path.abspath("docs/Accurate_Accounts_Mobile_App_Project_Report.docx")
    doc.save(output_path)
    print(f"Report successfully generated at: {output_path}")

if __name__ == "__main__":
    create_report()
