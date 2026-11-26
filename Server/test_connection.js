const db = require("./db");

async function testConn() {
    try {
        const res = await db.query("SELECT tablename FROM pg_tables WHERE schemaname = 'public'");
        console.log("✅ CONNECTION SUCCESS!");
        console.log(`Found ${res.rows.length} tables in PostgreSQL:`);
        console.log(res.rows.map(r => r.tablename).join(", "));
        
        const adminRes = await db.query("SELECT username, role FROM auth LIMIT 1");
        if (adminRes.rows.length > 0) {
            console.log("✅ Auth verified: Admin user exists (" + adminRes.rows[0].username + ", " + adminRes.rows[0].role + ")");
        }
        
        process.exit(0);
    } catch (err) {
        console.error("❌ CONNECTION FAILED:", err.message);
        process.exit(1);
    }
}

testConn();

