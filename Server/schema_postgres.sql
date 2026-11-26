-- ==============================================================================
-- Accurate Account Services - PostgreSQL Database Schema
-- Compatible with PostgreSQL 13, 14, 15, 16+
-- ==============================================================================

-- Drop existing tables (in reverse foreign-key order) if resetting
DROP TABLE IF EXISTS guest_messages CASCADE;
DROP TABLE IF EXISTS feedback CASCADE;
DROP TABLE IF EXISTS tasks CASCADE;
DROP TABLE IF EXISTS personal_chats CASCADE;
DROP TABLE IF EXISTS other_documents CASCADE;
DROP TABLE IF EXISTS important_documents CASCADE;
DROP TABLE IF EXISTS group_chat_message_seen CASCADE;
DROP TABLE IF EXISTS group_chat_messages CASCADE;
DROP TABLE IF EXISTS group_chat_members CASCADE;
DROP TABLE IF EXISTS group_chats CASCADE;
DROP TABLE IF EXISTS folder_connections CASCADE;
DROP TABLE IF EXISTS folders CASCADE;
DROP TABLE IF EXISTS client_user_relations CASCADE;
DROP TABLE IF EXISTS client_contacts CASCADE;
DROP TABLE IF EXISTS clients CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS announcements CASCADE;
DROP TABLE IF EXISTS auth CASCADE;

-- 1. AUTH TABLE
CREATE TABLE auth (
    auth_id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('admin', 'employee', 'client')),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 2. USERS TABLE (Employees & Admins)
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    auth_id INT REFERENCES auth(auth_id) ON DELETE CASCADE,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    profile_pic TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 3. CLIENTS TABLE
CREATE TABLE clients (
    client_id SERIAL PRIMARY KEY,
    auth_id INT REFERENCES auth(auth_id) ON DELETE CASCADE,
    company_name VARCHAR(100) NOT NULL,
    contact_person VARCHAR(100),
    email VARCHAR(100),
    gstin VARCHAR(15),
    pan_number VARCHAR(10),
    profile_pic TEXT,
    address TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 4. ANNOUNCEMENTS TABLE
CREATE TABLE announcements (
    announcement_id SERIAL PRIMARY KEY,
    auth_id INT REFERENCES auth(auth_id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 5. CLIENT CONTACTS TABLE
CREATE TABLE client_contacts (
    contact_id SERIAL PRIMARY KEY,
    client_id INT REFERENCES clients(client_id) ON DELETE CASCADE,
    contact_name VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 6. CLIENT USER RELATIONS (Favourites & Recents)
CREATE TABLE client_user_relations (
    relation_id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(user_id) ON DELETE CASCADE,
    client_id INT REFERENCES clients(client_id) ON DELETE CASCADE,
    relation_type VARCHAR(20) CHECK (relation_type IN ('Favourite', 'Recent')),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 7. FOLDERS TABLE
CREATE TABLE folders (
    folder_id SERIAL PRIMARY KEY,
    folder_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 8. FOLDER CONNECTIONS TABLE
CREATE TABLE folder_connections (
    connection_id SERIAL PRIMARY KEY,
    client_id INT REFERENCES clients(client_id) ON DELETE CASCADE,
    folder_id INT REFERENCES folders(folder_id) ON DELETE CASCADE
);

-- 9. GROUP CHATS TABLE
CREATE TABLE group_chats (
    group_id SERIAL PRIMARY KEY,
    group_name VARCHAR(100) NOT NULL,
    created_by INT REFERENCES users(user_id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 10. GROUP CHAT MEMBERS TABLE
CREATE TABLE group_chat_members (
    membership_id SERIAL PRIMARY KEY,
    group_id INT REFERENCES group_chats(group_id) ON DELETE CASCADE,
    user_id INT REFERENCES users(user_id) ON DELETE CASCADE
);

-- 11. GROUP CHAT MESSAGES TABLE
CREATE TABLE group_chat_messages (
    message_id SERIAL PRIMARY KEY,
    group_id INT REFERENCES group_chats(group_id) ON DELETE CASCADE,
    sender_id INT REFERENCES users(user_id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 12. GROUP CHAT MESSAGE SEEN TABLE
CREATE TABLE group_chat_message_seen (
    seen_id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(user_id) ON DELETE CASCADE,
    group_id INT REFERENCES group_chats(group_id) ON DELETE CASCADE,
    message_id INT REFERENCES group_chat_messages(message_id) ON DELETE CASCADE,
    is_seen BOOLEAN DEFAULT FALSE,
    seen_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 13. IMPORTANT DOCUMENTS TABLE
CREATE TABLE important_documents (
    doc_id SERIAL PRIMARY KEY,
    doc_type VARCHAR(50) NOT NULL CHECK (doc_type IN ('Balance Sheet', 'Profit and Loss', 'Capital Account', 'Aadhar Card', 'PAN Card')),
    client_id INT REFERENCES clients(client_id) ON DELETE CASCADE,
    folder_id INT REFERENCES folders(folder_id) ON DELETE SET NULL,
    doc_data TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 14. OTHER DOCUMENTS TABLE
CREATE TABLE other_documents (
    doc_id SERIAL PRIMARY KEY,
    doc_name VARCHAR(255) NOT NULL,
    doc_data TEXT NOT NULL,
    client_id INT REFERENCES clients(client_id) ON DELETE CASCADE,
    folder_id INT REFERENCES folders(folder_id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 15. PERSONAL CHATS TABLE
CREATE TABLE personal_chats (
    chat_id SERIAL PRIMARY KEY,
    sender_id INT REFERENCES users(user_id) ON DELETE CASCADE,
    receiver_id INT REFERENCES users(user_id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    is_seen BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 16. TASKS TABLE
CREATE TABLE tasks (
    task_id SERIAL PRIMARY KEY,
    task_name VARCHAR(100) NOT NULL,
    assigned_by INT REFERENCES users(user_id) ON DELETE SET NULL,
    assigned_to INT REFERENCES users(user_id) ON DELETE SET NULL,
    group_id INT REFERENCES group_chats(group_id) ON DELETE SET NULL,
    status VARCHAR(20) DEFAULT 'Pending' CHECK (status IN ('Pending', 'In Progress', 'Completed')),
    due_date DATE,
    priority VARCHAR(10) DEFAULT 'Medium' CHECK (priority IN ('High', 'Medium', 'Low')),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 17. FEEDBACK TABLE
CREATE TABLE feedback (
    feedback_id SERIAL PRIMARY KEY,
    client_id INT REFERENCES clients(client_id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 18. GUEST MESSAGES TABLE
CREATE TABLE guest_messages (
    message_id SERIAL PRIMARY KEY,
    guest_name VARCHAR(100) NOT NULL,
    guest_email VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- OPTIONAL INITIAL SEED DATA (Default Admin Account)
-- ==============================================================================
-- Password here is 'admin123' as an example placeholder:
INSERT INTO auth (username, password, role) 
VALUES ('admin', 'admin123', 'admin')
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (auth_id, first_name, last_name, email, phone)
SELECT auth_id, 'System', 'Admin', 'admin@accurateaccounts.com', '1234567890'
FROM auth WHERE username = 'admin'
LIMIT 1;


