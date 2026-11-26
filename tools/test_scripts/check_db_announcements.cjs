const { Pool } = require('pg');

const pool = new Pool({
  host: process.env.PG_HOST || 'localhost',
  port: parseInt(process.env.PG_PORT || '5432', 10),
  database: process.env.PG_DATABASE || 'accurate_accounts',
  user: process.env.PG_USER || 'postgres',
  password: process.env.PG_PASSWORD || 'password',
});

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
    await pool.end();
  }
}

checkAnnouncements();
