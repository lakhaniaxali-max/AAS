// userRoutes.js
const express = require("express");
const dataCache = require("./dataCache");

module.exports = (db) => {
  const router = express.Router();

  // Fetch all users with their roles (Direct DB with Cache fallback)
  router.get("/", async (req, res) => {
    try {
      const query = `
        SELECT 
          u.user_id, u.auth_id, a.username, a.role, 
          u.first_name, u.last_name, u.email, u.phone, 
          u.profile_pic, u.created_at, u.updated_at 
        FROM users u 
        INNER JOIN auth a ON u.auth_id = a.auth_id
        ORDER BY u.user_id DESC`;
      const { rows: results } = await db.query(query);
      res.json(results);
    } catch (err) {
      console.error("Database error fetching users, falling back to cache:", err);
      res.json(dataCache.getUsers());
    }
  });

  // Fetch a specific user by ID (Resilient to user_id or auth_id)
  router.get("/:id", async (req, res) => {
    const { id } = req.params;
    
    // 1. Try Cache by user_id
    let user = dataCache.getUser(id);
    if (user) return res.json(user);

    // 2. Try Cache by auth_id (Backwards compatibility)
    user = dataCache.getUserByAuthId(id);
    if (user) return res.json(user);

    // 3. Fallback to DB - Check USERS by user_id OR auth_id
    try {
      const query = `
        SELECT 
          u.user_id, u.auth_id, a.username, a.role, 
          u.first_name, u.last_name, u.email, u.phone, 
          u.profile_pic, u.created_at, u.updated_at 
        FROM users u 
        INNER JOIN auth a ON u.auth_id = a.auth_id
        WHERE u.user_id = $1 OR u.auth_id = $2`;
      
      const { rows: results } = await db.query(query, [id, id]);
      if (results.length > 0) return res.json(results[0]);

      return res.status(404).json({ error: "User not found" });
    } catch (err) {
      console.error("Database error fetching user:", err);
      res.status(500).json({ error: "Internal server error" });
    }
  });

  /**
   * Fetch profile by auth_id (Remains for specific lookups)
   */
  router.get("/auth/:auth_id", async (req, res) => {
    const { auth_id } = req.params;
    
    try {
      // 1. Try Admin/Employee Cache First
      let profile = dataCache.getUserByAuthId(auth_id);
      if (profile) return res.json(profile);

      // 2. Try Client Cache
      profile = dataCache.getClientByAuthId(auth_id);
      if (profile) return res.json(profile);

      // 3. Fallback to DB - Check USERS first
      const userQuery = `
        SELECT 
          u.user_id, u.auth_id, a.username, a.role, 
          u.first_name, u.last_name, u.email, u.phone, 
          u.profile_pic, u.created_at, u.updated_at 
        FROM users u 
        INNER JOIN auth a ON u.auth_id = a.auth_id
        WHERE u.auth_id = $1`;
      
      const { rows: userResults } = await db.query(userQuery, [auth_id]);
      if (userResults.length > 0) return res.json(userResults[0]);

      // 4. Fallback to DB - Check CLIENTS
      const clientQuery = `
        SELECT 
          c.client_id, c.auth_id, a.username, a.role,
          c.company_name, c.contact_person, c.email,
          c.gstin, c.pan_number, c.profile_pic,
          COALESCE((SELECT phone FROM client_contacts WHERE client_id = c.client_id ORDER BY contact_id LIMIT 1), '') AS phone,
          c.created_at, c.updated_at
        FROM clients c
        INNER JOIN auth a ON c.auth_id = a.auth_id
        WHERE c.auth_id = $1`;

      const { rows: clientResults } = await db.query(clientQuery, [auth_id]);
      if (clientResults.length > 0) return res.json(clientResults[0]);

      // Not found anywhere
      return res.status(404).json({ error: "User Profile Not Found" });

    } catch (err) {
      console.error("Database error during unified profile lookup:", err);
      return res.status(500).json({ error: "Internal server error" });
    }
  });

  return router;
};
