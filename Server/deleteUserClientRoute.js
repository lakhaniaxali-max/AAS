const express = require("express");

module.exports = (db, io) => {
    const router = express.Router();

    // DELETE route to delete a user (employee/admin) or a client
    router.delete("/:type/:id", async (req, res) => {
        const { type, id } = req.params;

        if (!["user", "client"].includes(type)) {
            return res.status(400).json({ message: "Invalid type." });
        }

        const client = await db.connect();
        try {
            await client.query("BEGIN");

            if (type === "user") {
                const userRes = await client.query(`SELECT auth_id FROM users WHERE user_id = $1`, [id]);
                if (userRes.rows.length === 0) {
                    await client.query("ROLLBACK");
                    return res.status(404).json({ message: "User not found" });
                }

                const authId = userRes.rows[0].auth_id;

                // Cleanup related data
                await client.query(`DELETE FROM client_user_relations WHERE user_id = $1`, [id]);
                await client.query(`DELETE FROM group_chat_message_seen WHERE user_id = $1`, [id]);
                await client.query(`DELETE FROM group_chat_messages WHERE sender_id = $1`, [id]);
                await client.query(`DELETE FROM group_chat_members WHERE user_id = $1`, [id]);
                await client.query(`DELETE FROM personal_chats WHERE sender_id = $1 OR receiver_id = $2`, [id, id]);
                await client.query(`DELETE FROM tasks WHERE assigned_by = $1 OR assigned_to = $2`, [id, id]);
                
                try {
                    await client.query(`UPDATE group_chats SET created_by = NULL WHERE created_by = $1`, [id]);
                } catch (e) {
                    console.warn("Could not nullify GroupChat owner:", e.message);
                }

                await client.query(`DELETE FROM users WHERE user_id = $1`, [id]);
                await client.query(`DELETE FROM announcements WHERE auth_id = $1`, [authId]);
                await client.query(`DELETE FROM auth WHERE auth_id = $1`, [authId]);

            } else if (type === "client") {
                const clientRes = await client.query(`SELECT auth_id FROM clients WHERE client_id = $1`, [id]);
                if (clientRes.rows.length === 0) {
                    await client.query("ROLLBACK");
                    return res.status(404).json({ message: "Client not found" });
                }

                const authId = clientRes.rows[0].auth_id;

                await client.query(`DELETE FROM client_user_relations WHERE client_id = $1`, [id]);
                await client.query(`DELETE FROM feedback WHERE client_id = $1`, [id]);
                await client.query(`DELETE FROM client_contacts WHERE client_id = $1`, [id]);
                await client.query(`DELETE FROM important_documents WHERE client_id = $1`, [id]);
                await client.query(`DELETE FROM other_documents WHERE client_id = $1`, [id]);
                await client.query(`DELETE FROM folder_connections WHERE client_id = $1`, [id]);
                await client.query(`DELETE FROM clients WHERE client_id = $1`, [id]);
                await client.query(`DELETE FROM auth WHERE auth_id = $1`, [authId]);
            }

            await client.query("COMMIT");
            io.emit("user_deleted", { id, type });
            res.status(200).json({ status: "success", message: `${type} deleted successfully.` });

        } catch (err) {
            await client.query("ROLLBACK");
            console.error(`Deletion error for ${type}:`, err.message);
            res.status(500).json({ status: "error", message: "Deletion failed.", details: err.message });
        } finally {
            client.release();
        }
    });

    return router;
};
