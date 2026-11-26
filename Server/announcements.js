const express = require("express");
const dataCache = require("./dataCache");

module.exports = (db) => {
    const router = express.Router();

    // Add announcement
    router.post("/", async (req, res) => {
        const { auth_id, title, message, created_by, announcement_date } = req.body;

        if (!title || !message) {
            return res.status(400).json({ error: "Title and message are required" });
        }

        const author = created_by || "Admin";
        const dateStr = announcement_date || new Date().toISOString().split("T")[0];

        const query = `
            INSERT INTO announcements (auth_id, title, message, created_by, announcement_date) 
            VALUES ($1, $2, $3, $4, $5) 
            RETURNING *`;

        try {
            const result = await db.query(query, [auth_id || null, title, message, author, dateStr]);
            const newAnnouncement = result.rows[0];

            // Refresh cache so all dashboards get updated data immediately
            if (dataCache && typeof dataCache.fetchData === "function") {
                dataCache.fetchData(db).catch(err => console.error("Cache update error:", err));
            }

            res.status(201).json(newAnnouncement);
        } catch (err) {
            console.error("Error adding announcement:", err);
            return res.status(500).json({
                error: "Failed to add announcement",
                details: err.message
            });
        }
    });

    // Get all announcements
    router.get("/", async (req, res) => {
        try {
            const query = `
                SELECT 
                    a.announcement_id, 
                    a.auth_id, 
                    a.title, 
                    a.message, 
                    COALESCE(a.created_by, u.first_name || ' ' || u.last_name, au.username, 'Admin') AS created_by,
                    COALESCE(a.announcement_date, TO_CHAR(a.created_at, 'YYYY-MM-DD')) AS announcement_date,
                    a.created_at
                FROM announcements a
                LEFT JOIN auth au ON a.auth_id = au.auth_id
                LEFT JOIN users u ON a.auth_id = u.auth_id
                ORDER BY a.announcement_id DESC`;
            const { rows } = await db.query(query);
            res.json(rows);
        } catch (err) {
            console.error("Error fetching announcements, falling back to cache:", err);
            const results = dataCache.getAllAnnouncements();
            res.json(results);
        }
    });

    return router;
};
