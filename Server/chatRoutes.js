// chatRoutes.js
const express = require("express");
const dataCache = require("./dataCache");

module.exports = (db, io) => {
  const router = express.Router();

  // GET chat messages (personal or group)
  router.get("/messages", (req, res) => {
    const { user_id, other_user_id, group_id } = req.query;

    if (!user_id) {
      return res.status(400).json({ error: "user_id is required" });
    }

    let results = [];
    if (other_user_id && !group_id) {
      results = dataCache.getPersonalMessages(user_id, other_user_id);
    } else if (group_id) {
      results = dataCache.getGroupMessages(group_id);
    } else {
      return res.status(400).json({ error: "Invalid parameters" });
    }

    const page = req.query.page ? parseInt(req.query.page) : null;
    const limit = req.query.limit ? parseInt(req.query.limit) : 50;

    if (page && limit) {
      const offset = (page - 1) * limit;
      const paginatedResults = results.slice(offset, offset + limit);
      return res.json({
        data: paginatedResults,
        pagination: {
          currentPage: page,
          pageSize: limit,
          total: results.length,
          totalPages: Math.ceil(results.length / limit)
        }
      });
    }

    return res.json(results);
  });

  // POST new message
  router.post("/messages", async (req, res) => {
    const { sender_id, receiver_id, group_id, message } = req.body;

    if (!sender_id || !message) {
      return res.status(400).json({ error: "sender_id and message are required" });
    }

    try {
      if (receiver_id && !group_id) {
        const query = `
          INSERT INTO personal_chats (sender_id, receiver_id, message) 
          VALUES ($1, $2, $3) 
          RETURNING chat_id`;
        const result = await db.query(query, [sender_id, receiver_id, message]);
        const chat_id = result.rows[0].chat_id;
        const senderUser = dataCache.getUser(sender_id);
        const messageData = {
          message_id: chat_id, 
          sender_id, 
          receiver_id, 
          message,
          created_at: new Date().toISOString(),
          first_name: senderUser?.first_name, 
          last_name: senderUser?.last_name, 
          profile_pic: senderUser?.profile_pic
        };
        io.to(`user_${receiver_id}`).emit("receive_message", messageData);
        io.to(`user_${sender_id}`).emit("receive_message", messageData);
        res.json({ success: true, chat_id });
      } else if (group_id) {
        const query = `
          INSERT INTO group_chat_messages (group_id, sender_id, message) 
          VALUES ($1, $2, $3) 
          RETURNING message_id`;
        const result = await db.query(query, [group_id, sender_id, message]);
        const message_id = result.rows[0].message_id;
        const senderUser = dataCache.getUser(sender_id);
        const messageData = {
          message_id, 
          sender_id, 
          group_id, 
          message,
          created_at: new Date().toISOString(),
          first_name: senderUser?.first_name, 
          last_name: senderUser?.last_name, 
          profile_pic: senderUser?.profile_pic
        };
        io.to(`group_${group_id}`).emit("receive_message", messageData);
        res.json({ success: true, message_id });
      } else {
        res.status(400).json({ error: "Invalid parameters" });
      }
    } catch (err) {
      console.error("Error sending message:", err);
      res.status(500).json({ error: "Error sending message" });
    }
  });

  // GET user's groups
  router.get("/groups", (req, res) => {
    const { user_id } = req.query;
    if (!user_id) return res.status(400).json({ error: "user_id is required" });
    res.json(dataCache.getGroups(user_id));
  });

  // GET group members
  router.get("/groups/:group_id/members", (req, res) => {
    res.json(dataCache.getGroupMembers(req.params.group_id));
  });

  // GET all group names
  router.get("/all-groups", (req, res) => {
    res.json(dataCache.getAllGroups());
  });

  // GET all groups (alias for ManageUsersMain)
  router.get("/groups/all", (req, res) => {
    res.json(dataCache.getAllGroups());
  });

  // POST create new group
  router.post("/groups", async (req, res) => {
    const { group_name, creator_id, members } = req.body;
    if (!group_name || !creator_id || !members || !Array.isArray(members)) {
      return res.status(400).json({ error: "Invalid input" });
    }

    const client = await db.connect();
    try {
      await client.query("BEGIN");
      const insertGroupRes = await client.query(
        "INSERT INTO group_chats (group_name, created_by) VALUES ($1, $2) RETURNING group_id", 
        [group_name, creator_id]
      );
      const group_id = insertGroupRes.rows[0].group_id;
      const membersToAdd = [...new Set(members)];
      
      for (const uid of membersToAdd) {
        await client.query(
          "INSERT INTO group_chat_members (group_id, user_id) VALUES ($1, $2)",
          [group_id, uid]
        );
      }

      await client.query("COMMIT");
      res.json({ success: true, group_id });
    } catch (err) {
      await client.query("ROLLBACK");
      console.error("Error creating group:", err);
      res.status(500).json({ error: "Error creating group: " + err.message });
    } finally {
      client.release();
    }
  });

  // PUT update group
  router.put("/groups/:group_id", async (req, res) => {
    const { group_id } = req.params;
    const { group_name, members } = req.body;
    if (!group_name) return res.status(400).json({ error: "group_name is required" });

    const client = await db.connect();
    try {
      await client.query("BEGIN");
      await client.query("UPDATE group_chats SET group_name = $1 WHERE group_id = $2", [group_name, group_id]);
      if (members && Array.isArray(members)) {
        await client.query("DELETE FROM group_chat_members WHERE group_id = $1", [group_id]);
        for (const uid of members) {
          await client.query("INSERT INTO group_chat_members (group_id, user_id) VALUES ($1, $2)", [group_id, uid]);
        }
      }
      await client.query("COMMIT");
      res.json({ success: true, message: "Group updated" });
    } catch (err) {
      await client.query("ROLLBACK");
      console.error("Error updating group:", err);
      res.status(500).json({ error: "Update failed" });
    } finally {
      client.release();
    }
  });

  // POST add member
  router.post("/groups/:group_id/members", async (req, res) => {
    const { group_id } = req.params;
    const { user_id } = req.body;
    try {
      await db.query("INSERT INTO group_chat_members (group_id, user_id) VALUES ($1, $2)", [group_id, user_id]);
      res.json({ success: true });
    } catch (err) {
      if (err.code === '23505') return res.json({ message: "Already a member" });
      console.error("Error adding member:", err);
      res.status(500).json({ error: "Error adding member" });
    }
  });

  // DELETE remove member
  router.delete("/groups/:group_id/members/:user_id", async (req, res) => {
    try {
      await db.query("DELETE FROM group_chat_members WHERE group_id = $1 AND user_id = $2", [req.params.group_id, req.params.user_id]);
      res.json({ success: true });
    } catch (err) {
      console.error("Error removing member:", err);
      res.status(500).json({ error: "Error removing member" });
    }
  });

  // DELETE group
  router.delete("/groups/:group_id", async (req, res) => {
    const { group_id } = req.params;
    const client = await db.connect();
    try {
      await client.query("BEGIN");
      await client.query("DELETE FROM group_chat_members WHERE group_id = $1", [group_id]);
      await client.query("DELETE FROM group_chat_message_seen WHERE group_id = $1", [group_id]);
      await client.query("DELETE FROM group_chat_messages WHERE group_id = $1", [group_id]);
      await client.query("UPDATE tasks SET group_id = NULL WHERE group_id = $1", [group_id]);
      await client.query("DELETE FROM group_chats WHERE group_id = $1", [group_id]);
      await client.query("COMMIT");
      res.json({ success: true });
    } catch (err) {
      await client.query("ROLLBACK");
      console.error("Error deleting group:", err);
      res.status(500).json({ error: "Delete failed" });
    } finally {
      client.release();
    }
  });

  return router;
};
