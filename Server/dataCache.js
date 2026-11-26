const db = require("./db");

class DataCache {
    constructor() {
        this.users = [];
        this.groups = [];
        this.groupMembers = [];
        this.groupMessages = [];
        this.personalMessages = [];
        this.announcements = [];

        // New Cache Arrays
        this.clients = [];
        this.clientContacts = [];
        this.tasks = [];
        this.folders = [];
        this.folderConnections = [];
        this.importantDocuments = [];
        this.otherDocuments = [];
        this.feedbacks = [];
        this.guestMessages = [];
        this.clientUserRelations = [];
    }

    start(intervalMs = 30000) {
        this.refresh();
        setInterval(() => this.refresh(), intervalMs);
    }

    async refresh() {
        try {
            // --- USERS ---
            const userQuery = `
                SELECT 
                    u.user_id, u.auth_id, a.username, a.role, 
                    u.first_name, u.last_name, u.email, u.phone, 
                    u.profile_pic, u.created_at, u.updated_at 
                FROM users u 
                INNER JOIN auth a ON u.auth_id = a.auth_id
                ORDER BY u.user_id ASC`;

            // --- GROUPS & MESSAGES ---
            const groupQuery = `SELECT group_id, group_name, created_at FROM group_chats ORDER BY created_at DESC`;
            const groupMembersQuery = `
                SELECT gcm.group_id, u.user_id, u.first_name, u.last_name, u.profile_pic, a.role
                FROM group_chat_members gcm
                JOIN users u ON gcm.user_id = u.user_id
                JOIN auth a ON u.auth_id = a.auth_id`;
            const groupMsgsQuery = `
                SELECT 
                    gcm.message_id, gcm.group_id, gcm.sender_id, gcm.message, gcm.created_at,
                    u.first_name, u.last_name, u.profile_pic
                FROM group_chat_messages gcm
                JOIN users u ON gcm.sender_id = u.user_id
                WHERE gcm.created_at >= (NOW() - INTERVAL '7 days')
                ORDER BY gcm.created_at ASC`;
            const personalMsgsQuery = `
                SELECT 
                    pc.chat_id, pc.sender_id, pc.receiver_id, pc.message, pc.created_at,
                    u1.first_name as sender_first_name, u1.last_name as sender_last_name, u1.profile_pic as sender_profile_pic,
                    u2.first_name as receiver_first_name, u2.last_name as receiver_last_name, u2.profile_pic as receiver_profile_pic
                FROM personal_chats pc
                JOIN users u1 ON pc.sender_id = u1.user_id
                JOIN users u2 ON pc.receiver_id = u2.user_id
                WHERE pc.created_at >= (NOW() - INTERVAL '7 days')
                ORDER BY created_at ASC`;

            // --- CLIENTS & DOCUMENTS ---
            const clientQuery = `
                SELECT c.client_id, c.auth_id, a.username, a.role,
                       c.company_name, c.contact_person, c.email,
                       c.gstin, c.pan_number, c.profile_pic,
                       COALESCE((SELECT phone FROM client_contacts WHERE client_id = c.client_id ORDER BY contact_id LIMIT 1), '') AS phone,
                       c.created_at, c.updated_at
                FROM clients c
                INNER JOIN auth a ON c.auth_id = a.auth_id`;

            const taskQuery = `
                SELECT 
                    t.task_id, t.task_name, t.status, t.due_date, t.created_at, t.priority, t.group_id,
                    t.assigned_by, t.assigned_to,
                    assignedBy.first_name AS assigned_by_first_name, assignedBy.last_name AS assigned_by_last_name,
                    assignedTo.first_name AS assigned_to_first_name, assignedTo.last_name AS assigned_to_last_name,
                    gc.group_name
                FROM tasks t
                LEFT JOIN users assignedBy ON t.assigned_by = assignedBy.user_id
                LEFT JOIN users assignedTo ON t.assigned_to = assignedTo.user_id
                LEFT JOIN group_chats gc ON t.group_id = gc.group_id
                ORDER BY 
                    CASE WHEN t.priority = 'High' THEN 1
                         WHEN t.priority = 'Medium' THEN 2
                         WHEN t.priority = 'Low' THEN 3
                         ELSE 4 END,
                    t.due_date ASC`;

            const feedbackQuery = `
                SELECT f.feedback_id, f.message, f.created_at, c.company_name, c.contact_person
                FROM feedback f
                JOIN clients c ON f.client_id = c.client_id
                ORDER BY f.created_at DESC`;

            // Execute all queries in parallel for maximum performance
            const [
                usersRes,
                groupsRes,
                groupMembersRes,
                groupMessagesRes,
                personalMessagesRes,
                announcementsRes,
                clientsRes,
                clientContactsRes,
                tasksRes,
                foldersRes,
                folderConnectionsRes,
                importantDocumentsRes,
                otherDocumentsRes,
                feedbacksRes,
                guestMessagesRes,
                clientUserRelationsRes
            ] = await Promise.all([
                db.query(userQuery),
                db.query(groupQuery),
                db.query(groupMembersQuery),
                db.query(groupMsgsQuery),
                db.query(personalMsgsQuery),
                db.query(`SELECT a.announcement_id, a.auth_id, a.title, a.message, COALESCE(a.created_by, 'Admin') as created_by, COALESCE(a.announcement_date, TO_CHAR(a.created_at, 'YYYY-MM-DD')) as announcement_date, a.created_at FROM announcements a ORDER BY a.announcement_id DESC`),
                db.query(clientQuery),
                db.query(`SELECT contact_id, client_id, contact_name, email, phone FROM client_contacts`),
                db.query(taskQuery),
                db.query(`SELECT folder_id, folder_name FROM folders`),
                db.query(`SELECT folder_id, client_id FROM folder_connections`),
                db.query(`SELECT doc_id, client_id, doc_type as doc_name, doc_type, folder_id, created_at FROM important_documents`),
                db.query(`SELECT doc_id, client_id, doc_name, folder_id, created_at FROM other_documents`),
                db.query(feedbackQuery),
                db.query(`SELECT message_id, guest_name, guest_email, message, created_at FROM guest_messages ORDER BY created_at DESC`),
                db.query(`SELECT client_id, user_id, relation_type FROM client_user_relations`)
            ]);

            // Update Cache
            this.users = usersRes.rows;
            this.groups = groupsRes.rows;
            this.groupMembers = groupMembersRes.rows;
            this.groupMessages = groupMessagesRes.rows;
            this.personalMessages = personalMessagesRes.rows;
            this.announcements = announcementsRes.rows;
            this.clients = clientsRes.rows;
            this.clientContacts = clientContactsRes.rows;
            this.tasks = tasksRes.rows;
            this.folders = foldersRes.rows;
            this.folderConnections = folderConnectionsRes.rows;
            this.importantDocuments = importantDocumentsRes.rows;
            this.otherDocuments = otherDocumentsRes.rows;
            this.feedbacks = feedbacksRes.rows;
            this.guestMessages = guestMessagesRes.rows;
            this.clientUserRelations = clientUserRelationsRes.rows;

        } catch (error) {
            console.error("❌ DataCache Refresh Error:", error.message);
        }
    }

