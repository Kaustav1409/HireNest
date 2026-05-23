const PUBLIC_AUTH_ENDPOINTS = new Set([
  "/api/auth/login",
  "/api/auth/register",
  "/api/auth/forgot-password",
  "/api/auth/reset-password",
  "/api/auth/google"
]);

function apiBase() {
  const base = typeof window.HIRENEST_API_BASE === "string" ? window.HIRENEST_API_BASE : "";
  return base.replace(/\/$/, "");
}

function resolveApiUrl(url) {
  const path = String(url || "");
  if (/^https?:\/\//i.test(path)) return path;
  const base = apiBase();
  if (!base) return path;
  return `${base}${path.startsWith("/") ? path : `/${path}`}`;
}

function normalizePath(url) {
  try {
    return new URL(url, window.location.origin).pathname;
  } catch (_) {
    return String(url || "");
  }
}

function isPublicAuthEndpoint(url) {
  return PUBLIC_AUTH_ENDPOINTS.has(normalizePath(url));
}

function isPlainNotFoundResponse(response, text) {
  if (!response || response.status !== 404) return false;
  const body = String(text || "").trim();
  return !body || /^not\s*found$/i.test(body) || body.includes("NOT_FOUND");
}

function formatAuthApiError(response, text, fallback) {
  if (isPlainNotFoundResponse(response, text)) {
    const base = apiBase();
    if (base) {
      return `Cannot reach API at ${base}. Render backend check karo.`;
    }
    return "API not found. Please verify backend is running.";
  }
  if (text && text.trim()) return text.trim();
  return fallback;
}

function clearHirenestAuth() {
  [
    "hirenest_token",
    "hirenest_userId",
    "hirenest_role",
    "hirenest_recruiterId",
    "hirenest_email",
    "hirenest_password",
    "hirenest_remember_email",
    "savedEmail",
    "savedPassword",
    "rememberEmail",
    "rememberPassword"
  ].forEach((key) => {
    try {
      localStorage.removeItem(key);
      sessionStorage.removeItem(key);
    } catch (_) {
      // ignore
    }
  });
}

function isUserNotFoundMessage(message) {
  return typeof message === "string" && /user not found/i.test(message.trim());
}

async function responseIndicatesMissingUser(response) {
  if (!response || response.status !== 404) return false;
  try {
    const text = await response.clone().text();
    if (!text || !text.trim()) return false;
    try {
      const body = JSON.parse(text);
      return isUserNotFoundMessage(body?.message || body?.detail || body?.error);
    } catch (_) {
      return isUserNotFoundMessage(text);
    }
  } catch (_) {
    return false;
  }
}

async function redirectToLoginIfSessionStale(response, options = {}) {
  const stale = await responseIndicatesMissingUser(response);
  if (!stale) return false;
  clearHirenestAuth();
  const reason = options.reason || "session_expired";
  location.href = `/login.html?reason=${encodeURIComponent(reason)}`;
  return true;
}

function authHeaders(includeJson = true, includeAuth = true) {
  const token = localStorage.getItem("hirenest_token");
  const base = includeJson ? { "Content-Type": "application/json" } : {};
  if (includeAuth && token) {
    base.Authorization = `Bearer ${token}`;
  }
  return base;
}

async function apiFetch(url, options = {}) {
  const resolvedUrl = resolveApiUrl(url);
  const isFormData = options.body instanceof FormData;
  const method = String(options.method || "GET").toUpperCase();
  const isPublicAuth = isPublicAuthEndpoint(url);
  // Do not send Content-Type: application/json on GET/HEAD (breaks some PDF/binary responses and proxies).
  const includeJsonContentType = !isFormData && method !== "GET" && method !== "HEAD";
  let response;
  try {
    response = await fetch(resolvedUrl, {
    ...options,
    headers: { ...authHeaders(includeJsonContentType, !isPublicAuth), ...(options.headers || {}) }
    });
  } catch (err) {
    if (isPublicAuth) {
      const base = apiBase();
      let msg = "Cannot connect to HireNest API.";
      if (base) {
        msg = `Cannot connect to ${base}. Check Render is running and CORS is enabled (redeploy backend).`;
      }
      const err2 = new Error(msg);
      err2.cause = err;
      throw err2;
    }
    throw err;
  }
  if (response.status === 401 && !isPublicAuth) {
    clearHirenestAuth();
    location.href = "/login.html?reason=session_expired";
    throw new Error("Unauthorized");
  }
  if (!isPublicAuth && (await redirectToLoginIfSessionStale(response))) {
    throw new Error("Session expired");
  }
  return response;
}

/**
 * GET with JWT and trigger a file download (e.g. PDF resume). Anchor navigation cannot send Authorization.
 * Uses Content-Disposition filename when present; otherwise fallbackFilename.
 */
async function downloadResumeWithAuth(url, fallbackFilename = "resume.pdf") {
  const res = await apiFetch(url, {
    method: "GET",
    headers: { Accept: "application/pdf,application/octet-stream;q=0.9,*/*;q=0.8" }
  });
  if (!res.ok) {
    const err = new Error("Download failed");
    err.status = res.status;
    throw err;
  }
  let filename = fallbackFilename;
  const disposition = res.headers.get("Content-Disposition");
  if (disposition) {
    const m = /filename\*=UTF-8''([^;\n]+)|filename="([^"]+)"|filename=([^;\s]+)/i.exec(disposition);
    if (m) {
      const raw = (m[1] || m[2] || m[3] || "").trim();
      try {
        filename = decodeURIComponent(raw.replace(/^["']|["']$/g, ""));
      } catch (_) {
        filename = raw.replace(/^["']|["']$/g, "") || fallbackFilename;
      }
    }
  }
  const blob = await res.blob();
  const u = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = u;
  a.download = filename;
  a.rel = "noopener";
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(u), 60_000);
}
