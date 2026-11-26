const http = require('http');

const data = JSON.stringify({
    auth_id: 1,
    title: "Q4 Tax & Audit Deadline",
    message: "All staff please ensure all Q4 financial audit files and balance sheets are uploaded.",
    created_by: "System Admin (@admin)",
    announcement_date: "2026-11-25"
});

const req = http.request({
    hostname: 'localhost',
    port: 3000,
    path: '/announcements',
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(data)
    }
}, (res) => {
    let body = '';
    res.on('data', chunk => body += chunk);
    res.on('end', () => {
        console.log("POST Status:", res.statusCode);
        console.log("POST Response:", body);
        
        // Also fetch GET
        http.get('http://localhost:3000/announcements', (getRes) => {
            let getBody = '';
            getRes.on('data', chunk => getBody += chunk);
            getRes.on('end', () => {
                console.log("\nGET Announcements (First 2):");
                const items = JSON.parse(getBody);
                console.log(items.slice(0, 2));
                process.exit(0);
            });
        });
    });
});

req.on('error', (e) => {
    console.error("Request Error:", e);
    process.exit(1);
});

req.write(data);
req.end();