    // --- GETTERS ---

    // USERS
    getUsers() { return this.users; }
    getUser(id) { return this.users.find(u => u.user_id == id); }
    getUserByAuthId(authId) { return this.users.find(u => u.auth_id == authId); }

    // CHATS
    getGroups(userId) {
        const userGroupIds = new Set(this.groupMembers.filter(m => m.user_id == userId).map(m => m.group_id));
        return this.groups
            .filter(g => userGroupIds.has(g.group_id))
            .map(g => ({ ...g, message_count: this.groupMessages.filter(m => m.group_id == g.group_id).length }));
    }
    getAllGroups() {
        return this.groups.map(g => ({ ...g, message_count: this.groupMessages.filter(m => m.group_id == g.group_id).length }));
    }
    getGroupMembers(groupId) { return this.groupMembers.filter(m => m.group_id == groupId); }
    getGroupMessages(groupId) { return this.groupMessages.filter(m => m.group_id == groupId); }
    getPersonalMessages(userId, otherUserId) {
        return this.personalMessages.filter(m =>
            (m.sender_id == userId && m.receiver_id == otherUserId) ||
            (m.sender_id == otherUserId && m.receiver_id == userId)
        );
    }
    getAllAnnouncements() { return this.announcements; }

    // CLIENTS
    getClients() {
        return this.clients.map(client => ({
            ...client,
            contacts: this.clientContacts.filter(c => c.client_id === client.client_id)
        }));
    }
    getClient(id) {
        const client = this.clients.find(c => c.client_id == id);
        if (!client) return null;
        return {
            ...client,
            contacts: this.clientContacts.filter(c => c.client_id === client.client_id)
        };
    }
    getClientByAuthId(authId) {
        const client = this.clients.find(c => c.auth_id == authId);
        if (!client) return null;
        return {
            ...client,
            contacts: this.clientContacts.filter(c => c.client_id === client.client_id)
        };
    }

