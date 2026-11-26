// feedbackRoutes.js
const express = require("express");
const dataCache = require("./dataCache");

module.exports = (db) => {
    const router = express.Router();

    // Submit feedback (Clients) directly into PostgreSQL
    router.post("/submit", async (req, res) => {
        const { client_id, message } = req.body;

        if (!client_id || !message) {
            return res.status(400).json({ error: "Client ID and message are required." });
        }

        const query = "INSERT INTO feedback (client_id, message) VALUES ($1, $2) RETURNING feedback_id";
        try {
            const result = await db.query(query, [client_id, message.trim()]);
            dataCache.refresh().catch(() => {});
            res.status(201).json({
                success: true,
                message: "Feedback submitted successfully.",
                feedback_id: result.rows[0].feedback_id
            });
        } catch (err) {
            console.error("Error submitting feedback:", err);
            return res.status(500).json({ error: "Failed to submit feedback." });
        }
    });

    // Get all feedbacks live from PostgreSQL (For Admin & Employees)
    router.get("/all", async (req, res) => {
        try {
            const { rows } = await db.query(`
                SELECT f.feedback_id, f.message, f.created_at, c.company_name, c.contact_person
                FROM feedback f
                LEFT JOIN clients c ON f.client_id = c.client_id
                ORDER BY f.created_at DESC
            `);
            res.status(200).json(rows);
        } catch (err) {
            console.error("Error fetching feedbacks:", err);
            res.status(200).json(dataCache.getAllFeedbacks());
        }
    });

    router.get("/count", async (req, res) => {
        try {
            const { rows } = await db.query(`SELECT COUNT(*)::int AS count FROM feedback`);
            res.status(200).json({ count: rows[0].count });
        } catch (err) {
            res.status(200).json({ count: dataCache.getFeedbackCount() });
        }
    });

    router.get("/count/yesterday", async (req, res) => {
        try {
            const { rows } = await db.query(
                `SELECT COUNT(*)::int AS count FROM feedback WHERE created_at >= CURRENT_DATE`
            );
            res.status(200).json({ count: rows[0].count });
        } catch (err) {
            res.status(200).json({ count: dataCache.getFeedbackCountYesterday() });
        }
    });

    return router;
};
