const { Pool } = require("pg");
const fs = require("fs");
const path = require("path");
require("dotenv").config({ path: path.join(__dirname, ".env") });
require("dotenv").config(); // fallback

// SSL Configuration for Aiven / Neon / Supabase / Production
const caPath = "/etc/secrets/ca.pem";
let sslConfig = false;

if (process.env.DB_SSL === "true" || process.env.DB_SSL === true) {
    sslConfig = {
        rejectUnauthorized: false
    };

    try {
        if (fs.existsSync(caPath)) {
            sslConfig.ca = fs.readFileSync(caPath);
            sslConfig.rejectUnauthorized = true;
            console.log("✅ PostgreSQL CA certificate loaded successfully.");
        }
    } catch (error) {
        console.error("❌ Error reading CA certificate:", error);
    }
}

const pool = new Pool({
    host: process.env.DB_HOST || "localhost",
    port: parseInt(process.env.DB_PORT || "5432", 10),
    user: process.env.DB_USER || "postgres",
    password: process.env.DB_PASSWORD || "",
    database: process.env.DB_NAME || "accurate_accounts",
    ssl: sslConfig,
    max: 15,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 5000
});

// Utility to verify connection on startup
(async () => {
    try {
        const client = await pool.connect();
        console.log("✅ PostgreSQL pool initialized and connection verified.");
        client.release();
    } catch (err) {
        console.error("❌ PostgreSQL connection failed during pool initialization:", err.message);
    }
})();

module.exports = pool;
