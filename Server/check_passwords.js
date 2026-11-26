const db = require('./db');

async function main() {
    try {
        const res = await db.query("SELECT auth_id, username, password, role FROM auth WHERE username IN ('admin', 'axali123', 'kashish', 'employee', 'client')");
        for (const r of res.rows) {
            console.log(r.username, 'role:', r.role, 'password:', r.password);
        }
    } catch (e) {
        console.error(e);
    } finally {
        process.exit(0);
    }
}
main();
