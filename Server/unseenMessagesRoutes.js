// unseenMessagesRoutes.js
const express = require("express");
const router = express.Router();

module.exports = (db) => {
    // Route: GET /unseen-messages/all?user_id=1
    router.get("/unseen-messages/all", async (req, res) => {
        const { user_id } = req.query;

        if (!user_id) {
            return res.status(400).json({ error: "user_id is required" });
        }

        // Object to store all unseen counts
        const unseenCounts = {
            personal_chats: {},
            group_chats: {}
        };

        const personalQuery = `
            SELECT sender_id, COUNT(*) as unseen_count
            FROM personal_chats
            WHERE receiver_id = $1 AND is_seen = FALSE
            GROUP BY sender_id
        `;

        const groupQuery = `
            SELECT group_id, COUNT(*) as unseen_count
            FROM group_chat_message_seen
            WHERE user_id = $1 AND is_seen = FALSE
            GROUP BY group_id
        `;

        try {
            // Execute both queries in parallel
            const [personalRes, groupRes] = await Promise.all([
                db.query(personalQuery, [user_id]),
                db.query(groupQuery, [user_id])
            ]);

            personalRes.rows.forEach(row => {
                unseenCounts.personal_chats[row.sender_id] = parseInt(row.unseen_count, 10);
            });

            groupRes.rows.forEach(row => {
                unseenCounts.group_chats[row.group_id] = parseInt(row.unseen_count, 10);
            });

            res.json(unseenCounts);
        } catch (error) {
            console.error("Error fetching unseen messages summary:", error);
            res.status(500).json({ error: "Database error fetching unseen messages" });
        }
    });

    router.get("/unseen-messages", async (req, res) => {
        const { user_id, group_id } = req.query;
        if (!user_id || !group_id) return res.status(400).json({ error: "Missing params" });

        const query = `
            SELECT COUNT(*) AS unseen_count
            FROM group_chat_message_seen
            WHERE user_id = $1 AND group_id = $2 AND is_seen = FALSE
        `;
        try {
            const { rows } = await db.query(query, [user_id, group_id]);
            res.json({ unseen_count: parseInt(rows[0]?.unseen_count || "0", 10) });
        } catch (err) {
            console.error("Error fetching unseen group messages:", err);
            res.status(500).json({ error: "Database error" });
        }
    });

    router.post("/mark-personal-messages-seen", async (req, res) => {
        const { sender_id, receiver_id } = req.body;
        const query = `UPDATE personal_chats SET is_seen = TRUE WHERE sender_id = $1 AND receiver_id = $2`;
        try {
            await db.query(query, [sender_id, receiver_id]);
            res.json({ success: true });
        } catch (err) {
            console.error("Error marking personal messages seen:", err);
            res.status(500).json({ error: "Error updating messages" });
        }
    });

    router.post("/mark-group-messages-seen", async (req, res) => {
        const { user_id, group_id } = req.body;
        const query = `UPDATE group_chat_message_seen SET is_seen = TRUE WHERE user_id = $1 AND group_id = $2 AND is_seen = FALSE`;
        try {
            await db.query(query, [user_id, group_id]);
            res.json({ success: true });
        } catch (err) {
            console.error("Error marking group messages seen:", err);
            res.status(500).json({ error: "Error updating messages" });
        }
    });

    router.get("/messages/unseen-count", async (req, res) => {
        const { user_id, receiver_id } = req.query;
        const query = `SELECT COUNT(*) as unseen_count FROM personal_chats WHERE sender_id = $1 AND receiver_id = $2 AND is_seen = FALSE`;
        try {
            const { rows } = await db.query(query, [user_id, receiver_id]);
            res.json({ unseen_count: parseInt(rows[0]?.unseen_count || "0", 10) });
        } catch (err) {
            console.error("Error fetching unseen count:", err);
            res.status(500).json({ error: "Error fetching unseen count" });
        }
    });

    return router;
};
