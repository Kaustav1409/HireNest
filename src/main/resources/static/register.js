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

const registerForm = document.getElementById("registerForm");
const registerMsg = document.getElementById("registerMsg");
const registerPassword = document.getElementById("password");
const toggleRegisterPassword = document.getElementById("toggleRegisterPassword");
const registerRole = document.getElementById("role");
const googleRegisterButton = document.getElementById("googleRegisterButton");
const googleClientId = String(document.body?.dataset?.googleClientId || "").trim();

function showRegisterMsg(text, isError = true) {
  registerMsg.textContent = text;
  registerMsg.classList.remove("hidden", "success", "error");
  registerMsg.classList.add(isError ? "error" : "success");
}

async function readErrorMessage(response) {
  const clone = response.clone();
  try {
    const payload = await response.json();
    if (payload && typeof payload.message === "string" && payload.message.trim()) {
      return payload.message.trim();
    }
  } catch (_) {
    // Fallback to default message if response is not JSON.
  }
  try {
    const text = (await clone.text()).trim();
    if (text) return text;
  } catch (_) {
    // ignore text parse errors
  }
  return "Registration failed. Please check your details and try again.";
}

function redirectByRole(user) {
  const role = (user.role || "").toUpperCase();
  if (role === "RECRUITER") {
    location.href = "/dashboard/recruiter.html";
  } else {
    location.href = "/onboarding.html";
  }
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
  redirectByRole(user);
}

async function handleGoogleCredential(credential) {
  if (!credential) {
    showRegisterMsg("Google sign-up credential missing.");
    return;
  }
  const selectedRole = String(registerRole?.value || "").trim().toUpperCase();
  if (selectedRole !== "JOB_SEEKER" && selectedRole !== "RECRUITER") {
    showRegisterMsg("Invalid role selected.");
    return;
  }
  let response;
  try {
    response = await apiFetch("/api/auth/google", {
      method: "POST",
      body: JSON.stringify({ idToken: credential, credential, role: selectedRole })
    });
  } catch (err) {
    showRegisterMsg(err?.message || "Google sign-up request failed.");
    return;
  }
  if (!response.ok) {
    showRegisterMsg(await readErrorMessage(response));
    return;
  }
  const user = await response.json();
  showRegisterMsg("Google account authenticated. Redirecting...", false);
  setTimeout(() => saveAuthAndRedirect(user), 300);
}

function initGoogleRegister(attempt = 0) {
  if (!googleRegisterButton) {
    return;
  }
  if (!googleClientId || googleClientId === "YOUR_GOOGLE_CLIENT_ID") {
    showRegisterMsg("Google Sign-Up is not configured. Add your Google Client ID.");
    return;
  }
  if (!window.google?.accounts?.id) {
    if (attempt < 10) {
      setTimeout(() => initGoogleRegister(attempt + 1), 250);
    }
    return;
  }
  window.google.accounts.id.initialize({
    client_id: googleClientId,
    callback: (response) => handleGoogleCredential(response.credential)
  });
  window.google.accounts.id.renderButton(googleRegisterButton, {
    theme: "outline",
    size: "large",
    text: "continue_with",
    shape: "pill",
    width: 320
  });
}

toggleRegisterPassword?.addEventListener("click", () => {
  const next = registerPassword.type === "password" ? "text" : "password";
  registerPassword.type = next;
  toggleRegisterPassword.textContent = next === "password" ? "Show" : "Hide";
});

registerForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  registerMsg.classList.add("hidden");
  const body = {
    fullName: document.getElementById("fullName").value.trim(),
    email: document.getElementById("email").value.trim(),
    password: registerPassword.value,
    role: String(document.getElementById("role").value || "").trim().toUpperCase()
  };
  if (!body.fullName || !body.email || !body.password) {
    showRegisterMsg("Please fill all required fields.");
    return;
  }
  if (body.role !== "JOB_SEEKER" && body.role !== "RECRUITER") {
    showRegisterMsg("Invalid role selected.");
    return;
  }
  const r = await apiFetch("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(body)
  });
  if (!r.ok) {
    showRegisterMsg(await readErrorMessage(r));
    return;
  }
  const loginRes = await apiFetch("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email: body.email, password: body.password })
  });
  if (!loginRes.ok) {
    showRegisterMsg("Registered. Please login to continue onboarding.", false);
    setTimeout(() => (location.href = "/login.html"), 900);
    return;
  }
  const user = await loginRes.json();
  localStorage.setItem("hirenest_token", user.token || "");
  localStorage.setItem("hirenest_userId", String(user.userId));
  localStorage.setItem("hirenest_role", user.role);
  if (user.recruiterId) {
    localStorage.setItem("hirenest_recruiterId", String(user.recruiterId));
  } else {
    localStorage.removeItem("hirenest_recruiterId");
  }
  showRegisterMsg("Account created. Redirecting...", false);
  setTimeout(() => redirectByRole(user), 800);
});

initGoogleRegister();
