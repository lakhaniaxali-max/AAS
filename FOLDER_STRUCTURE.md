# 📂 Accurate Account Services - Project Folder Structure & Architecture

A comprehensive guide and reference map for the **Accurate Account Services (AAS)** codebase.
The project is built as an enterprise-grade multi-platform system consisting of a **Web Client (React / TypeScript)**, a **Backend REST & WebSocket API (Node.js / Express)**, and a **Native Mobile Application (Android / Java)**.

---

## 🏛️ High-Level Directory Overview

```
AccurateAccountServices/
├── 🌐 src/                  # Web Frontend (React 18 + TypeScript + Vite + TailwindCSS)
├── 🖥️ Server/               # Backend API Server (Node.js + Express + MySQL/PostgreSQL + Socket.io)
├── 📱 AndroidApp/           # Native Android Mobile App (Java, Material Design 3, Retrofit)
├── 📚 docs/                 # Project documentation, database schemas, and UI screenshots
├── 📦 release/              # Compiled standalone release packages and production APKs
├── 🛠️ tools/                # Automation testing scripts, emulator drivers, and diagnostic tools
├── 📁 temp/                 # Temporary logs, UI automaton dumps, and runtime inspection files
├── ⚙️ Root Configs          # Vite, Tailwind, TypeScript, ESLint, Vercel, and Git configurations
└── 🚀 run_mobile_server.bat # Dual-launcher for local backend & public Cloudflare mobile tunnel
```

---

## 1. 🖥️ Backend Server (`Server/`)
The backend provides real-time WebSockets, JWT-based authentication, Role-Based Access Control (RBAC), and persistent storage (MySQL / PostgreSQL).

```
Server/
├── index.js                     # Main entry point: initializes Express, Socket.io, middleware & HTTP server
├── db.js                        # Database connection pool configuration (MySQL / PostgreSQL)
├── dataCache.js                 # In-memory synchronization cache with WebSocket event broadcaster
├── schema_postgres.sql          # Full PostgreSQL database schema with tables, indices & constraints
├── .env.example                 # Environment variable templates (DB credentials, JWT secrets, ports)
├── package.json                 # Backend dependencies (express, socket.io, mysql2, pg, bcryptjs, etc.)
│
├── 🛡️ Middleware
│   ├── authMiddleware.js        # JWT token extraction and verification header guard
│   └── roleMiddleware.js        # Strict Role-Based Access Control (Admin, Employee, Client)
│
├── 🛣️ API Route Modules
│   ├── authRoutes.js            # /auth - User authentication and JWT token issuance
│   ├── registerRoutes.js        # /register - Account registration for staff and clients
│   ├── userRoutes.js            # /users - User directory, profile updates, and pagination
│   ├── clientRoutes.js          # /clients - Client management, onboarding, and details
│   ├── clientDataRoutes.js      # /client-data - Client-specific metadata and records
│   ├── clientRelationRoutes.js  # /client-relations - Folder and staff assignments for clients
│   ├── taskRoutes.js            # /tasks - Task assignments, status transitions (Pending/In Progress/Completed)
│   ├── announcements.js         # /announcements - Administrative broadcast announcements
│   ├── chatRoutes.js            # /chat - Group chat history, direct messaging & attachments
│   ├── unseenMessagesRoutes.js  # /unseen-messages - Real-time unread counter tracking
│   ├── folderRoutes.js          # /folders - Digital document hierarchy and access rules
│   ├── uploadImportantRoutes.js # /upload-important - Secure storage for KYC, PAN, and Aadhar proofs
│   ├── uploadOtherRoutes.js     # /upload-other - General balance sheets, statements & Cloudinary uploads
│   ├── feedbackRoutes.js        # /feedback - User and client rating and review endpoints
│   ├── guestMessageRoutes.js    # /guest-messages - Public landing page contact inquiries
│   ├── heartbeatRoutes.js       # /heartbeat - Health check and monitoring endpoint
│   ├── deleteUserClientRoute.js # Cascaded deletion for accounts and linked resources
│   └── updateRoutes.js          # Batch synchronization endpoints
│
└── 📜 Scripts & Utilities
    ├── check_passwords.js       # BCrypt password validation utility
    └── test_connection.js      # Database connectivity verification script
```

---

## 2. 🌐 Web Frontend (`src/`)
Modern React single-page application built with Vite, TypeScript, and TailwindCSS.

