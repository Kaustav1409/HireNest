/**
 * Vercel: same-origin /api (serverless proxy). Local JAR: port 9090.
 * Live Server: optional hirenest-env.js with Render URL.
 */
(function initHirenestApiBase() {
  function normalize(url) {
    return String(url || "").trim().replace(/\/$/, "");
  }

  if (typeof window.HIRENEST_API_BASE === "string" && window.HIRENEST_API_BASE.trim()) {
    window.HIRENEST_API_BASE = normalize(window.HIRENEST_API_BASE);
    return;
  }

  const host = window.location.hostname || "";
  const isVercel = host.endsWith(".vercel.app") || host.includes("vercel.app");
  if (isVercel) {
    window.HIRENEST_API_BASE = "";
    return;
  }

  if (window.location.port === "9090") {
    window.HIRENEST_API_BASE = "";
    return;
  }

  const fromEnv = normalize(window.HIRENEST_BACKEND_URL);
  if (fromEnv && !/YOUR-BACKEND/i.test(fromEnv)) {
    window.HIRENEST_API_BASE = fromEnv;
    return;
  }

  window.HIRENEST_API_BASE = "";
})();
