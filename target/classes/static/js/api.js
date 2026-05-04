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
    localStorage.removeItem("hirenest_token");
    location.href = "/login.html";
    throw new Error("Unauthorized");
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
