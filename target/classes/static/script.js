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

  function setupHeroParallax() {
    const home = document.querySelector(".ta-homepage");
    const blobs = document.querySelectorAll(".ta-bg-blobs .blob");
    if (!home || blobs.length === 0 || prefersReducedMotion) return;

    window.addEventListener(
      "pointermove",
      (event) => {
        const x = (event.clientX / window.innerWidth - 0.5) * 14;
        const y = (event.clientY / window.innerHeight - 0.5) * 10;
        blobs.forEach((blob, idx) => {
          const factor = idx + 1;
          blob.style.transform = `translate(${x / factor}px, ${y / factor}px)`;
        });
      },
      { passive: true }
    );
  }

  function setupScroll3DEffects() {
    if (prefersReducedMotion) return;

    const scroll3dTargets = document.querySelectorAll(
      ".ta-hero, .ta-feature-card, .ta-how, .ta-benefit-card, .ta-cta, .ta-step"
    );
    const blobs = document.querySelectorAll(".ta-bg-blobs .blob");

    if (scroll3dTargets.length === 0 && blobs.length === 0) return;

    scroll3dTargets.forEach((el) => el.classList.add("ta-scroll-3d"));

    const clamp = (val, min, max) => Math.max(min, Math.min(max, val));

    let ticking = false;
    const onScroll = () => {
      if (ticking) return;
      ticking = true;
      requestAnimationFrame(() => {
        const vh = window.innerHeight || 1;
        const scrollY = window.scrollY || window.pageYOffset || 0;

        scroll3dTargets.forEach((el) => {
          const rect = el.getBoundingClientRect();
          const center = rect.top + rect.height / 2;
          const distanceFromCenter = (center - vh / 2) / vh;
          const proximity = 1 - clamp(Math.abs(distanceFromCenter), 0, 1);

          const rotateX = clamp(distanceFromCenter * -10, -7, 7);
          const rotateY = clamp(distanceFromCenter * 6, -5, 5);
          const depth = Math.round(proximity * 42);
          const yOffset = Math.round(distanceFromCenter * -14);
          const scale = (0.986 + proximity * 0.03).toFixed(3);

          el.style.setProperty("--scroll-rotate-x", `${rotateX.toFixed(2)}deg`);
          el.style.setProperty("--scroll-rotate-y", `${rotateY.toFixed(2)}deg`);
          el.style.setProperty("--scroll-depth", `${depth}px`);
          el.style.setProperty("--scroll-offset-y", `${yOffset}px`);
          el.style.setProperty("--scroll-scale", `${scale}`);
        });

        blobs.forEach((blob, idx) => {
          const dir = idx % 2 === 0 ? 1 : -1;
          const speedY = 0.06 + idx * 0.02;
          const speedX = 0.03 + idx * 0.015;
          const y = (scrollY * speedY * dir).toFixed(2);
          const x = (scrollY * speedX * -dir).toFixed(2);
          blob.style.setProperty("--blob-scroll-y", `${y}px`);
          blob.style.setProperty("--blob-scroll-x", `${x}px`);
        });

        ticking = false;
      });
    };

    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    window.addEventListener("resize", onScroll, { passive: true });
  }

  document.addEventListener("DOMContentLoaded", () => {
    applyPageEnter();
    setupNavbarShadowAndProgress();
    markAnimatableElements();
    setupScrollReveal();
    setupPageTransitions();
    setupRipples();
    setupHeroParallax();
    setupScroll3DEffects();
  });
})();

