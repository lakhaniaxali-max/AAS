const pool = require('./db.js');

async function checkAnnouncements() {
  try {
    const res = await pool.query(`
      SELECT announcement_id, title, message, created_by, announcement_date, created_at
      FROM announcements
      ORDER BY announcement_id DESC
      LIMIT 5;
    `);
    console.log('=== LATEST ANNOUNCEMENTS IN DATABASE ===');
    console.log(JSON.stringify(res.rows, null, 2));
  } catch (err) {
    console.error('DB query error:', err.message);
  } finally {
    process.exit(0);
  }
}

checkAnnouncements();
