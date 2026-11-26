const express = require("express");
const dataCache = require("./dataCache");

module.exports = (db) => {
    const router = express.Router();

    // Fetch all clients sweepingly (Direct DB with Cache fallback)
    router.get("/", async (req, res) => {
        try {
            const { rows } = await db.query(`
              SELECT c.*, a.username, a.role, cc.phone
              FROM clients c
              INNER JOIN auth a ON c.auth_id = a.auth_id
              LEFT JOIN client_contacts cc ON c.client_id = cc.client_id
              ORDER BY c.client_id DESC
            `);
            res.json(rows);
        } catch (err) {
            console.error("Client list fallback error:", err);
            res.json(dataCache.getClients());
        }
    });

    // Fetch a specific client by client_id (Primary ID for lookups)
    router.get("/:id", async (req, res) => {
        const { id } = req.params;
        
        // 1. Try Cache using the client_id primary key
        let client = dataCache.getClient(id);
        if (client) return res.json(client);

        // 2. Fallback to DB (Check by client_id)
        const query = `
            SELECT c.*, a.username, a.role
            FROM clients c
            INNER JOIN auth a ON c.auth_id = a.auth_id
            WHERE c.client_id = $1`;

        try {
            const { rows: results } = await db.query(query, [id]);
            if (results.length > 0) return res.json(results[0]);

            // 3. Last Resort: Try by auth_id (for backwards compatibility during transition)
            const { rows: authResults } = await db.query(`
                SELECT c.*, a.username, a.role
                FROM clients c
                INNER JOIN auth a ON c.auth_id = a.auth_id
                WHERE c.auth_id = $1`, [id]);
            
            if (authResults.length > 0) return res.json(authResults[0]);

            return res.status(404).json({ error: "Client profile not found" });
        } catch (err) {
            console.error("Database error during client fetch:", err);
            res.status(500).json({ error: "Internal server error" });
        }
    });

    return router;
};
