/**
 * Job Seeker AI Assistant — calls POST /api/ai/chat with JWT.
 */
(function () {
  const userId = localStorage.getItem("hirenest_userId");
  const role = localStorage.getItem("hirenest_role");
  if (!userId || role !== "JOB_SEEKER") return;

  const panel = document.getElementById("aiChatPanel");
  const fab = document.getElementById("aiChatFab");
  const closeBtn = document.getElementById("aiChatClose");
  const messagesEl = document.getElementById("aiChatMessages");
  const form = document.getElementById("aiChatForm");
  const input = document.getElementById("aiChatInput");
  const quickWrap = document.getElementById("aiChatQuickPrompts");

  if (!panel || !fab || !messagesEl || !form || !input) return;

  const QUICK_PROMPTS = [
    "Which jobs fit my profile?",
    "What skills am I missing?",
    "What should I learn next?",
    "Suggest a learning roadmap for my gaps"
  ];

  function appendBubble(text, who) {
    const div = document.createElement("div");
    div.className = `ai-chat-bubble ${who}`;
    const pre = document.createElement("div");
    pre.className = "ai-chat-text";
    pre.textContent = text;
    div.appendChild(pre);
    messagesEl.appendChild(div);
    messagesEl.scrollTop = messagesEl.scrollHeight;
  }

  function setLoading(on) {
    if (form.querySelector("button[type=submit]")) {
      form.querySelector("button[type=submit]").disabled = on;
    }
    input.disabled = on;
  }

  async function sendMessage(text) {
    const trimmed = String(text || "").trim();
    if (!trimmed) return;
    appendBubble(trimmed, "user");
    input.value = "";
    setLoading(true);
    try {
      const res = await apiFetch("/api/ai/chat", {
        method: "POST",
        body: JSON.stringify({ userId: Number(userId), message: trimmed })
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        appendBubble(data.message || "Could not get a reply. Please try again.", "assistant");
        return;
      }
      const reply = data.aiReply || "(No reply text)";
      appendBubble(reply, "assistant");
    } catch (e) {
      appendBubble("Network error. Check your connection and try again.", "assistant");
    } finally {
      setLoading(false);
    }
  }

  fab.addEventListener("click", () => {
    panel.classList.toggle("open");
    fab.setAttribute("aria-expanded", panel.classList.contains("open") ? "true" : "false");
    if (panel.classList.contains("open")) input.focus();
  });

  closeBtn?.addEventListener("click", () => {
    panel.classList.remove("open");
    fab.setAttribute("aria-expanded", "false");
  });

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    sendMessage(input.value);
  });

  if (quickWrap) {
    QUICK_PROMPTS.forEach((label) => {
      const b = document.createElement("button");
      b.type = "button";
      b.className = "ai-chat-quick";
      b.textContent = label;
      b.addEventListener("click", () => {
        sendMessage(label);
      });
      quickWrap.appendChild(b);
    });
  }
})();
