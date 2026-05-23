/**
 * Vercel build: injects Render URL into rewrites from HIRENEST_BACKEND_URL.
 */
const fs = require("fs");
const path = require("path");

const root = path.join(__dirname, "..");
const vercelPath = path.join(root, "vercel.json");

const backend = String(process.env.HIRENEST_BACKEND_URL || "").trim().replace(/\/$/, "");
if (!backend) {
  console.error(
    "\n[hirenest] Vercel → Settings → Environment Variables → add:\n" +
      "  HIRENEST_BACKEND_URL = https://your-app.onrender.com\n"
  );
  process.exit(1);
}

const config = {
  buildCommand: "node scripts/generate-vercel-json.js",
  outputDirectory: "src/main/resources/static",
  cleanUrls: true,
  rewrites: [
    { source: "/api/:path*", destination: `${backend}/api/:path*` },
    { source: "/uploads/:path*", destination: `${backend}/uploads/:path*` }
  ]
};

fs.writeFileSync(vercelPath, JSON.stringify(config, null, 2) + "\n");
console.log("[hirenest] vercel.json → API proxy:", backend);
