// folderRoutes.js
const express = require("express");
const dataCache = require("./dataCache");

module.exports = (db) => {
    const router = express.Router();

    // GET remaining folders for a client
    router.get("/remaining/:clientId", (req, res) => {
        const clientId = parseInt(req.params.clientId);

        if (isNaN(clientId)) {
            return res.status(400).json({ error: "Invalid client ID" });
        }

        const results = dataCache.getRemainingFolders(clientId);
        res.json(results);
    });

    // POST create folder and link to client
    router.post("/create", async (req, res) => {
        const { folder_name, client_id } = req.body;

        if (!folder_name || folder_name.trim() === "") {
            return res.status(400).json({ error: "Folder name is required" });
        }

        if (!client_id || isNaN(client_id)) {
            return res.status(400).json({ error: "Valid client ID is required" });
        }

        const client = await db.connect();
        try {
            await client.query("BEGIN");

            // Check if folder exists
            const folderCheck = await client.query("SELECT * FROM folders WHERE folder_name = $1", [folder_name]);

            let folder_id;
            if (folderCheck.rows.length > 0) {
                folder_id = folderCheck.rows[0].folder_id;
            } else {
                const insertFolder = await client.query(
                    "INSERT INTO folders (folder_name) VALUES ($1) RETURNING folder_id", 
                    [folder_name]
                );
                folder_id = insertFolder.rows[0].folder_id;
            }

            // Check if connection already exists
            const connCheck = await client.query(
                "SELECT * FROM folder_connections WHERE client_id = $1 AND folder_id = $2", 
                [client_id, folder_id]
            );
            
            if (connCheck.rows.length > 0) {
                await client.query("ROLLBACK");
                return res.status(400).json({ error: "Folder is already connected to this client" });
            }

            // Create connection
            await client.query(
                "INSERT INTO folder_connections (client_id, folder_id) VALUES ($1, $2)", 
                [client_id, folder_id]
            );

            // Initialize default important docs
            const docTypes = ['Balance Sheet', 'Profit and Loss', 'Capital Account'];
            for (const type of docTypes) {
                await client.query(
                    "INSERT INTO important_documents (doc_type, client_id, folder_id, doc_data) VALUES ($1, $2, $3, $4)",
                    [type, client_id, folder_id, '']
                );
            }

            await client.query("COMMIT");

            res.status(201).json({
                message: "Folder and associated records created successfully",
                folder_id,
                folder_name,
            });

        } catch (err) {
            await client.query("ROLLBACK");
            console.error("Error in folder creation flow:", err);
            res.status(500).json({ error: "Internal server error during folder creation" });
        } finally {
            client.release();
        }
    });

    // POST link existing folder to client
    router.post("/connect", async (req, res) => {
        const { client_id, folder_id } = req.body;

        if (!client_id || !folder_id) {
            return res.status(400).json({ error: "client_id and folder_id are required" });
        }

        const client = await db.connect();
        try {
            await client.query("BEGIN");
            const connCheck = await client.query(
                "SELECT * FROM folder_connections WHERE client_id = $1 AND folder_id = $2", 
                [client_id, folder_id]
            );

            if (connCheck.rows.length > 0) {
                await client.query("ROLLBACK");
                return res.status(400).json({ error: "Folder is already connected to this client" });
            }

            await client.query("INSERT INTO folder_connections (client_id, folder_id) VALUES ($1, $2)", [client_id, folder_id]);

            const docTypes = ['Balance Sheet', 'Profit and Loss', 'Capital Account'];
            for (const type of docTypes) {
                await client.query(
                    "INSERT INTO important_documents (doc_type, client_id, folder_id, doc_data) VALUES ($1, $2, $3, $4)", 
                    [type, client_id, folder_id, '']
                );
            }

            await client.query("COMMIT");

            res.status(201).json({
                message: "Folder linked successfully",
                client_id,
                folder_id
            });
        } catch (err) {
            await client.query("ROLLBACK");
            console.error("Error connecting folder:", err);
            res.status(500).json({ error: "Internal server error" });
        } finally {
            client.release();
        }
    });

    // DELETE folder and all related data
    router.delete("/cleanup", async (req, res) => {
        const { client_id, folder_id } = req.body;

        if (!client_id || !folder_id) {
            return res.status(400).json({ error: "Missing client_id or folder_id" });
        }

        const client = await db.connect();
        try {
            await client.query("BEGIN");
            await client.query("DELETE FROM other_documents WHERE client_id = $1 AND folder_id = $2", [client_id, folder_id]);
            await client.query("DELETE FROM important_documents WHERE client_id = $1 AND folder_id = $2", [client_id, folder_id]);
            await client.query("DELETE FROM folder_connections WHERE client_id = $1 AND folder_id = $2", [client_id, folder_id]);
            await client.query("COMMIT");
            res.json({ message: "Folder documents cleaned and connection removed successfully." });
        } catch (err) {
            await client.query("ROLLBACK");
            console.error("Cleanup error:", err);
            res.status(500).json({ error: "Cleanup failed" });
        } finally {
            client.release();
        }
    });

    // DELETE document (Important or Other)
    router.delete("/document", async (req, res) => {
        const { doc_id, type, only_nullify } = req.body;

        if (!doc_id || !type) {
            return res.status(400).json({ error: "Missing doc_id or type" });
        }

        try {
            if (type === "important") {
                if (only_nullify) {
                    await db.query("UPDATE important_documents SET doc_data = '' WHERE doc_id = $1", [doc_id]);
                } else {
                    await db.query("DELETE FROM important_documents WHERE doc_id = $1", [doc_id]);
                }
            } else if (type === "other") {
                await db.query("DELETE FROM other_documents WHERE doc_id = $1", [doc_id]);
            }
            res.json({ success: true });
        } catch (err) {
            console.error("Document action error:", err);
            res.status(500).json({ error: "Action failed" });
        }
    });

    return router;
};
