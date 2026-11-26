const db = require('./Server/db');

async function migrate() {
    try {
        console.log("Checking announcements table columns...");
        await db.query("ALTER TABLE announcements ADD COLUMN IF NOT EXISTS created_by VARCHAR(150)");
        await db.query("ALTER TABLE announcements ADD COLUMN IF NOT EXISTS announcement_date VARCHAR(50)");
        console.log("✅ Successfully updated announcements table with created_by and announcement_date.");

        const res = await db.query("SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'announcements'");
        console.log("Current columns:", res.rows.map(r => r.column_name));
    } catch (e) {
        console.error("Migration error:", e);
    } finally {
        process.exit(0);
    }
}
migrate();
