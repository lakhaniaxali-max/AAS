const express = require("express");
const dataCache = require("./dataCache");
const router = express.Router();

module.exports = (db) => {
  // Legacy route kept for compatibility
  router.post("/uploadimportant", async (req, res) => {
    const { doc_id, fileUrl } = req.body;

    if (!doc_id || !fileUrl) {
      return res.status(400).json({ success: false, message: "Missing required fields" });
    }

    try {
      const { rows } = await db.query("SELECT * FROM important_documents WHERE doc_id = $1", [doc_id]);
      if (rows.length === 0) {
        return res.status(404).json({ success: false, message: "Document not found" });
      }

      await db.query("UPDATE important_documents SET doc_data = $1 WHERE doc_id = $2", [fileUrl, doc_id]);
      dataCache.refresh().catch(() => {});
      res.json({ success: true, message: "Updated successfully" });
    } catch (err) {
      console.error("Database error:", err);
      res.status(500).json({ success: false, message: "Database error" });
    }
  });

  // Live GET all documents for a specific client (Financial Sheets + KYC Docs)
  router.get("/documents/client/:clientId", async (req, res) => {
    const clientId = parseInt(req.params.clientId, 10);
    if (!clientId) {
      return res.status(400).json({ error: "Invalid client ID" });
    }

    try {
      const impRes = await db.query(
        `SELECT doc_id, doc_type, client_id, folder_id, created_at,
                (doc_data IS NOT NULL AND LENGTH(TRIM(doc_data)) > 0) AS has_file,
                CASE
                  WHEN doc_data LIKE 'data:image/png%' THEN 'image/png'
                  WHEN doc_data LIKE 'data:image/jpeg%' OR doc_data LIKE 'data:image/jpg%' THEN 'image/jpeg'
                  WHEN doc_data LIKE 'data:application/pdf%' THEN 'application/pdf'
                  ELSE 'text/plain'
                END AS mime_type
         FROM important_documents
         WHERE client_id = $1
         ORDER BY created_at DESC`,
        [clientId]
      );

      const otherRes = await db.query(
        `SELECT doc_id, doc_name, client_id, folder_id, created_at,
                (doc_data IS NOT NULL AND LENGTH(TRIM(doc_data)) > 0) AS has_file,
                CASE
                  WHEN doc_data LIKE 'data:image/png%' THEN 'image/png'
                  WHEN doc_data LIKE 'data:image/jpeg%' OR doc_data LIKE 'data:image/jpg%' THEN 'image/jpeg'
                  WHEN doc_data LIKE 'data:application/pdf%' THEN 'application/pdf'
                  ELSE 'text/plain'
                END AS mime_type
         FROM other_documents
         WHERE client_id = $1
         ORDER BY created_at DESC`,
        [clientId]
      );

      res.status(200).json({
        client_id: clientId,
        important_documents: impRes.rows,
        other_documents: otherRes.rows
      });
    } catch (err) {
      console.error("Error fetching client documents:", err);
      res.status(500).json({ error: "Failed to fetch client documents" });
    }
  });

  // Client KYC Upload (Strictly PAN Card & Aadhar Card in PNG or JPEG format)
  router.post("/documents/kyc/upload", async (req, res) => {
    const { client_id, doc_type, file_data, mime_type } = req.body;

    if (!client_id || !doc_type || !file_data) {
      return res.status(400).json({ success: false, message: "client_id, doc_type, and file_data are required" });
    }

    // Only PAN Card and Aadhar Card allowed for client upload
    const allowedTypes = ["PAN Card", "Aadhar Card"];
    if (!allowedTypes.includes(doc_type)) {
      return res.status(403).json({ success: false, message: "Clients can only upload PAN Card or Aadhar Card" });
    }

    // Only PNG or JPEG allowed
    const allowedMimes = ["image/png", "image/jpeg", "image/jpg"];
    const normalizedMime = (mime_type || "").toLowerCase();
    if (!allowedMimes.includes(normalizedMime) && !file_data.startsWith("data:image/png") && !file_data.startsWith("data:image/jpeg") && !file_data.startsWith("data:image/jpg")) {
      return res.status(400).json({ success: false, message: "Only PNG or JPEG images are allowed" });
    }

    const storedData = file_data.startsWith("data:")
      ? file_data
      : `data:${normalizedMime || "image/jpeg"};base64,${file_data}`;

    try {
      const existing = await db.query(
        "SELECT doc_id FROM important_documents WHERE client_id = $1 AND doc_type = $2",
        [client_id, doc_type]
      );

      let docId;
      if (existing.rows.length > 0) {
        docId = existing.rows[0].doc_id;
        await db.query(
          "UPDATE important_documents SET doc_data = $1, created_at = CURRENT_TIMESTAMP WHERE doc_id = $2",
          [storedData, docId]
        );
      } else {
        const inserted = await db.query(
          "INSERT INTO important_documents (doc_type, client_id, doc_data) VALUES ($1, $2, $3) RETURNING doc_id",
          [doc_type, client_id, storedData]
        );
        docId = inserted.rows[0].doc_id;
      }

      dataCache.refresh().catch(() => {});
      res.status(200).json({
        success: true,
        message: `${doc_type} uploaded successfully`,
        doc_id: docId,
        doc_type
      });
    } catch (err) {
      console.error("Error uploading KYC document:", err);
      res.status(500).json({ success: false, message: "Failed to upload document" });
    }
  });

  // Admin / Employee Upload Financial Sheet or Other Document (PDF / Images)
  router.post("/documents/sheet/upload", async (req, res) => {
    const { client_id, doc_type, doc_name, file_data, mime_type } = req.body;

    if (!client_id || !file_data || (!doc_type && !doc_name)) {
      return res.status(400).json({ success: false, message: "client_id, sheet type/name, and file_data are required" });
    }

    const normalizedMime = (mime_type || "").toLowerCase();
    const importantSheetTypes = ["Balance Sheet", "Profit and Loss", "Capital Account"];
    const isImportant = doc_type && importantSheetTypes.includes(doc_type);

    if (isImportant) {
      if (normalizedMime !== "application/pdf" && !file_data.startsWith("data:application/pdf")) {
        return res.status(400).json({ success: false, message: "Only PDF files are allowed for financial sheets" });
      }
    } else {
      const allowed = ["application/pdf", "image/png", "image/jpeg", "image/jpg"];
      const isAllowed = allowed.includes(normalizedMime) || file_data.startsWith("data:application/pdf") || file_data.startsWith("data:image/");
      if (!isAllowed) {
        return res.status(400).json({ success: false, message: "Only PDF or image files (PNG/JPEG) are allowed" });
      }
    }

    let storedData = file_data;
    if (!file_data.startsWith("data:")) {
      const defaultMime = normalizedMime || "application/pdf";
      storedData = `data:${defaultMime};base64,${file_data}`;
    }

    try {
      if (doc_type && importantSheetTypes.includes(doc_type)) {
        const existing = await db.query(
          "SELECT doc_id FROM important_documents WHERE client_id = $1 AND doc_type = $2",
          [client_id, doc_type]
        );
        let docId;
        if (existing.rows.length > 0) {
          docId = existing.rows[0].doc_id;
          await db.query(
            "UPDATE important_documents SET doc_data = $1, created_at = CURRENT_TIMESTAMP WHERE doc_id = $2",
            [storedData, docId]
          );
        } else {
          const inserted = await db.query(
            "INSERT INTO important_documents (doc_type, client_id, doc_data) VALUES ($1, $2, $3) RETURNING doc_id",
            [doc_type, client_id, storedData]
          );
          docId = inserted.rows[0].doc_id;
        }
        dataCache.refresh().catch(() => {});
        return res.status(200).json({
          success: true,
          message: `${doc_type} PDF uploaded successfully`,
          doc_id: docId
        });
      } else {
        const name = (doc_name || doc_type || "Financial Sheet").trim();
        const inserted = await db.query(
          "INSERT INTO other_documents (doc_name, client_id, doc_data) VALUES ($1, $2, $3) RETURNING doc_id",
          [name, client_id, storedData]
        );
        dataCache.refresh().catch(() => {});
        return res.status(201).json({
          success: true,
          message: `${name} PDF uploaded successfully`,
          doc_id: inserted.rows[0].doc_id
        });
      }
    } catch (err) {
      console.error("Error uploading PDF sheet:", err);
      res.status(500).json({ success: false, message: "Failed to upload PDF sheet" });
    }
  });

  // View / Download a document directly from PostgreSQL
  router.get("/documents/download/:source/:docId", async (req, res) => {
    const { source, docId } = req.params;
    const id = parseInt(docId, 10);
    if (!id) {
      return res.status(400).json({ error: "Invalid document ID" });
    }

    try {
      let row = null;
      let title = "document";

      if (source === "important") {
        const { rows } = await db.query(
          "SELECT doc_id, doc_type, doc_data, created_at FROM important_documents WHERE doc_id = $1",
          [id]
        );
        if (rows.length > 0) {
          row = rows[0];
          title = row.doc_type;
        }
      } else {
        const { rows } = await db.query(
          "SELECT doc_id, doc_name, doc_data, created_at FROM other_documents WHERE doc_id = $1",
          [id]
        );
        if (rows.length > 0) {
          row = rows[0];
          title = row.doc_name;
        }
      }

      if (!row || !row.doc_data) {
        return res.status(404).json({ error: "Document not found" });
      }

      // Return JSON payload with title, mime_type, and data so Android can both View and Download cleanly
      let mimeType = "text/plain";
      let isBase64 = false;
      let content = row.doc_data;

      const dataUriMatch = row.doc_data.match(/^data:([^;]+);base64,(.+)$/s);
      if (dataUriMatch) {
        mimeType = dataUriMatch[1];
        content = dataUriMatch[2];
        isBase64 = true;
      }

      res.status(200).json({
        doc_id: row.doc_id,
        title,
        mime_type: mimeType,
        is_base64: isBase64,
        content,
        created_at: row.created_at
      });
    } catch (err) {
      console.error("Error downloading document:", err);
      res.status(500).json({ error: "Failed to fetch document content" });
    }
  });

  // Delete an important document (removes record so it returns to not uploaded)
  router.delete("/documents/important/:docId", async (req, res) => {
    const id = parseInt(req.params.docId, 10);
    if (!id) return res.status(400).json({ error: "Invalid doc ID" });
    try {
      await db.query("DELETE FROM important_documents WHERE doc_id = $1", [id]);
      dataCache.refresh().catch(() => {});
      res.json({ success: true, message: "Important document deleted successfully" });
    } catch (err) {
      console.error("Error deleting important document:", err);
      res.status(500).json({ error: "Failed to delete important document" });
    }
  });

  // Delete an other document
  router.delete("/documents/other/:docId", async (req, res) => {
    const id = parseInt(req.params.docId, 10);
    if (!id) return res.status(400).json({ error: "Invalid doc ID" });
    try {
      await db.query("DELETE FROM other_documents WHERE doc_id = $1", [id]);
      dataCache.refresh().catch(() => {});
      res.json({ success: true, message: "Document deleted successfully" });
    } catch (err) {
      console.error("Error deleting document:", err);
      res.status(500).json({ error: "Failed to delete document" });
    }
  });

  // Unified delete endpoint (source = 'important' or 'other')
  router.delete("/documents/:source/:docId", async (req, res) => {
    const { source, docId } = req.params;
    const id = parseInt(docId, 10);
    if (!id) return res.status(400).json({ error: "Invalid doc ID" });

    try {
      if (source === "important") {
        await db.query("DELETE FROM important_documents WHERE doc_id = $1", [id]);
      } else {
        await db.query("DELETE FROM other_documents WHERE doc_id = $1", [id]);
      }
      dataCache.refresh().catch(() => {});
      res.json({ success: true, message: "Document deleted successfully" });
    } catch (err) {
      console.error("Error deleting document:", err);
      res.status(500).json({ error: "Failed to delete document" });
    }
  });

  return router;
};
