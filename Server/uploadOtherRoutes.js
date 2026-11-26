const express = require("express");
const router = express.Router();

module.exports = (db) => {

  // ➕ Create a new other document
  router.post("/other/create", async (req, res) => {
    const { client_id, folder_id, doc_name, fileUrl } = req.body;
  
    if (!client_id || !doc_name) {
      return res.status(400).json({ success: false, message: "Missing required fields: client_id and doc_name are mandatory" });
    }
  
    const insertSql = `
      INSERT INTO other_documents (client_id, folder_id, doc_name, doc_data)
      VALUES ($1, $2, $3, $4)
      RETURNING doc_id
    `;
    const fileData = fileUrl && fileUrl.trim() !== "" ? fileUrl : "";
  
    try {
      const result = await db.query(insertSql, [client_id, folder_id || null, doc_name, fileData]);
      res.status(201).json({
        success: true,
        message: "Other document inserted successfully",
        insertedId: result.rows[0].doc_id,
      });
    } catch (err) {
      console.error("Insert error:", err);
      res.status(500).json({ success: false, message: "Failed to insert document" });
    }
  });
  

  // 🔁 Update an existing document
  router.post("/other/update", async (req, res) => {
    const { doc_id, fileUrl } = req.body;

    if (!doc_id || !fileUrl) {
      return res.status(400).json({ success: false, message: "Missing required fields" });
    }

    try {
      const { rows } = await db.query("SELECT * FROM other_documents WHERE doc_id = $1", [doc_id]);
      if (rows.length === 0) {
        return res.status(404).json({ success: false, message: "Document not found" });
      }

      await db.query("UPDATE other_documents SET doc_data = $1 WHERE doc_id = $2", [fileUrl, doc_id]);
      res.json({ success: true, message: "Other document updated successfully" });
    } catch (err) {
      console.error("Update error:", err);
      res.status(500).json({ success: false, message: "Database error" });
    }
  });

  // 🔍 Get document by doc_id
  router.get("/other/get/:doc_id", async (req, res) => {
    try {
      const { rows } = await db.query("SELECT * FROM other_documents WHERE doc_id = $1", [req.params.doc_id]);
      if (rows.length === 0) {
        return res.status(404).json({ success: false, message: "Document not found" });
      }
      res.json({ success: true, data: rows[0] });
    } catch (err) {
      res.status(500).json({ success: false, message: "Failed to fetch document" });
    }
  });

  // 🔍 Get all documents for a client
  router.get("/other/getByClient/:client_id", async (req, res) => {
    try {
      const { rows } = await db.query("SELECT * FROM other_documents WHERE client_id = $1", [req.params.client_id]);
      res.json({ success: true, documents: rows });
    } catch (err) {
      res.status(500).json({ success: false, message: "Failed to fetch documents" });
    }
  });

  return router;
};
