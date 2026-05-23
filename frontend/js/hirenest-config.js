/**
 * HireNest API Base URL Configuration.
 *
 * Static frontend — backend URL is hardcoded here.
 * Local dev on port 9090 uses same-origin; production uses Render.
 */
(function initHirenestApiBase() {
  if (window.location.port === "9090") {
    window.HIRENEST_API_BASE = "";
    return;
  }
  window.HIRENEST_API_BASE = "https://hirenest-backend-8c7o.onrender.com";
})();