```
src/
├── main.tsx                     # React DOM root bootstrapping
├── App.tsx                      # Core router, authentication guards, and context provider tree
├── config.ts                    # Global dynamic environment configuration (Dev vs Prod endpoints)
├── cloudinaryUploads.tsx        # Secure Cloudinary file signing and direct client upload engine
├── index.css                    # Tailwind CSS directives, typography, and global animations
│
├── 💾 Context & State (`src/Data/`)
│   ├── AuthData.tsx             # Authentication context (JWT storage, session state, Axios interceptor)
│   ├── UserData.tsx             # Internal employee and staff state provider
│   ├── ClientData.tsx           # Client database synchronization provider
│   ├── Client.ts                # TypeScript interfaces and entity data types
│   ├── ProfileData.tsx          # Current logged-in user profile state
│   ├── UnseenMessagesContext.tsx# Real-time unread message notifications
│   └── initialUsers.ts          # Mock fixture data for offline testing
│
├── 🧩 UI Components (`src/components/`)
│   ├── Navbar.tsx               # Dynamic navigation bar tailored to user role
│   ├── ProtectedRoute.tsx       # Route guard redirecting unauthorized users
│   ├── LoginPopup.tsx           # Modal authentication dialog
│   ├── Chat.tsx                 # Real-time messaging panel with Socket.io
│   ├── MessagesPopup.tsx        # Quick floating message drawer
│   ├── StatusBadge.tsx          # Status badge components for tasks and documents
│   ├── FeedbackForm.tsx         # Interactive feedback modal
│   ├── ProfileUser.tsx          # Staff user profile view/edit card
│   ├── ProfileClient.tsx        # Client profile card
│   ├── Hero.tsx                 # Public landing page hero banner
│   ├── Footer.tsx               # Public footer
│   └── Testimonials.tsx         # Customer reviews and social proof
│
└── 📄 Pages & Views (`src/pages/`)
    ├── Home.tsx                 # Public company landing page
    ├── About.tsx                # About Accurate Account Services
    ├── Contact.tsx              # Public contact form & map
    ├── FAQ.tsx                  # Frequently Asked Questions
    │
    ├── AdminDashboard.tsx       # Executive dashboard: stats, client feedbacks, announcements, quick tasks
    ├── EmployeeDashboard.tsx    # Staff portal: assigned clients, workflows, task metrics
    ├── ClientDashboard.tsx      # Client portal: uploaded financial files, reports, direct messaging
    │
    ├── DocumentsMain.tsx        # Digital Document Vault
    │   └── Documents/           # ClientCard, FolderButton, DocumentButton, SearchBar, FilterTabs
    │
    ├── ManageUsersMain.tsx      # User & Role Administration directory
    │   └── ManageUsers/         # UserTable, ViewUserModal, UserForm, ClientForm, GroupForm
    │
    └── TaskDashboard.tsx        # Tasks & Deadlines tracking
        └── TasksPage/           # TaskCard, TaskForm, PriorityFilter
```

---

## 3. 📱 Android Native Mobile App (`AndroidApp/`)
Production native Android application built with Material 3, AndroidX, Retrofit 2, and clean separation of concerns.

