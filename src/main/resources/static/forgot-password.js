const forgotForm = document.getElementById("forgotPasswordForm");
const forgotMsg = document.getElementById("forgotMsg");
const forgotTokenBox = document.getElementById("forgotTokenBox");
const resetTokenValue = document.getElementById("resetTokenValue");
const goResetLink = document.getElementById("goResetLink");

function showForgotMsg(text, isError = true) {
  forgotMsg.textContent = text;
  forgotMsg.classList.remove("hidden", "success", "error");
  forgotMsg.classList.add(isError ? "error" : "success");
}

forgotForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  forgotMsg.classList.add("hidden");
  forgotTokenBox.classList.add("hidden");
  const email = document.getElementById("forgotEmail").value.trim();
  if (!email) {
    showForgotMsg("Email is required.");
    return;
  }
  const res = await apiFetch("/api/auth/forgot-password", {
    method: "POST",
    body: JSON.stringify({ email })
  });
  if (!res.ok) {
    showForgotMsg("Unable to generate reset token. Check the email and try again.");
    return;
  }
  const data = await res.json();
  showForgotMsg(data.message || "Reset token generated.", false);
  if (data.resetToken) {
    resetTokenValue.textContent = data.resetToken;
    goResetLink.href = `/reset-password.html?token=${encodeURIComponent(data.resetToken)}`;
    forgotTokenBox.classList.remove("hidden");
  }
});

