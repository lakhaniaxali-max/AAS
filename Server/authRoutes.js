const express = require("express");

module.exports = (db) => {
    const router = express.Router();

    // Authentication route (PostgreSQL async/await)
    router.post("/", async (req, res) => {
        const { username, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({ error: "Username and password required" });
        }

        const query = `
            SELECT 
                a.auth_id, a.username, a.password, a.role,
                u.user_id, u.first_name, u.last_name,
                c.client_id, c.company_name, c.contact_person, c.gstin, c.pan_number,
                COALESCE(u.email, c.email, '') AS email,
                COALESCE(u.phone, (SELECT phone FROM client_contacts WHERE client_id = c.client_id ORDER BY contact_id LIMIT 1), '') AS phone
            FROM auth a
            LEFT JOIN users u ON a.auth_id = u.auth_id
            LEFT JOIN clients c ON a.auth_id = c.auth_id
            WHERE a.username = $1 
               OR (a.username = 'admin' AND ($1 = 'admin123' OR $1 = 'admin'))
               OR (a.username = 'employee' AND ($1 = 'employee123' OR $1 = 'employee'))
               OR (a.username = 'client' AND ($1 = 'client123' OR $1 = 'client'))`;

        try {
            const { rows } = await db.query(query, [username]);

            if (rows.length === 0) {
                return res.status(401).json({ error: "Invalid credentials" });
            }

            const user = rows[0];

            if (user.password !== password) {
                return res.status(401).json({ error: "Invalid credentials" });
            }

            // Success
            res.json({ message: "Login successful", user });
        } catch (err) {
            console.error("❌ Database error during login:", err);
            res.status(500).json({ error: "Internal Server Error" });
        }
    });

    return router;
};
