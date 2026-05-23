const resetForm = document.getElementById("resetPasswordForm");
const resetMsg = document.getElementById("resetMsg");
const resetTokenInput = document.getElementById("resetToken");
const newPasswordInput = document.getElementById("newPassword");
const confirmPasswordInput = document.getElementById("confirmPassword");

function showResetMsg(text, isError = true) {
  resetMsg.textContent = text;
  resetMsg.classList.remove("hidden", "success", "error");
  resetMsg.classList.add(isError ? "error" : "success");
}

function wirePasswordToggle(buttonId, inputEl) {
  const btn = document.getElementById(buttonId);
  btn?.addEventListener("click", () => {
    const next = inputEl.type === "password" ? "text" : "password";
    inputEl.type = next;
    btn.textContent = next === "password" ? "Show" : "Hide";
  });
}

wirePasswordToggle("toggleNewPassword", newPasswordInput);
wirePasswordToggle("toggleConfirmPassword", confirmPasswordInput);

const qsToken = new URLSearchParams(location.search).get("token");
if (qsToken) {
  resetTokenInput.value = qsToken;
}

resetForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  resetMsg.classList.add("hidden");
  const payload = {
    token: resetTokenInput.value.trim(),
    newPassword: newPasswordInput.value,
    confirmPassword: confirmPasswordInput.value
  };
  if (!payload.token || !payload.newPassword || !payload.confirmPassword) {
    showResetMsg("All fields are required.");
    return;
  }
  if (payload.newPassword !== payload.confirmPassword) {
    showResetMsg("Passwords do not match.");
    return;
  }
  const res = await apiFetch("/api/auth/reset-password", {
    method: "POST",
    body: JSON.stringify(payload)
  });
  if (!res.ok) {
    showResetMsg("Password reset failed. Check token validity or try again.");
    return;
  }
  const data = await res.json();
  showResetMsg(data.message || "Password reset successful.", false);
  setTimeout(() => {
    location.href = "/login.html";
  }, 900);
});