```
AndroidApp/
├── build.gradle                 # Project-level Gradle build script
├── settings.gradle              # Gradle module inclusions
├── gradlew / gradlew.bat        # Standalone Gradle wrapper binaries
├── local.properties             # Local Android SDK configuration
│
└── app/
    ├── build.gradle             # App-level dependencies (Retrofit, Material, Gson, OkHttp)
    └── src/main/
        ├── AndroidManifest.xml  # Permissions (INTERNET), activities, and themes
        │
        ├── ☕ Java Source Code (`java/com/accurateaccounts/app/`)
        │   ├── 📡 api/
        │   │   ├── ApiClient.java          # Retrofit singleton with OkHttp logging and timeouts
        │   │   └── ApiService.java         # REST endpoints matching backend Express routes
        │   │
        │   ├── 📦 models/
        │   │   ├── User.java               # Staff & Client user entity
        │   │   ├── Task.java               # Task entity with status, priority, dates & new badge
        │   │   ├── CreateTaskRequest.java  # Task creation payload model
        │   │   ├── Announcement.java       # Announcement post model
        │   │   ├── ClientDocument.java     # Digital document metadata model
        │   │   ├── FeedbackItem.java       # Client review model
        │   │   ├── GuestMessageItem.java   # Guest inquiry model
        │   │   ├── LoginRequest.java       # Auth credentials payload
        │   │   ├── LoginResponse.java      # Auth result with JWT token and user info
        │   │   └── CountResponse.java      # Generic metric count payload
        │   │
        │   ├── 📱 ui/
        │   │   ├── landing/
        │   │   │   ├── LandingActivity.java    # Public entry portal (Home, About, Contact, Sign In)
        │   │   │   ├── HomeLandingFragment.java# Public landing hero and company highlights
        │   │   │   ├── AboutFragment.java      # Company information
        │   │   │   ├── ContactFragment.java    # Guest inquiry submission
        │   │   │   └── FAQFragment.java        # Interactive accordion FAQ
        │   │   │
        │   │   ├── auth/
        │   │   │   └── LoginActivity.java      # Native login screen with credential validation
        │   │   │
        │   │   ├── main/
        │   │   │   └── MainActivity.java       # Bottom navigation container, unread badge manager & toolbar
        │   │   │
        │   │   ├── admin/
        │   │   │   ├── AdminDashboardFragment.java # Admin dashboard with stats, announcements FAB & task preview
        │   │   │   ├── ManageUsersFragment.java    # Staff & Client management with pagination (10/page)
        │   │   │   ├── AdminDocumentsFragment.java # Document approval and client verification vault
        │   │   │   ├── AnnouncementAdapter.java    # RecyclerView adapter for administrative announcements
        │   │   │   └── UserAdapter.java            # RecyclerView adapter for staff directory
        │   │   │
        │   │   ├── employee/
        │   │   │   └── EmployeeDashboardFragment.java # Staff portal with Assigned Tasks & red New Tasks counter
        │   │   │
        │   │   ├── client/
        │   │   │   ├── ClientDashboardFragment.java   # Client overview portal
        │   │   │   ├── ClientDocumentsFragment.java   # Minimalist document viewing and upload
        │   │   │   └── DocumentAdapter.java           # RecyclerView adapter for documents
        │   │   │
        │   │   ├── tasks/
        │   │   │   ├── TasksFragment.java      # Tasks & Deadlines screen with role-guarded task creation
        │   │   │   └── TaskAdapter.java        # Task card adapter with red "1" badge & status bottom sheet
        │   │   │
        │   │   ├── chat/
        │   │   │   ├── ChatActivity.java       # Native real-time message chat interface
        │   │   │   └── MessageAdapter.java     # Chat bubble RecyclerView adapter
        │   │   │
        │   │   ├── profile/
        │   │   │   └── ProfileFragment.java    # User account profile, roles, and session logout
        │   │   │
        │   │   └── common/
        │   │       └── FeedbackDialogHelper.java # Scrollable popups for Client Reviews & Guest Messages
        │   │
        │   └── 🛠️ utils/
        │       ├── SessionManager.java         # SharedPreferences manager for JWT tokens and user session
        │       ├── TaskPreferenceManager.java  # Persistent per-user tracking of seen vs new task IDs
        │       └── SmoothNestedScrollView.java # Fluid scroll coordinator for nested dashboards
        │
        └── 🎨 Resources (`res/`)
            ├── drawable/       # Icons, custom gradients, status pills, and red notification badge (bg_badge_red.xml)
            ├── layout/         # XML layouts for activities, fragments, dialogs, bottom sheets, and cards
            ├── menu/           # Role-filtered bottom navigation menu definitions
            └── values/         # Colors (AAS brand palette), themes, strings, and dimensions
```

---

## 4. 📚 Documentation & Reference (`docs/`)
```
docs/
├── Schema of the project.txt    # Database entity relationship and table definitions
└── screenshots/                 # Captured UI verification snapshots across mobile workflows
```

---

## 5. 🛠️ Development & Testing Tools (`tools/`)
```
tools/
├── test_scripts/                # Python and Node.js automation and API verification scripts
└── emulator/                    # Android Emulator Hypervisor Driver (AEHD) & installation batch script
```

---

## 6. 📦 Production Artifacts (`release/`)
```
release/
└── AccurateAccounts.apk         # Compiled standalone Android debug/release package
```

---

## ⚙️ Key Configuration Files

| File | Purpose |
| :--- | :--- |
| **`run_mobile_server.bat`** | Launches local Node backend on port 3000 and tunnels to public HTTPS via Cloudflare. |
| **`package.json`** | React frontend dependencies, build scripts (`npm run dev`, `npm run build`). |
| **`vite.config.ts`** | Vite bundler plugins, dev server port, and module aliases. |
| **`tailwind.config.js`** | Tailwind CSS custom color themes, typography, and responsive breakpoints. |
| **`tsconfig.json`** | Strict TypeScript compilation configuration. |
| **`vercel.json`** | Production reverse-proxy and deployment configuration. |
| **`.gitignore`** | Excludes dependencies (`node_modules`), build outputs (`dist`, `.gradle`), and `temp/`. |
