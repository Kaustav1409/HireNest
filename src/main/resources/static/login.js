function clearCredentialCache() {
  [
    "hirenest_email",
    "hirenest_password",
    "savedEmail",
    "savedPassword",
    "rememberPassword"
  ].forEach((k) => {
    try {
      localStorage.removeItem(k);
      sessionStorage.removeItem(k);
    } catch (_) {
      // ignore
    }
  });
}

// Defensive: clear any previously stored email/password keys.
clearCredentialCache();

const loginForm = document.getElementById("loginForm");
const msg = document.getElementById("loginMsg");
const emailEl = document.getElementById("email");
const passwordEl = document.getElementById("password");
const rememberEl = document.getElementById("rememberMe");
const toggleLoginPassword = document.getElementById("toggleLoginPassword");
const googleLoginButton = document.getElementById("googleLoginButton");
const googleClientId = String(document.body?.dataset?.googleClientId || "").trim();

function showMsg(text, isError = true) {
  msg.textContent = text;
  msg.classList.remove("hidden", "success", "error");
  msg.classList.add(isError ? "error" : "success");
}

async function readErrorMessage(response, fallbackMessage) {
  const clone = response.clone();
  try {
    const payload = await response.json();
    if (payload && typeof payload.message === "string" && payload.message.trim()) {
      return payload.message.trim();
    }
  } catch (_) {
    // Keep fallback message when backend response is not JSON.
  }
  try {
    const text = (await clone.text()).trim();
    if (text) return text;
  } catch (_) {
    // ignore text parse errors
  }
  return fallbackMessage;
}

function saveAuthAndRedirect(user) {
  localStorage.setItem("hirenest_token", user.token || "");
  localStorage.setItem("hirenest_userId", String(user.userId));
  localStorage.setItem("hirenest_role", user.role);
  if (user.recruiterId) {
    localStorage.setItem("hirenest_recruiterId", String(user.recruiterId));
  } else {
    localStorage.removeItem("hirenest_recruiterId");
  }
  if (user.role === "RECRUITER") {
    location.href = "/dashboard/recruiter.html";
  } else {
    location.href = "/dashboard/job-seeker.html";
  }
}

async function handleGoogleCredential(credential) {
  if (!credential) {
    showMsg("Google sign-in credential missing.");
    return;
  }
  let response;
  try {
    response = await apiFetch("/api/auth/google", {
      method: "POST",
      body: JSON.stringify({ idToken: credential, credential })
    });
  } catch (err) {
    showMsg(err?.message || "Google sign-in request failed.");
    return;
  }
  if (!response.ok) {
    showMsg(await readErrorMessage(response, "Google sign-in failed. Please try again."));
    return;
  }
  const user = await response.json();
  saveAuthAndRedirect(user);
}

function initGoogleLogin(attempt = 0) {
  if (!googleLoginButton) {
    return;
  }
  if (!googleClientId || googleClientId === "YOUR_GOOGLE_CLIENT_ID") {
    showMsg("Google Sign-In is not configured. Add your Google Client ID.");
    return;
  }
  if (!window.google?.accounts?.id) {
    if (attempt < 10) {
      setTimeout(() => initGoogleLogin(attempt + 1), 250);
    }
    return;
  }
  window.google.accounts.id.initialize({
    client_id: googleClientId,
    callback: (response) => handleGoogleCredential(response.credential)
  });
  window.google.accounts.id.renderButton(googleLoginButton, {
    theme: "outline",
    size: "large",
    text: "continue_with",
    shape: "pill",
    width: 320
  });
}

const loginParams = new URLSearchParams(window.location.search);
if (loginParams.get("error") === "tab_switch_detected") {
  showMsg("You were suspected of switching tabs during assessment. Please login again.");
  try {
    window.history.replaceState({}, document.title, "/login.html");
  } catch (_) {
    // ignore
  }
}

const rememberedEmail = localStorage.getItem("hirenest_remember_email");
if (rememberedEmail) {
  emailEl.value = rememberedEmail;
  rememberEl.checked = true;
}

toggleLoginPassword?.addEventListener("click", () => {
  const next = passwordEl.type === "password" ? "text" : "password";
  passwordEl.type = next;
  toggleLoginPassword.textContent = next === "password" ? "Show" : "Hide";
});

loginForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  msg.classList.add("hidden");
  const body = {
    email: emailEl.value.trim(),
    password: passwordEl.value
  };
  if (!body.email || !body.password) {
    showMsg("Email and password are required.");
    return;
  }
  const r = await apiFetch("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(body)
  });
  if (!r.ok) {
    showMsg(await readErrorMessage(r, "Invalid email or password"));
    return;
  }
  const user = await r.json();
  if (rememberEl.checked) {
    localStorage.setItem("hirenest_remember_email", body.email);
  } else {
    localStorage.removeItem("hirenest_remember_email");
  }
  saveAuthAndRedirect(user);
});

initGoogleLogin();