    // CLIENT DATA (Documents & Folders)
    getClientData(clientId) {
        const targetClients = clientId ? this.clients.filter(c => c.client_id == clientId) : this.clients;

        return targetClients.map(client => {
            const clientConnections = this.folderConnections.filter(fc => fc.client_id == client.client_id);

            const clientFolders = clientConnections.map(conn => {
                const folderDef = this.folders.find(f => f.folder_id == conn.folder_id);
                return {
                    folder_id: conn.folder_id,
                    folder_name: folderDef ? folderDef.folder_name : 'Unknown',
                    importantDocuments: [],
                    otherDocuments: []
                };
            });

            const clientImpDocs = this.importantDocuments.filter(d => d.client_id == client.client_id);
            const clientOtherDocs = this.otherDocuments.filter(d => d.client_id == client.client_id);

            clientImpDocs.forEach(doc => {
                if (doc.folder_id) {
                    const f = clientFolders.find(cf => cf.folder_id == doc.folder_id);
                    if (f) f.importantDocuments.push(doc);
                }
            });
            clientOtherDocs.forEach(doc => {
                if (doc.folder_id) {
                    const f = clientFolders.find(cf => cf.folder_id == doc.folder_id);
                    if (f) f.otherDocuments.push(doc);
                }
            });

            return {
                ...client,
                importantDocuments: clientImpDocs, 
                folders: clientFolders,
                otherDocuments: [] 
            };
        });
    }

    // TASKS
    getTasks() { return this.tasks; }
    getUserTasks(userId) { return this.tasks.filter(t => t.assigned_to == userId); }

    // FOLDERS
    getRemainingFolders(clientId) {
        const connectedFolderIds = new Set(this.folderConnections
            .filter(fc => fc.client_id == clientId)
            .map(fc => fc.folder_id));

        return this.folders.filter(f => f.folder_id != 1 && !connectedFolderIds.has(f.folder_id));
    }

    // FEEDBACK & GUEST MESSAGES
    getAllFeedbacks() { return this.feedbacks; }
    getFeedbackCount() { return this.feedbacks.length; }
    getFeedbackCountYesterday() {
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);
        yesterday.setHours(0, 0, 0, 0);
        return this.feedbacks.filter(f => new Date(f.created_at) >= yesterday).length;
    }

    getAllGuestMessages() { return this.guestMessages; }
    getGuestMessageCount() { return this.guestMessages.length; }
    getGuestMessageCountYesterday() {
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);
        yesterday.setHours(0, 0, 0, 0);
        return this.guestMessages.filter(m => new Date(m.created_at) >= yesterday).length;
    }

    // CLIENT RELATIONS
    getClientRelations(userId) {
        return this.clientUserRelations
            .filter(r => r.user_id == userId)
            .map(r => {
                const client = this.clients.find(c => c.client_id == r.client_id);
                if (!client) return null;
                return {
                    client_id: r.client_id,
                    relation_type: r.relation_type,
                    company_name: client.company_name,
                    contact_person: client.contact_person,
                    email: client.email,
                    profile_pic: client.profile_pic
                };
            })
            .filter(r => r !== null);
    }
}

module.exports = new DataCache();
