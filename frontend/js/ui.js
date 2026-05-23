function showToast(message, type = "default") {
  const existing = document.getElementById("appToast");
  if (existing) existing.remove();
  const toast = document.createElement("div");
  toast.id = "appToast";
  toast.className = `toast ${type !== "default" ? type : ""}`.trim();
  toast.textContent = message;
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 2200);
}

function setLoading(id, text = "Loading...") {
  const el = document.getElementById(id);
  if (!el) return;
  if (el.dataset.skeleton === "jobs") {
    const skeletonCards = Array.from({ length: 3 })
      .map(
        () => `
      <div class="skeleton-card">
        <div class="skeleton skeleton-line long"></div>
        <div class="skeleton skeleton-line medium"></div>
        <div class="skeleton skeleton-line short"></div>
      </div>`
      )
      .join("");
    el.innerHTML = skeletonCards;
    return;
  }
  if (el.dataset.skeleton === "suggestions") {
    const skeletonCards = Array.from({ length: 3 })
      .map(
        () => `
      <div class="skeleton-card">
        <div class="skeleton skeleton-line medium"></div>
        <div class="skeleton skeleton-line long" style="height: 10px;"></div>
        <div class="skeleton skeleton-line short" style="height: 34px; border-radius: 12px;"></div>
      </div>`
      )
      .join("");
    el.innerHTML = skeletonCards;
    return;
  }
  el.innerHTML = `<div class="muted">${text}</div>`;
}

function setEmpty(id, text = "No data available.") {
  const el = document.getElementById(id);
  if (!el) return;
  el.innerHTML = `<div class="muted">${text}</div>`;
}

/**
 * Dashboard section tabs: shows one `.tab-panel` at a time. Only elements with a valid
 * `data-target` matching an existing panel `id` are wired; others are ignored even if they share the button class.
 */
function setupTabs(buttonClass, panelClass) {
  const panelEls = [...document.querySelectorAll(`.${panelClass}`)];
  const panelIds = new Set(panelEls.map((p) => p.id).filter(Boolean));
  if (!panelEls.length || !panelIds.size) return;

  const rawButtons = [...document.querySelectorAll(`.${buttonClass}`)];
  const tabButtons = rawButtons.filter((btn) => {
    const t = (btn.dataset.target || "").trim();
    return t !== "" && panelIds.has(t);
  });
  if (!tabButtons.length) return;

  tabButtons.forEach((btn) => {
    btn.addEventListener("click", () => {
      const target = (btn.dataset.target || "").trim();
      if (!target || !panelIds.has(target)) return;

      tabButtons.forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      panelEls.forEach((p) => p.classList.toggle("hidden", p.id !== target));
    });
  });
}
