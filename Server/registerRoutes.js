const express = require("express");
const bcrypt = require("bcrypt");

module.exports = (db) => {
  const router = express.Router();

  router.post("/register", async (req, res) => {
    const {
      username,
      password,
      role,
      first_name,
      last_name,
      email,
      phone,
      profile_pic,
      company_name,
      contact_person,
      gstin,
      pan_number,
      contacts
    } = req.body;

    // Standardize role and fields
    const normalizedRole = (role || 'employee').toLowerCase();
    const effectiveFirstName = first_name || contact_person || username;
    const effectiveLastName = last_name || '';
    const effectiveCompanyName = company_name || (effectiveFirstName + " Enterprise");
    const effectiveContactPerson = contact_person || effectiveFirstName;

    // Validate essential required fields
    if (!username || !password) {
      return res.status(400).json({
        error: "Username and password are required"
      });
    }

    const client = await db.connect();
    try {
      // Check if username already exists
      const existingUser = await client.query("SELECT username FROM auth WHERE username = $1", [username]);

      if (existingUser.rows.length > 0) {
        client.release();
        return res.status(400).json({ error: "Username already exists" });
      }

      await client.query("BEGIN");

      // Insert into auth table
      const authResult = await client.query(
        "INSERT INTO auth (username, password, role) VALUES ($1, $2, $3) RETURNING auth_id",
        [username, password, normalizedRole]
      );

      const auth_id = authResult.rows[0].auth_id;

      if (normalizedRole === "admin" || normalizedRole === "employee") {
        // Insert into users table
        await client.query(
          `INSERT INTO users 
            (auth_id, first_name, last_name, email, phone, profile_pic)
            VALUES ($1, $2, $3, $4, $5, $6)`,
          [auth_id, effectiveFirstName, effectiveLastName, email || null, phone || null, profile_pic || null]
        );
      } else if (normalizedRole === "client") {
        // Insert into clients table
        const clientResult = await client.query(
          `INSERT INTO clients 
            (auth_id, company_name, contact_person, email, gstin, pan_number, profile_pic)
            VALUES ($1, $2, $3, $4, $5, $6, $7) 
            RETURNING client_id`,
          [
            auth_id,
            effectiveCompanyName,
            effectiveContactPerson,
            email || null,
            gstin || null,
            pan_number || null,
            profile_pic || null
          ]
        );
        const client_id = clientResult.rows[0].client_id;

        // Insert default important documents (folder_id is null for general client documents)
        await client.query(
          `INSERT INTO important_documents (doc_type, client_id, folder_id, doc_data) 
           VALUES ($1, $2, $3, $4)`,
          ['Aadhar Card', client_id, null, '']
        );
        await client.query(
          `INSERT INTO important_documents (doc_type, client_id, folder_id, doc_data) 
           VALUES ($1, $2, $3, $4)`,
          ['PAN Card', client_id, null, '']
        );

        // Insert client contacts if they exist, or use phone
        if (contacts && Array.isArray(contacts) && contacts.length > 0) {
          for (const contact of contacts) {
            await client.query(
              `INSERT INTO client_contacts (client_id, contact_name, phone, email)
               VALUES ($1, $2, $3, $4)`,
              [client_id, contact.contact_name, contact.phone, contact.email]
            );
          }
        } else if (phone) {
          await client.query(
            `INSERT INTO client_contacts (client_id, contact_name, phone, email)
             VALUES ($1, $2, $3, $4)`,
            [client_id, effectiveContactPerson, phone, email || null]
          );
        }
      }

      await client.query("COMMIT");
      try {
        const dataCache = require("./dataCache");
        if (dataCache && dataCache.loadAll) dataCache.loadAll();
      } catch (cacheErr) {
        console.error("Cache reload notice:", cacheErr.message);
      }
      res.status(201).json({ message: "Registration successful" });

    } catch (err) {
      await client.query("ROLLBACK");
      console.error("Registration error:", err);
      res.status(500).json({
        error: "Internal server error",
        details: err.message
      });
    } finally {
      client.release();
    }
  });

  return router;
};
