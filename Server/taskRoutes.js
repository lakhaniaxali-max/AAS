const express = require("express");
const dataCache = require("./dataCache");
const router = express.Router();

module.exports = (db) => {
  // GET / - Fetch all tasks
  router.get("/", async (req, res) => {
    let results = dataCache.getTasks();
    
    // DB Fallback if cache is empty
    if (results.length === 0) {
      try {
        const { rows } = await db.query(`
          SELECT t.*, assignedBy.first_name AS assigned_by_first_name, assignedTo.first_name AS assigned_to_first_name, gc.group_name
          FROM tasks t
          LEFT JOIN users assignedBy ON t.assigned_by = assignedBy.user_id
          LEFT JOIN users assignedTo ON t.assigned_to = assignedTo.user_id
          LEFT JOIN group_chats gc ON t.group_id = gc.group_id
        `);
        results = rows;
      } catch (err) {
        console.error("Task list fallback error:", err);
      }
    }

    const page = req.query.page ? parseInt(req.query.page) : null;
    const limit = req.query.limit ? parseInt(req.query.limit) : 20;

    if (page && limit) {
      const offset = (page - 1) * limit;
      return res.json({
        data: results.slice(offset, offset + limit),
        pagination: { currentPage: page, pageSize: limit, total: results.length, totalPages: Math.ceil(results.length / limit) }
      });
    }
    res.json(results);
  });

  // GET /user/:userId
  router.get("/user/:userId", (req, res) => {
    const userId = req.params.userId;
    const results = dataCache.getUserTasks(userId);
    res.json(results);
  });

  // POST /tasks
  router.post("/", async (req, res) => {
    const { task_name, assigned_by, assigned_to, group_id, due_date, status, priority } = req.body;
    const query = `
      INSERT INTO tasks (task_name, assigned_by, assigned_to, group_id, due_date, status, priority) 
      VALUES ($1, $2, $3, $4, $5, $6, $7) 
      RETURNING task_id`;
    try {
      const result = await db.query(query, [
        task_name, 
        assigned_by, 
        assigned_to || null, 
        group_id || null, 
        due_date, 
        status || 'Pending', 
        priority || 'Medium'
      ]);
      await dataCache.refresh();
      res.json({ message: "Task created", task_id: result.rows[0].task_id });
    } catch (err) {
      console.error("Error creating task:", err);
      res.status(500).json({ error: "Error creating task" });
    }
  });

  // PUT /tasks/:id
  router.put("/:id", async (req, res) => {
    const { id } = req.params;
    const { task_name, assigned_by, assigned_to, group_id, due_date, status, priority } = req.body;
    const query = `
      UPDATE tasks 
      SET task_name = $1, assigned_by = $2, assigned_to = $3, group_id = $4, due_date = $5, status = $6, priority = $7 
      WHERE task_id = $8`;
    try {
      await db.query(query, [
        task_name, 
        assigned_by, 
        assigned_to || null, 
        group_id || null, 
        due_date, 
        status, 
        priority || 'Medium', 
        id
      ]);
      await dataCache.refresh();
      res.json({ message: "Task updated" });
    } catch (err) {
      console.error("Error updating task:", err);
      res.status(500).json({ error: "Error updating task" });
    }
  });

  // PATCH & PUT /tasks/:id/status - Update task status (Pending / In Progress / Completed)
  const updateStatusHandler = async (req, res) => {
    const { id } = req.params;
    const { status } = req.body;
    if (!status) {
      return res.status(400).json({ error: "Status is required" });
    }
    try {
      await db.query(`UPDATE tasks SET status = $1 WHERE task_id = $2`, [status, id]);
      await dataCache.refresh();
      res.json({ message: "Task status updated successfully", task_id: id, status });
    } catch (err) {
      console.error("Error updating task status:", err);
      res.status(500).json({ error: "Error updating task status" });
    }
  };

  router.patch("/:id/status", updateStatusHandler);
  router.put("/:id/status", updateStatusHandler);

  // DELETE /tasks/:id
  router.delete("/:id", async (req, res) => {
    try {
      await db.query(`DELETE FROM tasks WHERE task_id = $1`, [req.params.id]);
      await dataCache.refresh();
      res.json({ message: "Task deleted" });
    } catch (err) {
      console.error("Error deleting task:", err);
      res.status(500).json({ error: "Error deleting task" });
    }
  });

  return router;
};
