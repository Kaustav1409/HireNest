const PUBLIC_AUTH_ENDPOINTS = new Set([
  "/api/auth/login",
  "/api/auth/register",
  "/api/auth/forgot-password",
  "/api/auth/reset-password",
  "/api/auth/google"
]);

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
  const isFormData = options.body instanceof FormData;
  const method = String(options.method || "GET").toUpperCase();
  const isPublicAuth = isPublicAuthEndpoint(url);
  // Do not send Content-Type: application/json on GET/HEAD (breaks some PDF/binary responses and proxies).
  const includeJsonContentType = !isFormData && method !== "GET" && method !== "HEAD";
  const response = await fetch(url, {
    ...options,
    headers: { ...authHeaders(includeJsonContentType, !isPublicAuth), ...(options.headers || {}) }
  });
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
