(() => {
  const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  function applyPageEnter() {
    if (prefersReducedMotion) {
      document.body.classList.add("page-ready");
      return;
    }
    requestAnimationFrame(() => document.body.classList.add("page-ready"));
  }

  function setupNavbarShadowAndProgress() {
    const header = document.querySelector("header");
    if (!header) return;
    let progress = header.querySelector(".scroll-progress");
    if (!progress) {
      progress = document.createElement("div");
      progress.className = "scroll-progress";
      header.appendChild(progress);
    }
    const onScroll = () => {
      header.classList.toggle("header-scrolled", window.scrollY > 8);
      const scrollHeight = document.documentElement.scrollHeight - window.innerHeight;
      const percent = scrollHeight > 0 ? (window.scrollY / scrollHeight) * 100 : 0;
      progress.style.width = `${percent}%`;
    };
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
  }

  function markAnimatableElements() {
    const selectors = [
      "main section",
      ".card",
      "form",
      ".hero",
      ".feature-card",
      "main h2",
      "main h3"
    ];
    selectors.forEach((selector) => {
      document.querySelectorAll(selector).forEach((el) => {
        if (!el.classList.contains("animate-on-scroll")) {
          el.classList.add("animate-on-scroll", "fade-up");
        }
      });
    });
  }

  function setupScrollReveal() {
    if (prefersReducedMotion || !("IntersectionObserver" in window)) {
      document.querySelectorAll(".animate-on-scroll").forEach((el) => el.classList.add("is-visible"));
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add("is-visible");
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.12, rootMargin: "0px 0px -40px 0px" }
    );
    document.querySelectorAll(".animate-on-scroll").forEach((el) => observer.observe(el));
  }

  function setupPageTransitions() {
    document.addEventListener("click", (event) => {
      const anchor = event.target.closest("a[href]");
      if (!anchor) return;
      if (anchor.target === "_blank" || anchor.hasAttribute("download")) return;

      const href = anchor.getAttribute("href");
      if (!href || href.startsWith("#") || href.startsWith("javascript:")) return;

      const url = new URL(href, window.location.origin);
      const isInternal = url.origin === window.location.origin;
      if (!isInternal || url.pathname === window.location.pathname) return;
      if (prefersReducedMotion) return;

      event.preventDefault();
      document.body.classList.add("page-transition-out");
      setTimeout(() => {
        window.location.href = url.href;
      }, 220);
    });
  }

  function setupRipples() {
    document.body.addEventListener("click", (event) => {
      const target = event.target.closest(
        ".ripple-container, button, .tab-btn, .dashboard-tab-btn, .secondary-btn, .filter-btn, .action-btn, .cta-btn"
      );
      if (!target) return;
      const rect = target.getBoundingClientRect();
      const ripple = document.createElement("span");
      ripple.className = "ripple";
      const size = Math.max(rect.width, rect.height);
      const x = event.clientX - rect.left - size / 2;
      const y = event.clientY - rect.top - size / 2;
      ripple.style.width = ripple.style.height = `${size}px`;
      ripple.style.left = `${x}px`;
      ripple.style.top = `${y}px`;
      target.classList.add("ripple-container");
      target.appendChild(ripple);
      setTimeout(() => ripple.remove(), 500);
    });
  }

  document.addEventListener("DOMContentLoaded", () => {
    applyPageEnter();
    setupNavbarShadowAndProgress();
    markAnimatableElements();
    setupScrollReveal();
    setupPageTransitions();
    setupRipples();
  });
})();
