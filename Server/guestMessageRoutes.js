// guestMessageRoutes.js
const express = require("express");
const dataCache = require("./dataCache");

module.exports = (db) => {
    const router = express.Router();

    // Route to add a guest message directly to PostgreSQL
    router.post("/", async (req, res) => {
        const { guest_name, guest_email, message } = req.body;

        if (!guest_name || !guest_email || !message) {
            return res.status(400).json({ error: "All fields are required" });
        }

        const query = `
            INSERT INTO guest_messages (guest_name, guest_email, message)
            VALUES ($1, $2, $3)
            RETURNING message_id, guest_name, guest_email, message, created_at
        `;

        try {
            const result = await db.query(query, [guest_name.trim(), guest_email.trim(), message.trim()]);
            dataCache.refresh().catch(() => {});
            res.status(201).json({
                success: true,
                message: "Message sent successfully!",
                message_id: result.rows[0].message_id,
                data: result.rows[0]
            });
        } catch (err) {
            console.error("Error inserting guest message:", err);
            res.status(500).json({ error: "Internal Server Error" });
        }
    });

    // Route to get all guest messages live from PostgreSQL (for Admin & Employee dashboards)
    router.get("/", async (req, res) => {
        try {
            const { rows } = await db.query(
                `SELECT message_id, guest_name, guest_email, message, created_at
                 FROM guest_messages
                 ORDER BY created_at DESC`
            );
            res.status(200).json(rows);
        } catch (err) {
            console.error("Error fetching guest messages:", err);
            res.status(200).json(dataCache.getAllGuestMessages());
        }
    });

    // Live total count from PostgreSQL
    router.get("/count", async (req, res) => {
        try {
            const { rows } = await db.query(`SELECT COUNT(*)::int AS count FROM guest_messages`);
            res.status(200).json({ count: rows[0].count });
        } catch (err) {
            console.error("Error counting guest messages:", err);
            res.status(200).json({ count: dataCache.getGuestMessageCount() });
        }
    });

    // Live count from today/yesterday
    router.get("/count/yesterday", async (req, res) => {
        try {
            const { rows } = await db.query(
                `SELECT COUNT(*)::int AS count FROM guest_messages WHERE created_at >= CURRENT_DATE`
            );
            res.status(200).json({ count: rows[0].count });
        } catch (err) {
            res.status(200).json({ count: dataCache.getGuestMessageCountYesterday() });
        }
    });

    return router;
};
