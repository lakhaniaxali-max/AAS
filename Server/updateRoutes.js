const express = require('express');
const router = express.Router();

module.exports = (db) => {
  /**
   * Update user information across Auth and Users tables
   */
  router.put('/update-user', async (req, res) => {
    const {
      auth_id,
      user_id,
      username,
      password,
      first_name,
      last_name,
      email,
      phone,
      profile_pic,
      role
    } = req.body;

    // Validate required fields
    if (!auth_id || !user_id) {
      return res.status(400).json({
        error: 'Missing required fields: auth_id and user_id'
      });
    }

    const client = await db.connect();
    try {
      await client.query("BEGIN");

      // 1. Update Auth table
      if (password && password.trim() !== '') {
        await client.query(
          'UPDATE auth SET username = $1, password = $2, role = $3 WHERE auth_id = $4',
          [username, password, role, auth_id]
        );
      } else {
        await client.query(
          'UPDATE auth SET username = $1, role = $2 WHERE auth_id = $3',
          [username, role, auth_id]
        );
      }

      // 2. Update Users table
      const userUpdateQuery = `
        UPDATE users 
        SET first_name = $1, last_name = $2, email = $3, phone = $4, profile_pic = $5
        WHERE user_id = $6
      `;
      const userUpdateParams = [first_name, last_name, email, phone, profile_pic, user_id];
      await client.query(userUpdateQuery, userUpdateParams);

      await client.query("COMMIT");

      res.status(200).json({
        success: true,
        message: 'User updated successfully'
      });

    } catch (error) {
      await client.query("ROLLBACK");
      console.error('Error during user update:', error.message);
      res.status(500).json({ error: 'Failed to update user' });
    } finally {
      client.release();
    }
  });

  router.put('/update-client', async (req, res) => {
    const {
      auth_id,
      client_id,
      username,
      password,
      company_name,
      contact_person,
      email,
      gstin,
      pan_number,
      profile_pic,
      contacts = []
    } = req.body;

    if (!auth_id || !client_id) {
      return res.status(400).json({ error: 'Missing required IDs' });
    }

    const client = await db.connect();
    try {
      await client.query("BEGIN");

      // 1. Update Auth table
      if (password && password.trim() !== '') {
        await client.query(
          'UPDATE auth SET username = $1, password = $2 WHERE auth_id = $3',
          [username, password, auth_id]
        );
      } else {
        await client.query(
          'UPDATE auth SET username = $1 WHERE auth_id = $2',
          [username, auth_id]
        );
      }

      // 2. Update Clients table
      const clientUpdateQuery = `
        UPDATE clients 
        SET company_name = $1, contact_person = $2, email = $3, gstin = $4, pan_number = $5, profile_pic = $6
        WHERE client_id = $7
      `;
      const clientUpdateParams = [company_name, contact_person, email, gstin || null, pan_number || null, profile_pic, client_id];
      await client.query(clientUpdateQuery, clientUpdateParams);

      // 3. Update ClientContacts
      await client.query('DELETE FROM client_contacts WHERE client_id = $1', [client_id]);

      if (contacts && contacts.length > 0) {
        for (const contact of contacts) {
          await client.query(
            'INSERT INTO client_contacts (client_id, contact_name, phone, email) VALUES ($1, $2, $3, $4)',
            [client_id, contact.contact_name, contact.phone, contact.email]
          );
        }
      }

      await client.query("COMMIT");
      res.status(200).json({ success: true, message: 'Client updated successfully' });

    } catch (error) {
      await client.query("ROLLBACK");
      console.error('Error during client update:', error.message);
      res.status(500).json({ error: 'Failed to update client' });
    } finally {
      client.release();
    }
  });

  return router;
};
