const express = require("express");
const dataCache = require("./dataCache");

module.exports = (db) => {
    const router = express.Router();

    // 📌 Add Favourite
    router.post("/favourite", async (req, res) => {
        const { user_id, client_id } = req.body;
        try {
            await db.query(`DELETE FROM client_user_relations WHERE user_id = $1 AND client_id = $2`, [user_id, client_id]);
            await db.query(`
                INSERT INTO client_user_relations (user_id, client_id, relation_type) 
                VALUES ($1, $2, 'Favourite')
            `, [user_id, client_id]);
            res.send("Client marked as favourite.");
        } catch (err) {
            console.error("Error adding favourite:", err);
            res.status(500).send("Error adding to favourite.");
        }
    });

    // ❌ Remove Favourite
    router.delete("/favourite", async (req, res) => {
        const { user_id, client_id } = req.body;
        try {
            await db.query(`
                DELETE FROM client_user_relations 
                WHERE user_id = $1 AND client_id = $2 AND relation_type = 'Favourite'
            `, [user_id, client_id]);
            res.send("Client removed from favourite.");
        } catch (err) {
            console.error("Error removing favourite:", err);
            res.status(500).send("Error removing from favourite.");
        }
    });

    // ⚡ Add Recent
    router.post("/recent", async (req, res) => {
        const { user_id, client_id } = req.body;
        try {
            // Cleanup old recents (older than 7 days)
            await db.query(`
                DELETE FROM client_user_relations 
                WHERE relation_type = 'Recent' 
                AND created_at < (NOW() - INTERVAL '7 days')
            `);

            await db.query(`DELETE FROM client_user_relations WHERE user_id = $1 AND client_id = $2 AND relation_type = 'Recent'`, [user_id, client_id]);

            await db.query(`
                INSERT INTO client_user_relations (user_id, client_id, relation_type) 
                VALUES ($1, $2, 'Recent')
            `, [user_id, client_id]);
            res.send("Client added to recent.");
        } catch (err) {
            console.error("Error adding recent:", err);
            res.status(500).send("Error adding to recent.");
        }
    });

    // 📄 Get all relations for a user
    router.get("/:userId", (req, res) => {
        const userId = req.params.userId;
        const results = dataCache.getClientRelations(userId);
        res.json(results);
    });

    return router;
};
