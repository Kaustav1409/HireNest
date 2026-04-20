const userId = localStorage.getItem("hirenest_userId");
const role = localStorage.getItem("hirenest_role");
if (!userId || role !== "JOB_SEEKER") location.href = "/login.html";

const logoutBtn = document.getElementById("logoutBtn");
if (logoutBtn) {
  logoutBtn.addEventListener("click", () => {
    [
      "hirenest_userId",
      "hirenest_role",
      "hirenest_recruiterId",
      "hirenest_token",
      "hirenest_email",
      "hirenest_password",
      "savedEmail",
      "savedPassword",
      "rememberEmail",
      "rememberPassword"
    ].forEach((k) => {
      try {
        localStorage.removeItem(k);
        sessionStorage.removeItem(k);
      } catch (_) {
        // ignore
      }
    });
    location.href = "/login.html";
  });
}

let radarChart = null;
let applicationStatusChart = null;
let weeklyActivityChart = null;

const chartState = {
  recommendedJobs: [],
  applications: [],
  filtersBound: false
};
let assessmentInProgress = false;
let tabSwitchViolationHandled = false;
let currentQuizAttemptId = null;
let quizHeartbeatTimer = null;
let quizHeartbeatIntervalSeconds = 5;

function stopQuizHeartbeat() {
  if (quizHeartbeatTimer) {
    clearInterval(quizHeartbeatTimer);
    quizHeartbeatTimer = null;
  }
}

function startQuizHeartbeat() {
  stopQuizHeartbeat();
  if (!currentQuizAttemptId) return;
  const intervalMs = Math.max(2000, Number(quizHeartbeatIntervalSeconds || 5) * 1000);
  quizHeartbeatTimer = setInterval(async () => {
    if (!assessmentInProgress || !currentQuizAttemptId) return;
    try {
      await apiFetch("/api/quiz/attempt/heartbeat", {
        method: "POST",
        body: JSON.stringify({
          userId: Number(userId),
          attemptId: Number(currentQuizAttemptId)
        })
      });
    } catch (_) {
      // Best effort; submit endpoint will also enforce heartbeat freshness.
    }
  }, intervalMs);
}

async function clearAuthSessionAndRedirectForTabSwitch() {
  if (tabSwitchViolationHandled) return;
  tabSwitchViolationHandled = true;
  assessmentInProgress = false;
  stopQuizHeartbeat();
  try {
    if (currentQuizAttemptId) {
      await apiFetch("/api/quiz/attempt/violation", {
        method: "POST",
        body: JSON.stringify({
          userId: Number(userId),
          attemptId: Number(currentQuizAttemptId),
          reason: "TAB_SWITCH_DETECTED"
        })
      });
    }
  } catch (_) {
    // ignore - redirect should still happen
  }
  currentQuizAttemptId = null;
  [
    "hirenest_userId",
    "hirenest_role",
    "hirenest_recruiterId",
    "hirenest_token"
  ].forEach((k) => {
    try {
      localStorage.removeItem(k);
      sessionStorage.removeItem(k);
    } catch (_) {
      // ignore
    }
  });
  location.href = "/login.html?error=tab_switch_detected";
}

function importanceBadgeHtml(importance) {
  const imp = safeUpper(importance);
  if (imp === "HIGH") return `<span class="importance-badge high">🔥 High Demand Skill</span>`;
  if (imp === "MEDIUM") return `<span class="importance-badge medium">Priority Skill</span>`;
  return `<span class="importance-badge low">Recommended Skill</span>`;
}

function openLink(url) {
  if (!url) return;
  window.open(url, "_blank", "noopener,noreferrer");
}

function getSkillSuggestionContainers() {
  return ["skillSuggestionsOverview", "skillSuggestionsMatches"]
    .map((id) => document.getElementById(id))
    .filter(Boolean);
}

function safeUpper(s) {
  return (s || "").toString().trim().toUpperCase();
}

function escapeHtml(str) {
  if (str == null) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function renderSkillChips(skills, type) {
  if (!Array.isArray(skills) || !skills.length) return `<span class="muted">None</span>`;
  return `<div class="skill-chip-row">${skills
    .map((s) => `<span class="skill-chip ${type}">${escapeHtml(s)}</span>`)
    .join("")}</div>`;
}

function renderRoadmapResources(items, levelLabel) {
  if (!Array.isArray(items) || !items.length) return "";
  return `
    <div class="roadmap-level">
      <div class="roadmap-level-head">
        <span class="roadmap-level-badge">${levelLabel}</span>
      </div>
      <ul class="roadmap-list">
        ${items.map((r) => `
          <li class="roadmap-resource-card">
            <a href="${r.url}" target="_blank" rel="noopener noreferrer">${escapeHtml(r.title || "Open resource")}</a>
            <span class="roadmap-type-pill">${escapeHtml(r.type || "resource")}</span>
          </li>
        `).join("")}
      </ul>
    </div>
  `;
}

function renderLearningRecommendations(roadmaps, legacyItems) {
  const useRoadmaps = Array.isArray(roadmaps) && roadmaps.length;
  if (!useRoadmaps && (!Array.isArray(legacyItems) || !legacyItems.length)) {
    return `<div class="muted">No learning recommendations needed.</div>`;
  }

  if (useRoadmaps) {
    return `<div class="learning-grid">${roadmaps
      .map(
        (rm, idx) => `
      <details class="roadmap-skill-details learning-item" ${idx === 0 ? "open" : ""}>
        <summary class="roadmap-skill-summary">
          <span class="roadmap-summary-title">${escapeHtml(rm.skillName || "Skill")}</span>
          <span class="roadmap-skill-kicker">Learning roadmap</span>
        </summary>
        <div class="roadmap-skill-body">
          ${renderRoadmapResources(rm.beginnerResources || [], "Beginner")}
          ${renderRoadmapResources(rm.intermediateResources || [], "Intermediate")}
          ${renderRoadmapResources(rm.advancedResources || [], "Advanced")}
        </div>
      </details>
    `
      )
      .join("")}</div>`;
  }

  // Backward-compatible fallback rendering from old structure.
  return `<div class="learning-grid">${legacyItems
    .map((rec) => {
      const skill = escapeHtml(rec.skillName || rec.skill || "Skill");
      const beginner = rec.beginnerLink || rec.primaryLink || "";
      const intermediate = rec.intermediateLink || rec.primaryLink || "";
      const advanced = rec.advancedLink || rec.primaryLink || "";
      const type = escapeHtml(rec.resourceType || "course");
      return `
        <div class="learning-item">
          <b>${skill}</b>
          <div class="muted" style="font-size:12px; margin-bottom:6px;">Primary type: ${type}</div>
          ${beginner ? `<a href="${beginner}" target="_blank" rel="noopener noreferrer">Beginner</a>` : ""}
          ${intermediate ? `<a href="${intermediate}" target="_blank" rel="noopener noreferrer">Intermediate</a>` : ""}
          ${advanced ? `<a href="${advanced}" target="_blank" rel="noopener noreferrer">Advanced</a>` : ""}
        </div>
      `;
    })
    .join("")}</div>`;
}

function getMatchLabelClass(label) {
  const s = safeUpper(label);
  if (s === "STRONG MATCH") return "label-strong";
  if (s === "GOOD MATCH") return "label-good";
  if (s === "PARTIAL MATCH") return "label-partial";
  return "label-low";
}

function salaryText(minSalary, maxSalary) {
  const hasMin = minSalary != null && !Number.isNaN(Number(minSalary));
  const hasMax = maxSalary != null && !Number.isNaN(Number(maxSalary));
  if (!hasMin && !hasMax) return "Salary not disclosed";
  if (hasMin && hasMax) return `₹${Number(minSalary).toLocaleString()} - ₹${Number(maxSalary).toLocaleString()}`;
  if (hasMin) return `From ₹${Number(minSalary).toLocaleString()}`;
  return `Up to ₹${Number(maxSalary).toLocaleString()}`;
}

function buildMatchFilterQuery() {
  const minSalary = document.getElementById("filterMinSalary")?.value?.trim();
  const maxSalary = document.getElementById("filterMaxSalary")?.value?.trim();
  const company = document.getElementById("filterCompany")?.value?.trim();
  const location = document.getElementById("filterLocation")?.value?.trim();
  const keyword = document.getElementById("filterKeyword")?.value?.trim();
  const remote = document.getElementById("filterRemote")?.value;

  const params = new URLSearchParams();
  if (minSalary) params.set("minSalary", minSalary);
  if (maxSalary) params.set("maxSalary", maxSalary);
  if (company) params.set("company", company);
  if (location) params.set("location", location);
  if (keyword) params.set("keyword", keyword);
  if (remote === "true" || remote === "false") params.set("remote", remote);
  const query = params.toString();
  return query ? `?${query}` : "";
}

async function loadMatchedJobsWithFilters() {
  const jobsEl = document.getElementById("jobs");
  if (!jobsEl) return;
  setLoading("jobs", "Applying filters and ranking jobs...");
  const query = buildMatchFilterQuery();
  const res = await apiFetch(`/api/matching/${userId}${query}`);
  const jobs = await res.json();
  renderMatchedJobs(Array.isArray(jobs) ? jobs : []);
}

function renderMatchedJobs(recJobs) {
  const jobsEl = document.getElementById("jobs");
  if (!jobsEl) return;
  jobsEl.innerHTML = "";
  if (!recJobs.length) {
    setEmpty("jobs", "No jobs matched your current filters.");
    return;
  }

  recJobs.forEach((j) => {
    const card = document.createElement("div");
    card.className = "card match-card";
    const explain = j.explanation
      ? `<p class="match-explanation">${escapeHtml(j.explanation)}</p>`
      : "";
    const gapSummary = j.gapSummary
      ? `<div class="gap-summary">${escapeHtml(j.gapSummary)}</div>`
      : "";
    const matchedSkillsHtml = renderSkillChips(j.matchedSkills || [], "match");
    const missingSkillsHtml = renderSkillChips(j.missingSkills || [], "missing");
    const learningHtml = renderLearningRecommendations(j.learningRoadmaps || [], j.learningRecommendations || []);
    const companyName = escapeHtml(j.companyName || "Unknown company");
    const label = escapeHtml(j.matchLabel || "Low Match");
    const labelClass = getMatchLabelClass(j.matchLabel);
    const rankingScore = j.rankingScore != null ? Number(j.rankingScore).toFixed(2) : "0.00";
    const isRemote = Boolean(j.remote);
    const location = escapeHtml(j.location || "Location not specified");
    const salary = escapeHtml(salaryText(j.minSalary, j.maxSalary));
    card.innerHTML = `
      <div class="match-top-accent"></div>
      <div class="match-header">
        <div class="match-title-wrap">
          <b class="match-title">${escapeHtml(j.title || "")}</b>
          <div class="company-line">${companyName}</div>
        </div>
        <div class="match-badge-row">
          <span class="match-pill">${j.matchScore}% Match</span>
          <span class="label-pill ${labelClass}">${label}</span>
          <span class="rank-pill">Rank ${rankingScore}</span>
        </div>
      </div>
      <div class="job-meta-row">
        <span class="meta-chip">${location}</span>
        <span class="meta-chip ${isRemote ? "remote" : "onsite"}">${isRemote ? "Remote" : "On-site"}</span>
        <span class="meta-chip">${salary}</span>
      </div>
      <div class="match-explain-wrap">${explain}</div>
      ${gapSummary}
      <div class="match-section">
        <div class="match-label">Matched Skills</div>
        ${matchedSkillsHtml}
      </div>
      <div class="match-section">
        <div class="match-label">Missing Skills</div>
        ${missingSkillsHtml}
      </div>
      <div class="match-section">
        <div class="match-label">Learning Recommendations</div>
        <div class="muted roadmap-note">Start with Beginner resources first, then move to Intermediate and Advanced.</div>
        ${learningHtml}
      </div>
      <div class="match-actions">
        <button class="save-btn" data-job="${j.jobId}">Save Job</button>
        <button class="apply-btn" data-job="${j.jobId}">Apply</button>
      </div>
    `;
    jobsEl.appendChild(card);
  });

  document.querySelectorAll(".save-btn").forEach((btn) => {
    btn.addEventListener("click", async () => {
      await apiFetch("/api/jobs/save", {
        method: "POST",
        body: JSON.stringify({ userId: Number(userId), jobId: Number(btn.dataset.job) })
      });
      showToast("Job saved", "success");
      await loadDashboard();
      await loadActivity();
    });
  });
  document.querySelectorAll(".apply-btn").forEach((btn) => {
    btn.addEventListener("click", async () => {
      await apiFetch(`/api/job-seeker/${userId}/apply/${btn.dataset.job}`, { method: "POST" });
      showToast("Application submitted", "success");
      await loadActivity();
    });
  });
}

function renderLevelLinks(linksByLevel) {
  if (!linksByLevel || typeof linksByLevel !== "object") return "";
  const order = ["Beginner", "Intermediate", "Advanced"];
  return order
    .filter((lvl) => Array.isArray(linksByLevel[lvl]) && linksByLevel[lvl].length)
    .map((lvl) => {
      const items = linksByLevel[lvl]
        .map((item) => {
          const url = (item && item.url) || "";
          const label = escapeHtml((item && item.label) || "Open");
          const safeUrl = url.replace(/'/g, "\\'");
          return `<li><button type="button" class="link-inline" onclick="openSkillLink('${safeUrl}')">${label}</button></li>`;
        })
        .join("");
      return `<div class="level-block"><div class="level-title">${lvl}</div><ul class="level-links">${items}</ul></div>`;
    })
    .join("");
}

function getJobIdFromApplication(a) {
  if (!a) return null;
  if (a.job && a.job.id != null) return Number(a.job.id);
  if (a.jobId != null) return Number(a.jobId);
  return null;
}

function parseCreatedAt(value) {
  if (!value) return null;
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? null : d;
}

function applicationTimestamp(a) {
  return a && (a.appliedAt || a.createdAt);
}

function statusBadgeHtml(status) {
  const st = safeUpper(status);
  if (st === "UNDER_REVIEW") return `<span class="status-badge status-under-review">Under Review</span>`;
  if (st === "INTERVIEW") return `<span class="status-badge status-interview">Interview</span>`;
  if (st === "HIRED") return `<span class="status-badge status-hired">Hired</span>`;
  if (st === "SHORTLISTED") return `<span class="status-badge status-shortlisted">Shortlisted</span>`;
  if (st === "REJECTED") return `<span class="status-badge status-rejected">Rejected</span>`;
  return `<span class="status-badge status-applied">Applied</span>`;
}

function applicationProgressPercent(status) {
  const st = safeUpper(status);
  if (st === "HIRED") return 100;
  if (st === "INTERVIEW") return 80;
  if (st === "SHORTLISTED") return 60;
  if (st === "UNDER_REVIEW") return 40;
  if (st === "REJECTED") return 100;
  return 20; // APPLIED
}

function applicationTimelineCompact(status) {
  const st = safeUpper(status);
  const stages = ["APPLIED", "UNDER_REVIEW", "SHORTLISTED", "INTERVIEW", "HIRED"];
  const idx = stages.indexOf(st);
  return `
    <div class="app-stage-row">
      ${stages.map((s, i) => {
        const done = idx >= i || (st === "REJECTED" && i <= 1);
        return `<span class="app-stage-dot ${done ? "done" : ""}" title="${s.replace("_", " ")}"></span>`;
      }).join("")}
    </div>
  `;
}

function startOfWeekLocal(date) {
  const d = new Date(date);
  const day = d.getDay(); // 0=Sun, 1=Mon
  const diff = day === 0 ? -6 : 1 - day; // shift to Monday
  d.setDate(d.getDate() + diff);
  d.setHours(0, 0, 0, 0);
  return d;
}

function formatISODate(date) {
  // YYYY-MM-DD in local time
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function formatMonthLabel(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  return `${y}-${m}`;
}

function groupApplicationsByRange(apps, range) {
  const map = new Map(); // key => {label, sortKey, count}

  apps.forEach((a) => {
    const dt = parseCreatedAt(applicationTimestamp(a));
    if (!dt) return;

    let label;
    let sortKey;
    if (range === "monthly") {
      const monthStart = new Date(dt.getFullYear(), dt.getMonth(), 1);
      label = formatMonthLabel(monthStart);
      sortKey = monthStart.getTime();
    } else {
      const weekStart = startOfWeekLocal(dt);
      label = formatISODate(weekStart);
      sortKey = weekStart.getTime();
    }

    const prev = map.get(label);
    if (!prev) {
      map.set(label, { label, sortKey, count: 1 });
    } else {
      prev.count += 1;
    }
  });

  const items = Array.from(map.values()).sort((a, b) => a.sortKey - b.sortKey);
  // Keep last 10 buckets for readability
  return items.slice(Math.max(0, items.length - 10));
}

function renderCharts() {
  const radarSelect = document.getElementById("radarJobFilter");
  const rangeSelect = document.getElementById("activityRangeFilter");
  const radarHintEl = document.getElementById("radarHint");

  const range = rangeSelect ? rangeSelect.value : "weekly";
  const selectedValue = radarSelect ? radarSelect.value : "ALL";
  const recJobs = chartState.recommendedJobs || [];
  const appsAll = chartState.applications || [];

  const topJob = recJobs[0];
  const selectedJob = selectedValue === "ALL"
    ? topJob
    : recJobs.find((j) => String(j.jobId) === String(selectedValue)) || topJob;

  const selectedJobIdForApps = selectedValue === "ALL" ? null : Number(selectedValue);
  const appsFiltered = selectedJobIdForApps
    ? appsAll.filter((a) => getJobIdFromApplication(a) === selectedJobIdForApps)
    : appsAll;

  // 1) Radar chart
  const radarCanvas = document.getElementById("skillMatchRadar");
  if (radarCanvas && selectedJob) {
    const matched = Array.isArray(selectedJob.matchedSkills) ? selectedJob.matchedSkills : [];
    const missing = Array.isArray(selectedJob.missingSkills) ? selectedJob.missingSkills : [];
    const required = [...matched, ...missing];

    const requiredUnique = Array.from(new Set(required));
    // Limit to keep it readable
    const requiredSkills = (matched.length ? matched : requiredUnique).concat(missing).filter((s, idx, arr) => arr.indexOf(s) === idx).slice(0, 8);

    const candidateSeries = requiredSkills.map((s) => (matched.includes(s) ? 100 : 0));
    const requiredSeries = requiredSkills.map(() => 100);

    const labels = requiredSkills.length ? requiredSkills : ["No required skills"];
    const cand = candidateSeries.length ? candidateSeries : [0];
    const req = requiredSeries.length ? requiredSeries : [100];

    if (!radarChart) {
      radarChart = new Chart(radarCanvas, {
        type: "radar",
        data: {
          labels,
          datasets: [
            {
              label: "Your Skills",
              data: cand,
              borderColor: "#4f46e5",
              backgroundColor: "rgba(79, 70, 229, 0.18)",
              pointBackgroundColor: "#4f46e5"
            },
            {
              label: "Required Skills",
              data: req,
              borderColor: "#06b6d4",
              backgroundColor: "rgba(6, 182, 212, 0.16)",
              pointBackgroundColor: "#06b6d4"
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          animation: { duration: 700, easing: "easeOutQuart" },
          plugins: {
            legend: { position: "bottom" },
            tooltip: { callbacks: { label: (ctx) => ` ${ctx.dataset.label}: ${ctx.raw}%` } }
          },
          scales: {
            r: {
              min: 0,
              max: 100,
              ticks: { callback: (v) => `${v}%` },
              grid: { color: "rgba(15, 23, 42, 0.08)" }
            }
          }
        }
      });
    } else {
      radarChart.data.labels = labels;
      radarChart.data.datasets[0].data = cand;
      radarChart.data.datasets[1].data = req;
      radarChart.update({ duration: 650 });
    }

    if (radarHintEl) {
      const missingCount = (missing || []).length;
      radarHintEl.textContent = missingCount
        ? `Missing ${missingCount} skill(s). Use the job gaps panel below to learn and improve.`
        : `Perfect match for this job. Keep it going!`;
    }
  }

  // 2) Application status pie chart
  const pieCanvas = document.getElementById("applicationStatusPie");
  if (pieCanvas) {
    const statuses = ["APPLIED", "SHORTLISTED", "REJECTED"];
    const counts = statuses.map((st) =>
      appsFiltered.filter((a) => safeUpper(a.status) === st).length
    );

    const colors = ["#ca8a04", "#2563eb", "#dc2626"];

    if (!applicationStatusChart) {
      applicationStatusChart = new Chart(pieCanvas, {
        type: "pie",
        data: {
          labels: statuses.map((s) => s[0] + s.slice(1).toLowerCase()),
          datasets: [
            {
              data: counts,
              backgroundColor: colors,
              borderColor: "rgba(255,255,255,0.9)",
              borderWidth: 2
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          animation: { duration: 650, easing: "easeOutQuart" },
          plugins: {
            legend: { position: "bottom" },
            tooltip: {
              callbacks: {
                label: (ctx) => ` ${ctx.label}: ${ctx.raw}`
              }
            }
          }
        }
      });
    } else {
      applicationStatusChart.data.datasets[0].data = counts;
      applicationStatusChart.update({ duration: 600 });
    }
  }

  // 3) Weekly/Monthly activity line chart
  const lineCanvas = document.getElementById("weeklyActivityLine");
  if (lineCanvas) {
    const grouped = groupApplicationsByRange(appsFiltered, range);
    const labels = grouped.length ? grouped.map((x) => x.label) : ["No data"];
    const data = grouped.length ? grouped.map((x) => x.count) : [0];

    if (!weeklyActivityChart) {
      weeklyActivityChart = new Chart(lineCanvas, {
        type: "line",
        data: {
          labels,
          datasets: [
            {
              label: "Applications",
              data,
              borderColor: "#06b6d4",
              backgroundColor: "rgba(6, 182, 212, 0.12)",
              pointBackgroundColor: "#06b6d4",
              fill: true,
              tension: 0.35
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          animation: { duration: 700, easing: "easeOutQuart" },
          plugins: {
            legend: { position: "bottom" },
            tooltip: {
              callbacks: {
                label: (ctx) => ` ${ctx.dataset.label}: ${ctx.raw}`
              }
            }
          },
          scales: {
            y: {
              beginAtZero: true,
              ticks: { precision: 0 },
              grid: { color: "rgba(15, 23, 42, 0.08)" }
            }
          }
        }
      });
    } else {
      weeklyActivityChart.data.labels = labels;
      weeklyActivityChart.data.datasets[0].data = data;
      weeklyActivityChart.update({ duration: 650 });
    }
  }
}

function setStatSkeleton(id) {
  const el = document.getElementById(id);
  if (!el) return;
  el.innerHTML = `<div class="skeleton skeleton-line long"></div>`;
}

function renderDetectedSkillsUi(detectedSkills = [], mergedSkillsCsv = "") {
  const detectedBox = document.getElementById("detectedSkillsBox");
  const detectedText = document.getElementById("detectedSkillsText");
  const detectedChips = document.getElementById("detectedSkillsChips");
  const mergedWrap = document.getElementById("mergedSkillsWrap");
  const mergedChips = document.getElementById("mergedSkillsChips");
  if (!detectedBox || !detectedText || !detectedChips || !mergedWrap || !mergedChips) return;

  const detected = Array.isArray(detectedSkills) ? detectedSkills : [];
  const merged = String(mergedSkillsCsv || "").trim();

  if (!detected.length) {
    detectedBox.classList.add("hidden");
    detectedText.textContent = "";
    detectedChips.innerHTML = "";
  } else {
    detectedBox.classList.remove("hidden");
    detectedText.textContent = "Skills identified from your uploaded resume:";
    detectedChips.innerHTML = detected.map((s) => `<span class="skill-chip match">${escapeHtml(s)}</span>`).join("");
  }

  if (merged) {
    mergedWrap.classList.remove("hidden");
    const parts = merged.split(/[,;|]+/).map((s) => s.trim()).filter(Boolean);
    mergedChips.innerHTML = parts.map((s) => `<span class="skill-chip match">${escapeHtml(s)}</span>`).join("");
  } else {
    mergedWrap.classList.add("hidden");
    mergedChips.innerHTML = "";
  }
}

/** Normalize CSV / pipe-separated skills for display in the profile textarea. */
function formatSkillsFieldFromPayload(skillsCsv) {
  if (skillsCsv == null || typeof skillsCsv !== "string") return "";
  const parts = skillsCsv.split(/[,;|]+/).map((s) => s.trim()).filter(Boolean);
  return parts.join(", ");
}

/**
 * Fills My Profile form fields from POST /resume JSON (parsed resume + merged DB state).
 * Only overwrites a field when the payload provides a meaningful value (does not clear with empty).
 */
function applyResumePayloadToProfileForm(payload) {
  if (!payload || typeof payload !== "object") return;
  const skillsEl = document.getElementById("skills");
  const rolesEl = document.getElementById("preferredRoles");
  const locEl = document.getElementById("location");
  const expEl = document.getElementById("experienceYears");
  const bioEl = document.getElementById("bio");

  let skillsVal = "";
  if (typeof payload.mergedSkills === "string" && payload.mergedSkills.trim() !== "") {
    skillsVal = formatSkillsFieldFromPayload(payload.mergedSkills);
  } else if (Array.isArray(payload.detectedSkills) && payload.detectedSkills.length) {
    skillsVal = payload.detectedSkills.map((s) => String(s).trim()).filter(Boolean).join(", ");
  } else if (typeof payload.extractedSkills === "string" && payload.extractedSkills.trim() !== "") {
    skillsVal = formatSkillsFieldFromPayload(payload.extractedSkills);
  }
  if (skillsVal && skillsEl) skillsEl.value = skillsVal;

  if (typeof payload.preferredRoles === "string" && payload.preferredRoles.trim() !== "") {
    if (rolesEl) rolesEl.value = payload.preferredRoles.trim();
  }
  if (typeof payload.location === "string" && payload.location.trim() !== "") {
    if (locEl) locEl.value = payload.location.trim();
  }
  const expRaw = payload.experienceYears;
  if (expRaw !== null && expRaw !== undefined && expRaw !== "") {
    const expNum = Number(expRaw);
    if (Number.isFinite(expNum) && expEl) expEl.value = String(expNum);
  }
  if (typeof payload.bio === "string" && payload.bio.trim() !== "") {
    if (bioEl) bioEl.value = payload.bio.trim();
  }
}

function renderJobSeekerAiInsights(ai = {}) {
  const host = document.getElementById("jobSeekerAiInsights");
  if (!host) return;
  const bf = ai.bestFitRole || {};
  const topMissing = Array.isArray(ai.topMissingSkills) ? ai.topMissingSkills : [];
  const missText = topMissing.length ? topMissing.join(", ") : "None listed yet — complete your profile and review matches.";
  const bestTitle = bf.title ? escapeHtml(bf.title) : "—";
  const bestCo = bf.companyName ? escapeHtml(bf.companyName) : "—";
  const bestScore = bf.matchScore != null ? `${bf.matchScore}%` : "—";
  const bestLabel = bf.matchLabel ? escapeHtml(bf.matchLabel) : "";
  host.innerHTML = `
    <div class="card ai-insight-card">
      <div class="ai-insight-kicker">Best-fit role</div>
      <div class="ai-insight-title">${bestTitle}</div>
      <p class="muted ai-insight-body">${bestCo} · ${bestScore} match${bestLabel ? ` · ${bestLabel}` : ""}</p>
    </div>
    <div class="card ai-insight-card">
      <div class="ai-insight-kicker">Top missing skills</div>
      <p class="ai-insight-body">${escapeHtml(missText)}</p>
    </div>
    <div class="card ai-insight-card">
      <div class="ai-insight-kicker">Next recommended action</div>
      <p class="ai-insight-body">${escapeHtml(ai.nextRecommendedAction || "Keep your profile and skills updated.")}</p>
    </div>
    <div class="card ai-insight-card">
      <div class="ai-insight-kicker">Placement readiness</div>
      <p class="ai-insight-body">${escapeHtml(ai.placementReadiness || "Complete your profile for a stronger signal.")}</p>
    </div>
  `;
}

function renderProfileCompletion(data = {}) {
  const percent = Number(data.profileCompletionPercentage || 0);
  const missing = Array.isArray(data.missingProfileItems) ? data.missingProfileItems : [];
  const safePercent = Math.max(0, Math.min(100, percent));

  const stat = document.getElementById("profileCompletionStat");
  const badge = document.getElementById("profileCompletionBadge");
  const fill = document.getElementById("profileCompletionFill");
  const missingEl = document.getElementById("missingProfileItems");
  if (stat) stat.textContent = `${safePercent}%`;
  if (badge) badge.textContent = `${safePercent}%`;
  if (fill) fill.style.width = `${safePercent}%`;
  if (missingEl) {
    if (!missing.length) {
      missingEl.innerHTML = `<span class="status-badge status-hired">Profile complete</span> Great work - your profile is ready for strong matching.`;
    } else {
      missingEl.innerHTML = missing.map((item) => `<span class="missing-item-chip">${escapeHtml(item)}</span>`).join("");
    }
  }
}

function renderOverviewRecommendedJobs(recJobs = []) {
  const host = document.getElementById("overviewRecommendedJobs");
  if (!host) return;
  if (!Array.isArray(recJobs) || !recJobs.length) {
    host.innerHTML = `<div class="tracking-empty">No recommended jobs available. Complete your profile first.</div>`;
    return;
  }
  const top = recJobs.slice(0, 3);
  host.innerHTML = top
    .map((j) => {
      const title = escapeHtml(j.title || "Job");
      const company = escapeHtml(j.companyName || "Company");
      const label = escapeHtml(j.matchLabel || "Low Match");
      const labelClass = getMatchLabelClass(j.matchLabel);
      const score = j.matchScore != null ? `${j.matchScore}%` : "0%";
      const explain = j.explanation
        ? `<p class="overview-match-explain">${escapeHtml(j.explanation)}</p>`
        : "";
      return `
        <div class="tracking-card">
          <div class="tracking-head">
            <b>${title}</b>
            <span class="match-pill">${score}</span>
          </div>
          <div class="tracking-subhead">${company}</div>
          <div class="job-meta-row">
            <span class="label-pill ${labelClass}">${label}</span>
          </div>
          ${explain}
        </div>
      `;
    })
    .join("");
}

function updateResumeUi(profile = {}) {
  const statusEl = document.getElementById("resumeStatus");
  const linkEl = document.getElementById("resumeDownloadLink");
  const banner = document.getElementById("resumeUploadedBanner");
  const nameDisplay = document.getElementById("resumeFileNameDisplay");
  const replaceBtn = document.getElementById("replaceResumeBtn");
  if (!statusEl || !linkEl) return;

  const resumeName = (profile.resumeFileName || profile.fileName || "").trim();
  const hasResume = Boolean(resumeName);

  if (hasResume) {
    const uploadedAt = profile.resumeUploadedAt ? new Date(profile.resumeUploadedAt).toLocaleString() : "just now";
    statusEl.textContent = `Last updated: ${uploadedAt}`;
    if (banner) banner.classList.remove("hidden");
    if (nameDisplay) nameDisplay.textContent = resumeName;
    if (replaceBtn) replaceBtn.classList.remove("hidden");
    linkEl.href = "#";
    linkEl.dataset.downloadUrl = profile.downloadUrl || `/api/profile/candidate/${userId}/resume`;
    linkEl.dataset.fileName = resumeName || "resume.pdf";
    linkEl.classList.remove("hidden");

    const extracted = (profile.extractedSkills || "").trim();
    const extractedList = extracted ? extracted.split(",").map((x) => x.trim()).filter(Boolean) : [];
    renderDetectedSkillsUi(extractedList, profile.skills || "");
  } else {
    statusEl.textContent = "No resume uploaded yet.";
    if (banner) banner.classList.add("hidden");
    if (nameDisplay) nameDisplay.textContent = "";
    if (replaceBtn) replaceBtn.classList.add("hidden");
    linkEl.href = "#";
    delete linkEl.dataset.downloadUrl;
    delete linkEl.dataset.fileName;
    linkEl.classList.add("hidden");
    renderDetectedSkillsUi([], "");
  }
}

const resumeDownloadLink = document.getElementById("resumeDownloadLink");
if (resumeDownloadLink) {
  resumeDownloadLink.addEventListener("click", async (e) => {
    e.preventDefault();
    const url = resumeDownloadLink.dataset.downloadUrl;
    if (!url) return;
    const name = resumeDownloadLink.dataset.fileName || "resume.pdf";
    try {
      await downloadResumeWithAuth(url, name);
    } catch (_) {
      showToast("Could not download resume", "error");
    }
  });
}

const resumeFileInput = document.getElementById("resumeFile");
const pickResumeBtn = document.getElementById("pickResumeBtn");
const replaceResumeBtn = document.getElementById("replaceResumeBtn");
if (pickResumeBtn && resumeFileInput) {
  pickResumeBtn.addEventListener("click", () => resumeFileInput.click());
}
if (replaceResumeBtn && resumeFileInput) {
  replaceResumeBtn.addEventListener("click", () => resumeFileInput.click());
}

async function loadDashboard() {
  ["profileCompletionStat", "totalJobs", "matchedJobsCount", "savedJobsCount", "avgQuizScore", "totalApplications", "latestQuizScore"].forEach(setStatSkeleton);
  const radarHintEl = document.getElementById("radarHint");
  if (radarHintEl) radarHintEl.textContent = "Loading analytics...";
  getSkillSuggestionContainers().forEach((c) => setLoading(c.id, "Analyzing skill gaps..."));
  setLoading("jobs", "Fetching recommended jobs...");
  const res = await apiFetch(`/api/dashboard/job-seeker/${userId}`);
  const data = await res.json();
  const insightsRes = await apiFetch(`/api/dashboard/job-seeker/${userId}/insights`);
  const insights = await insightsRes.json();
  let profile = data.profile || {};
  if (!profile.resumeFileName) {
    try {
      const profileRes = await apiFetch(`/api/profile/candidate/${userId}`);
      if (profileRes.ok) {
        const freshProfile = await profileRes.json();
        profile = { ...profile, ...(freshProfile || {}) };
      }
    } catch (_) {
      // Keep dashboard profile fallback
    }
  }
  renderProfileCompletion(data);
  renderJobSeekerAiInsights(data.aiInsights || {});
  document.getElementById("totalJobs").textContent = data.totalJobs || 0;
  document.getElementById("matchedJobsCount").textContent = data.matchedJobsCount || 0;
  document.getElementById("savedJobsCount").textContent = data.savedJobsCount || 0;
  document.getElementById("avgQuizScore").textContent = `${insights.avgQuizScore || 0}%`;
  document.getElementById("totalApplications").textContent = data.totalApplications != null ? data.totalApplications : 0;
  document.getElementById("latestQuizScore").textContent = `${data.assessmentScore != null ? data.assessmentScore : 0}%`;
  updateResumeUi(profile);

  // Populate profile form fields (keeps the module feeling "real-world")
  const skillsEl = document.getElementById("skills");
  const expEl = document.getElementById("experienceYears");
  const bioEl = document.getElementById("bio");
  const rolesEl = document.getElementById("preferredRoles");
  const locEl = document.getElementById("location");
  const remoteEl = document.getElementById("remotePreferred");
  if (skillsEl) skillsEl.value = profile.skills || "";
  if (expEl) expEl.value = profile.experienceYears != null ? profile.experienceYears : "";
  if (bioEl) bioEl.value = profile.bio || "";
  if (rolesEl) rolesEl.value = profile.preferredRoles || "";
  if (locEl) locEl.value = profile.location || "";
  if (remoteEl) remoteEl.checked = Boolean(profile.remotePreferred);
  const recJobs = data.recommendedJobs || [];
  chartState.recommendedJobs = recJobs;
  renderOverviewRecommendedJobs(recJobs);
  if (!recJobs.length) {
    getSkillSuggestionContainers().forEach((c) => setEmpty(c.id, "No skill suggestions available yet."));
    await loadMatchedJobsWithFilters();
    return;
  }

  // Fetch applications for charts (status pie + activity line)
  const appsRes = await apiFetch(`/api/job-seeker/${userId}/applications`);
  const appsAll = await appsRes.json();
  chartState.applications = appsAll;

  // Bind filters once, then update on every reload
  const radarSelect = document.getElementById("radarJobFilter");
  if (radarSelect) {
    radarSelect.innerHTML = `<option value="ALL" selected>All Recommended Jobs</option>`;
    recJobs.slice(0, 8).forEach((j) => {
      const title = j.title || "Job";
      radarSelect.insertAdjacentHTML(
        "beforeend",
        `<option value="${j.jobId}">${title} (${j.matchScore || 0}% match)</option>`
      );
    });
  }

  const rangeSelect = document.getElementById("activityRangeFilter");
  if (rangeSelect) {
    rangeSelect.onchange = () => renderCharts();
  }

  if (radarSelect) {
    radarSelect.onchange = () => renderCharts();
  }
  await loadMatchedJobsWithFilters();

  // Render analytics charts for the initial view
  renderCharts();

  await loadSkillSuggestions();
}

async function loadSkillSuggestions() {
  const containers = getSkillSuggestionContainers();
  if (!containers.length) return;

  try {
    const suggestionsRes = await apiFetch(`/api/skills/suggestions/${userId}`);
    const payload = await suggestionsRes.json();
    const suggestions = payload && Array.isArray(payload.suggestions) ? payload.suggestions : [];

    containers.forEach((c) => (c.innerHTML = ""));
    if (!suggestions.length) {
      containers.forEach((c) =>
        setEmpty(c.id, "No missing skills detected from your recommended jobs.")
      );
      return;
    }

    const html = suggestions
      .map((s) => {
        const links = s.learningLinks || {};
        const youtube = (links.youtube || "").replace(/'/g, "\\'");
        const course = (links.course || "").replace(/'/g, "\\'");
        const docs = (links.docs || "").replace(/'/g, "\\'");
        const importance = s.importance || "Low";
        const byLevel = renderLevelLinks(s.linksByLevel);

        return `
        <div class="card suggestion-card" style="margin-top: 12px;">
          <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;">
            <div>
              <b style="font-size:16px;">${escapeHtml(s.skill)}</b>
              <div class="muted" style="margin-top:2px;font-size:13px;">Improve this skill to increase match quality.</div>
            </div>
            ${importanceBadgeHtml(importance)}
          </div>
          ${byLevel ? `<div class="links-by-level">${byLevel}</div>` : ""}
          <div class="suggestion-actions">
            <button type="button" class="cta-btn" style="width:auto;" onclick="openSkillLink('${youtube}')">Quick: YouTube</button>
            <button type="button" class="cta-btn secondary" style="width:auto;" onclick="openSkillLink('${course}')">Course</button>
            <button type="button" class="cta-btn secondary" style="width:auto;" onclick="openSkillLink('${docs}')">Docs</button>
          </div>
        </div>
      `;
      })
      .join("");

    containers.forEach((c) => (c.innerHTML = html));

    // Make URL opening available to inline onclick handlers (keeps code simple)
    window.openSkillLink = openLink;
  } catch (e) {
    console.error(e);
    showToast("Failed to load learning suggestions", "error");
    getSkillSuggestionContainers().forEach((c) =>
      setEmpty(c.id, "Could not load skill suggestions right now.")
    );
  }
}

async function loadActivity() {
  const savedRes = await apiFetch(`/api/jobs/saved/${userId}`);
  const saved = await savedRes.json();
  const appRes = await apiFetch(`/api/job-seeker/${userId}/applications`);
  const apps = await appRes.json();
  const savedEl = document.getElementById("savedJobsList");
  const appsEl = document.getElementById("applicationsList");
  savedEl.innerHTML = saved.length
    ? saved
        .map((s) => {
          const job = s.job || {};
          const title = escapeHtml(job.title || "Job");
          const company = escapeHtml(job.companyName || "Company");
          const location = escapeHtml(job.location || "Location");
          const remoteText = Boolean(job.remote) ? "Remote" : "On-site";
          return `
            <div class="tracking-card saved-card">
              <div class="tracking-head">
                <b>${title}</b>
                <span class="meta-chip ${Boolean(job.remote) ? "remote" : "onsite"}">${remoteText}</span>
              </div>
              <div class="tracking-subhead">${company} • ${location}</div>
              <div class="tracking-actions">
                <button type="button" class="apply-saved-btn action-btn" data-job="${job.id}">Apply</button>
              </div>
            </div>
          `;
        })
        .join("")
    : `<div class="tracking-empty">No saved jobs yet.</div>`;
  appsEl.innerHTML = apps.length
    ? apps
        .map((a) => {
          const job = a.job || {};
          const title = escapeHtml(job.title || "Job");
          const company = escapeHtml(job.companyName || "Company");
          const location = escapeHtml(job.location || "Location");
          const appliedAt = a.appliedAt ? new Date(a.appliedAt).toLocaleString() : "Date not available";
          const progress = applicationProgressPercent(a.status);
          const rejected = safeUpper(a.status) === "REJECTED";
          return `
            <div class="tracking-card application-card">
              <div class="tracking-head">
                <b>${title}</b>
                ${statusBadgeHtml(a.status)}
              </div>
              <div class="tracking-subhead">${company} • ${location}</div>
              <div class="tracking-time">Applied: ${appliedAt}</div>
              ${applicationTimelineCompact(a.status)}
              <div class="app-progress">
                <div class="app-progress-fill ${rejected ? "rejected" : ""}" style="width:${progress}%"></div>
              </div>
            </div>
          `;
        })
        .join("")
    : `<div class="tracking-empty">No applications yet.</div>`;

  document.querySelectorAll(".apply-saved-btn").forEach((btn) => {
    btn.addEventListener("click", async () => {
      await apiFetch(`/api/job-seeker/${userId}/apply/${btn.dataset.job}`, { method: "POST" });
      showToast("Application submitted", "success");
      await loadActivity();
      await loadDashboard();
    });
  });
}

document.getElementById("profileForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const res = await apiFetch("/api/profile/candidate", {
    method: "POST",
    body: JSON.stringify({
      userId: Number(userId),
      skills: document.getElementById("skills").value,
      preferredRoles: document.getElementById("preferredRoles")?.value || "",
      location: document.getElementById("location")?.value || "",
      remotePreferred: Boolean(document.getElementById("remotePreferred")?.checked),
      experienceYears: Number(document.getElementById("experienceYears").value || 0),
      experienceLevel: "",
      bio: document.getElementById("bio").value
    })
  });
  if (!res.ok) {
    let msg = "Could not save profile";
    try {
      const err = await res.json();
      msg = err.message || msg;
    } catch (_) {
      // ignore
    }
    showToast(msg, "error");
    return;
  }
  showToast("Profile saved", "success");
  await loadDashboard();
  void loadQuizRecommended();
});

document.getElementById("resumeForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const fileInput = document.getElementById("resumeFile");
  const file = fileInput && fileInput.files ? fileInput.files[0] : null;
  if (!file) {
    showToast("Please choose a PDF file first", "error");
    return;
  }

  const maxSize = 10 * 1024 * 1024; // 10MB
  const fileName = (file.name || "").toLowerCase();
  const isPdf = file.type === "application/pdf" || fileName.endsWith(".pdf");

  if (!isPdf) {
    showToast("Only PDF files are allowed", "error");
    return;
  }
  if (file.size > maxSize) {
    showToast("File size exceeds 10MB limit", "error");
    return;
  }

  const formData = new FormData();
  formData.append("file", file);

  const res = await apiFetch(`/api/profile/candidate/${userId}/resume`, {
    method: "POST",
    body: formData
  });

  if (!res.ok) {
    let msg = "Failed to upload resume";
    try {
      const err = await res.json();
      msg = err.message || msg;
    } catch (_) {
      // ignore parse errors
    }
    showToast(msg, "error");
    return;
  }

  let payload = null;
  try {
    payload = await res.json();
  } catch (_) {
    // ignore
  }

  const detectedRaw = payload && Array.isArray(payload.detectedSkills) ? payload.detectedSkills : [];
  const extractedListFromPayload =
    payload && typeof payload.extractedSkills === "string" && payload.extractedSkills.trim()
      ? payload.extractedSkills
          .split(/[,;|]+/)
          .map((s) => s.trim())
          .filter(Boolean)
      : [];
  const detectedForUi = detectedRaw.length ? detectedRaw : extractedListFromPayload;
  const mergedSkills = payload && typeof payload.mergedSkills === "string" ? payload.mergedSkills : "";

  applyResumePayloadToProfileForm(payload);
  renderDetectedSkillsUi(detectedForUi, mergedSkills);

  if (payload && payload.parseStatus === "warning") {
    showToast(payload.parseMessage || "Resume uploaded, but parsing had issues.", "error");
  } else if (payload && (payload.parseStatus === "success" || payload.parseStatus === "ok")) {
    showToast("Resume parsed and profile fields auto-filled", "success");
  } else if (payload && payload.parseMessage) {
    showToast(payload.parseMessage, "success");
  } else {
    showToast("Resume uploaded successfully", "success");
  }

  fileInput.value = "";
  updateResumeUi({
    resumeFileName: payload && (payload.resumeFileName || payload.fileName),
    resumeUploadedAt: payload && payload.resumeUploadedAt,
    extractedSkills: payload && payload.extractedSkills,
    skills: mergedSkills,
    downloadUrl: payload && payload.downloadUrl
  });
  await loadDashboard();
  // Upload response is authoritative for parsed fields; re-apply so the form matches without a manual refresh.
  applyResumePayloadToProfileForm(payload);
  renderDetectedSkillsUi(detectedForUi, mergedSkills);
  void loadQuizRecommended();
});

let quizQuestions = [];
let quizStartTime = Date.now();
let quizSelectedSkill = "java";
let quizSelectedDifficulty = "";
let quizOverviewRows = [];
let quizRecommendedSkills = new Set();

const QUIZ_FALLBACK_DOMAINS = [
  "java", "python", "c", "c++", "javascript", "typescript", "html", "css", "react", "angular", "vue.js",
  "node.js", "express.js", "spring", "spring boot", "hibernate", "jpa", "rest api", "microservices", "git",
  "github", "docker", "kubernetes", "aws basics", "azure basics", "linux", "sql", "mysql", "postgresql",
  "mongodb", "firebase", "dbms", "data structures", "algorithms", "problem solving", "oop", "operating systems",
  "computer networks", "cybersecurity basics", "network security", "ethical hacking basics", "risk analysis",
  "excel", "advanced excel", "power bi", "tableau", "statistics", "probability", "data cleaning", "data analysis",
  "machine learning basics", "deep learning basics", "pandas", "numpy", "scikit-learn", "r programming",
  "figma", "adobe xd", "photoshop", "illustrator", "canva", "ui principles", "ux principles", "wireframing",
  "prototyping", "user research", "design thinking", "typography", "color theory", "visual hierarchy",
  "layout design", "branding", "poster design", "social media design", "thumbnail design", "video editing",
  "premiere pro", "capcut", "after effects basics", "color grading", "storytelling", "script writing",
  "photo editing", "lightroom", "audio editing", "mixing", "mastering", "music composition", "seo",
  "social media marketing", "content marketing", "email marketing", "marketing analytics", "market research",
  "communication", "public speaking", "teamwork", "time management", "leadership", "decision making",
  "conflict resolution", "project management", "teaching skills", "explanation skills", "assessment creation",
  "technical writing", "creative writing", "copywriting", "blog writing", "autocad", "cad basics", "circuit design",
  "electronics basics", "mechanical basics", "civil basics", "problem solving aptitude", "logical reasoning",
  "quantitative aptitude", "verbal ability", "interview skills", "resume writing", "presentation skills",
  "customer handling", "sales skills", "business analysis", "hr basics", "recruitment basics", "obs basics",
  "game design basics", "unity basics", "unreal basics", "singing basics", "voice modulation", "acting basics",
  "dance basics"
];

const QUIZ_INTERMEDIATE_SKILLS = new Set([
  "java", "python", "c++", "javascript", "typescript", "react", "angular", "vue.js", "node.js", "express.js",
  "spring", "spring boot", "hibernate", "jpa", "rest api", "microservices", "git", "docker", "kubernetes",
  "sql", "mysql", "postgresql", "mongodb", "dbms", "data structures", "algorithms", "oop", "linux",
  "network security", "ethical hacking basics", "power bi", "tableau", "data analysis", "pandas", "numpy",
  "scikit-learn", "figma", "adobe xd", "photoshop", "illustrator", "video editing", "premiere pro",
  "marketing analytics", "project management", "business analysis", "autocad", "circuit design"
]);

function quizMetaFallback(slug) {
  const key = String(slug || "").toLowerCase();
  return {
    category: "Skill Assessment",
    difficulty: QUIZ_INTERMEDIATE_SKILLS.has(key) ? "Intermediate" : "Beginner",
    questionCount: 25,
    estimatedMinutes: 18
  };
}

function quizStatusLabel(status) {
  const s = String(status || "").toUpperCase();
  if (s === "COMPLETED") return "Completed";
  if (s === "IN_PROGRESS") return "In Progress";
  return "Not Started";
}

function quizOverviewBySkill(skill) {
  return quizOverviewRows.find((row) => String(row.skill || "").toLowerCase() === String(skill || "").toLowerCase()) || null;
}


function formatDomainLabel(slug) {
  if (!slug || typeof slug !== "string") return "";
  return slug
    .trim()
    .split(/\s+/)
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
    .join(" ");
}

/** Map API display label back to quiz skill slug when `skill` is omitted. */
function resolveQuizSkillSlugFromRow(row) {
  if (!row || typeof row !== "object") return "";
  if (typeof row.skill === "string" && row.skill.trim()) {
    return row.skill.trim();
  }
  if (typeof row.skillName === "string" && row.skillName.trim()) {
    return row.skillName.trim().toLowerCase();
  }
  const label = typeof row.domain === "string" ? row.domain.trim() : "";
  if (!label) return "";
  const lower = label.toLowerCase();
  const hit = QUIZ_FALLBACK_DOMAINS.find(
    (s) => s === lower || formatDomainLabel(s).toLowerCase() === lower
  );
  return hit || "";
}

function getSelectedDomainSkill() {
  const selected = document.querySelector(".quiz-domain-card.is-selected");
  if (selected && selected.dataset.skill) {
    return selected.dataset.skill;
  }
  return quizSelectedSkill;
}

function setQuizDomainSelection(slug) {
  document.querySelectorAll(".quiz-domain-card").forEach((btn) => {
    const on = btn.dataset.skill === slug;
    btn.classList.toggle("is-selected", on);
    btn.setAttribute("aria-checked", on ? "true" : "false");
  });
  quizSelectedSkill = slug;
  const label = document.getElementById("quizSelectedDomainLabel");
  if (label) label.textContent = formatDomainLabel(slug);
}

function setQuizQuestionsLoadedLabel(count) {
  const el = document.getElementById("quizQuestionsLoadedLabel");
  if (!el) return;
  const n = Number.isFinite(Number(count)) ? Math.max(0, Number(count)) : 0;
  el.textContent = `Questions loaded: ${n}`;
}

function showQuizEmptyState(title, text, isError) {
  const wrap = document.getElementById("quizEmptyState");
  const loadEl = document.getElementById("quizLoadingState");
  const container = document.getElementById("quizContainer");
  const submitRow = document.getElementById("quizSubmitRow");
  const meta = document.getElementById("quizSessionMeta");
  const sessionTitle = document.getElementById("quizSessionTitle");
  assessmentInProgress = false;
  currentQuizAttemptId = null;
  stopQuizHeartbeat();
  hideQuizResultPanel();
  if (loadEl) loadEl.classList.add("hidden");
  if (container) {
    container.innerHTML = "";
    container.classList.add("hidden");
  }
  if (wrap) {
    wrap.classList.remove("hidden");
    if (title) {
      const t = wrap.querySelector(".quiz-empty-title");
      const p = wrap.querySelector(".quiz-empty-text");
      if (t) t.textContent = title;
      if (p) p.innerHTML = text || "";
    }
    wrap.classList.toggle("quiz-empty-state--error", !!isError);
  }
  if (submitRow) submitRow.classList.add("hidden");
  if (meta) meta.classList.add("hidden");
  setQuizQuestionsLoadedLabel(0);
  if (sessionTitle && title) {
    if (title === "Could not load questions") {
      sessionTitle.textContent = "Unable to load";
    } else if (title === "No questions available") {
      sessionTitle.textContent = "No questions for this setup";
    } else {
      sessionTitle.textContent = "Unable to load quiz";
    }
  }
}

function resetQuizEmptyStateCopy() {
  const wrap = document.getElementById("quizEmptyState");
  if (!wrap) return;
  const t = wrap.querySelector(".quiz-empty-title");
  const p = wrap.querySelector(".quiz-empty-text");
  if (t) t.textContent = "No assessment loaded yet";
  if (p) {
    p.innerHTML =
      'Choose a domain above and click <strong>Start Assessment</strong> to load questions.';
  }
  wrap.classList.remove("quiz-empty-state--error");
}

function hideQuizResultPanel() {
  const el = document.getElementById("quizResultPanel");
  if (el) el.classList.add("hidden");
}

function formatDurationSeconds(sec) {
  const s = Math.max(0, Number(sec) || 0);
  if (s < 60) return `${s}s`;
  const m = Math.floor(s / 60);
  const r = s % 60;
  return r ? `${m}m ${r}s` : `${m}m`;
}

function quizResultLevelClass(level) {
  const m = String(level || "").toLowerCase();
  if (m === "advanced") return "quiz-result-level--advanced";
  if (m === "intermediate") return "quiz-result-level--intermediate";
  return "quiz-result-level--beginner";
}

/**
 * Populates learning follow-up from POST /api/quiz/submit (topics, links, hints).
 * @param {object} out
 */
function applyQuizLearningFields(out) {
  const wrap = document.getElementById("quizResultLearning");
  if (!wrap) return;
  const topics = Array.isArray(out.topicsToImprove) ? out.topicsToImprove : [];
  const resources = Array.isArray(out.learningResources) ? out.learningResources : [];
  const nextL = typeof out.nextLearningSuggestion === "string" ? out.nextLearningSuggestion.trim() : "";
  const roleFit = typeof out.roleFitDirection === "string" ? out.roleFitDirection.trim() : "";
  const nextA = typeof out.nextAssessmentHint === "string" ? out.nextAssessmentHint.trim() : "";
  const hasContent =
    topics.length > 0 ||
    resources.length > 0 ||
    nextL ||
    roleFit ||
    nextA;
  if (!hasContent) {
    wrap.classList.add("hidden");
    return;
  }
  wrap.classList.remove("hidden");

  const topicsWrap = document.getElementById("quizResultTopicsWrap");
  const ul = document.getElementById("quizResultTopics");
  if (topicsWrap && ul) {
    if (topics.length) {
      topicsWrap.classList.remove("hidden");
      ul.innerHTML = topics.map((t) => `<li>${escapeHtml(String(t))}</li>`).join("");
    } else {
      topicsWrap.classList.add("hidden");
      ul.innerHTML = "";
    }
  }

  const resWrap = document.getElementById("quizResultResources");
  const resSection = document.getElementById("quizResultResourcesSection");
  if (resWrap) {
    if (resources.length) {
      resWrap.innerHTML = resources
        .filter((r) => r && r.url)
        .map((r) => {
          const title = escapeHtml(r.title || "Resource");
          const url = escapeHtml(String(r.url));
          return `<a class="quiz-learning-link" href="${url}" target="_blank" rel="noopener noreferrer">${title}</a>`;
        })
        .join("");
      if (resSection) resSection.classList.remove("hidden");
    } else {
      resWrap.innerHTML = "";
      if (resSection) resSection.classList.add("hidden");
    }
  }

  const nl = document.getElementById("quizResultNextLearning");
  if (nl) {
    nl.textContent = nextL;
    nl.classList.toggle("hidden", !nextL);
  }
  const rf = document.getElementById("quizResultRoleFit");
  if (rf) {
    rf.textContent = roleFit;
    rf.classList.toggle("hidden", !roleFit);
  }
  const na = document.getElementById("quizResultNextAssessment");
  if (na) {
    na.textContent = nextA;
    na.classList.toggle("hidden", !nextA);
  }
}

/**
 * Renders the post-submit result card (domain, stats, score ring, feedback, actions).
 * @param {object} out — API body from POST /api/quiz/submit
 */
function showQuizResultPanel(out) {
  const score = Math.min(100, Math.max(0, Number(out.score) || 0));
  const total = Number(out.totalQuestions) || 0;
  const correct = Number(out.correctAnswers) || 0;
  const perf = out.performanceLevel || out.skillLevel || "Beginner";
  const skillSlug = out.skill || quizSelectedSkill;
  const domainLabel = formatDomainLabel(skillSlug);
  const feedback = typeof out.feedback === "string" ? out.feedback : "";

  const empty = document.getElementById("quizEmptyState");
  const box = document.getElementById("quizContainer");
  const submitRow = document.getElementById("quizSubmitRow");
  const meta = document.getElementById("quizSessionMeta");
  assessmentInProgress = false;
  currentQuizAttemptId = null;
  stopQuizHeartbeat();
  if (empty) empty.classList.add("hidden");
  if (box) {
    box.innerHTML = "";
    box.classList.add("hidden");
  }
  if (submitRow) submitRow.classList.add("hidden");
  if (meta) meta.classList.add("hidden");
  setQuizQuestionsLoadedLabel(Number(out.totalQuestions) || 0);

  const sessionTitle = document.getElementById("quizSessionTitle");
  if (sessionTitle) sessionTitle.textContent = "Results";

  const heading = document.getElementById("quizResultHeading");
  if (heading) heading.textContent = "Assessment complete";

  const domainBadge = document.getElementById("quizResultDomainBadge");
  if (domainBadge) domainBadge.textContent = domainLabel;

  const diffBadge = document.getElementById("quizResultDifficultyBadge");
  if (diffBadge) {
    const d = out.difficulty && String(out.difficulty).trim();
    if (d) {
      diffBadge.textContent = d;
      diffBadge.classList.remove("hidden");
    } else {
      diffBadge.classList.add("hidden");
    }
  }

  const levelBadge = document.getElementById("quizResultLevelBadge");
  if (levelBadge) {
    levelBadge.textContent = perf;
    levelBadge.className = `quiz-result-level-badge ${quizResultLevelClass(perf)}`;
  }

  const pctEl = document.getElementById("quizResultScorePct");
  if (pctEl) pctEl.textContent = `${Math.round(score)}%`;

  const ring = document.getElementById("quizResultScoreRing");
  if (ring) ring.style.setProperty("--score", String(score));

  const correctEl = document.getElementById("quizResultCorrect");
  if (correctEl) correctEl.textContent = String(correct);
  const totalEl = document.getElementById("quizResultTotal");
  if (totalEl) totalEl.textContent = String(total);
  const durEl = document.getElementById("quizResultDuration");
  if (durEl) durEl.textContent = formatDurationSeconds(out.durationSeconds);

  const fill = document.getElementById("quizResultProgressFill");
  if (fill) fill.style.width = `${score}%`;

  const fb = document.getElementById("quizResultFeedback");
  if (fb) fb.textContent = feedback;

  applyQuizLearningFields(out);

  const panel = document.getElementById("quizResultPanel");
  if (panel) {
    panel.classList.remove("hidden");
    panel.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }
}

function tryAnotherDomain() {
  const grid = document.getElementById("quizDomainGrid");
  if (!grid) return;
  const btns = [...grid.querySelectorAll(".quiz-domain-card")];
  if (btns.length < 2) {
    hideQuizResultPanel();
    document.querySelector(".quiz-setup-card")?.scrollIntoView({ behavior: "smooth", block: "nearest" });
    return;
  }
  const cur = quizSelectedSkill;
  const idx = btns.findIndex((b) => b.dataset.skill === cur);
  const next = btns[(idx + 1) % btns.length];
  if (next && next.dataset.skill) {
    setQuizDomainSelection(next.dataset.skill);
  }
  hideQuizResultPanel();
  resetQuizEmptyStateCopy();
  const empty = document.getElementById("quizEmptyState");
  if (empty) {
    empty.classList.remove("hidden");
    const t = empty.querySelector(".quiz-empty-title");
    const p = empty.querySelector(".quiz-empty-text");
    if (t) t.textContent = "Pick a domain";
    if (p) {
      p.innerHTML =
        "You switched domains. Adjust difficulty if you like, then click <strong>Start Assessment</strong>.";
    }
  }
  const st = document.getElementById("quizSessionTitle");
  if (st) st.textContent = "Ready when you are";
  document.querySelector(".quiz-setup-card")?.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

function renderQuizRecommendedSkeleton() {
  return [1, 2, 3]
    .map(
      () => `
    <div class="quiz-rec-skeleton-item" aria-hidden="true">
      <div class="quiz-rec-skeleton-line quiz-rec-skeleton-line--title"></div>
      <div class="quiz-rec-skeleton-line quiz-rec-skeleton-line--short"></div>
      <div class="quiz-rec-skeleton-chips">
        <span class="quiz-rec-skeleton-chip"></span><span class="quiz-rec-skeleton-chip"></span>
      </div>
    </div>`
    )
    .join("");
}

/**
 * Syncs the manual domain grid + label with a slug, then starts the quiz (same fetch as the main Start button).
 * Scrolls the session card into view so loading/questions are visible after auto-start.
 */
function startAssessmentFromRecommendation(skillSlug) {
  if (!skillSlug || typeof skillSlug !== "string") return;
  hideQuizResultPanel();
  setQuizDomainSelection(skillSlug.trim());
  document.querySelector(".quiz-setup-card")?.scrollIntoView({ behavior: "smooth", block: "nearest" });
  void startAssessment({ scrollToSessionAfterStart: true });
}

async function loadQuizRecommended() {
  const host = document.getElementById("quizRecommendedGrid");
  const emptyEl = document.getElementById("quizRecommendedEmpty");
  const loadingEl = document.getElementById("quizRecommendedLoading");
  if (!host) return;
  host.innerHTML = "";
  if (emptyEl) emptyEl.classList.add("hidden");
  if (loadingEl) {
    loadingEl.classList.remove("hidden");
    loadingEl.innerHTML = renderQuizRecommendedSkeleton();
    loadingEl.setAttribute("aria-busy", "true");
  }
  let items = [];
  try {
    const res = await apiFetch(`/api/quiz/recommended/${userId}`);
    if (!res.ok) {
      items = [];
    } else {
      const raw = await res.json();
      items = Array.isArray(raw) ? raw : [];
    }
  } catch (_) {
    items = [];
  }
  if (loadingEl) {
    loadingEl.classList.add("hidden");
    loadingEl.innerHTML = "";
    loadingEl.setAttribute("aria-busy", "false");
  }
  if (!items.length) {
    quizRecommendedSkills = new Set();
    if (emptyEl) emptyEl.classList.remove("hidden");
    return;
  }
  if (emptyEl) emptyEl.classList.add("hidden");
  quizRecommendedSkills = new Set();
  items.forEach((row) => {
    const skill = resolveQuizSkillSlugFromRow(row);
    if (!skill) return;
    quizRecommendedSkills.add(String(skill).toLowerCase());
    const domain = row.domain || formatDomainLabel(skill);
    const reason = row.reason || "";
    const scoreNum = Number(row.score);
    const showScoreBadge = row.score !== null && row.score !== undefined && Number.isFinite(scoreNum);
    const matched =
      Array.isArray(row.matchedSkills) && row.matchedSkills.length
        ? row.matchedSkills.filter((s) => typeof s === "string" && s.trim())
        : [];
    const wrap = document.createElement("div");
    wrap.className = "quiz-recommended-item";
    wrap.setAttribute("role", "listitem");
    const body = document.createElement("div");
    body.className = "quiz-recommended-body";
    const titleRow = document.createElement("div");
    titleRow.className = "quiz-rec-title-row";
    const title = document.createElement("b");
    title.className = "quiz-recommended-title";
    title.textContent = domain;
    titleRow.appendChild(title);
    if (showScoreBadge) {
      const badge = document.createElement("span");
      badge.className = "quiz-rec-score-badge";
      badge.setAttribute("title", "Recommendation strength based on your skills");
      badge.textContent = String(Math.round(scoreNum));
      titleRow.appendChild(badge);
    }
    body.appendChild(titleRow);

    const chipRow = document.createElement("div");
    chipRow.className = "quiz-rec-skills";
    chipRow.setAttribute("aria-label", "Matched skills");
    if (matched.length) {
      matched.forEach((label) => {
        const chip = document.createElement("span");
        chip.className = "quiz-rec-skill-chip";
        chip.textContent = label.trim();
        chipRow.appendChild(chip);
      });
    } else {
      const chip = document.createElement("span");
      chip.className = "quiz-rec-skill-chip quiz-rec-skill-chip--muted";
      chip.textContent = "No direct overlap found yet";
      chipRow.appendChild(chip);
    }
    body.appendChild(chipRow);

    const p = document.createElement("p");
    p.className = "muted quiz-recommended-reason";
    p.textContent = reason;
    body.appendChild(p);
    const startBtn = document.createElement("button");
    startBtn.type = "button";
    startBtn.className = "action-btn quiz-recommended-start";
    startBtn.textContent = "Start Assessment";
    startBtn.setAttribute("aria-label", `Start assessment for ${domain}`);
    startBtn.addEventListener("click", () => startAssessmentFromRecommendation(skill));
    wrap.appendChild(body);
    wrap.appendChild(startBtn);
    host.appendChild(wrap);
  });
  applyQuizSearchFilter();
}

async function loadQuizDomains() {
  const grid = document.getElementById("quizDomainGrid");
  if (!grid) return;
  let list = [];
  try {
    const res = await apiFetch("/api/quiz/domains");
    const raw = await res.json();
    list = Array.isArray(raw) ? raw : [];
  } catch (_) {
    list = [];
  }
  try {
    const overviewRes = await apiFetch(`/api/quiz/overview/${userId}`);
    const overviewRaw = await overviewRes.json();
    quizOverviewRows = Array.isArray(overviewRaw) ? overviewRaw : [];
  } catch (_) {
    quizOverviewRows = [];
  }
  if (!list.length) {
    list = QUIZ_FALLBACK_DOMAINS.slice();
  }
  list = [...new Set([...QUIZ_FALLBACK_DOMAINS, ...list.map((s) => String(s || "").trim().toLowerCase()).filter(Boolean)])];
  grid.dataset.skills = JSON.stringify(list);
  const searchInput = document.getElementById("quizSearchInput");
  if (searchInput && !searchInput.dataset.bound) {
    searchInput.dataset.bound = "1";
    searchInput.addEventListener("input", applyQuizSearchFilter);
  }
  applyQuizSearchFilter();
}

function applyQuizSearchFilter() {
  const grid = document.getElementById("quizDomainGrid");
  if (!grid) return;
  let base = [];
  try {
    base = JSON.parse(grid.dataset.skills || "[]");
  } catch (_) {
    base = [];
  }
  if (!base.length) {
    base = QUIZ_FALLBACK_DOMAINS.slice();
  }
  const query = String(document.getElementById("quizSearchInput")?.value || "").trim().toLowerCase();
  const filtered = base.filter((slug) => {
    const domain = formatDomainLabel(slug).toLowerCase();
    const overview = quizOverviewBySkill(slug);
    const fallback = quizMetaFallback(slug);
    const difficulty = String((overview && overview.difficulty) || fallback.difficulty || "").toLowerCase();
    if (!query) return true;
    return domain.includes(query);
  });
  renderQuizDomainCards(filtered.length ? filtered : base);
}

function renderQuizDomainCards(slugs) {
  const grid = document.getElementById("quizDomainGrid");
  if (!grid) return;
  grid.innerHTML = "";
  slugs.forEach((slug) => {
    const recommended = quizRecommendedSkills.has(String(slug).toLowerCase());

    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "quiz-domain-card";
    btn.dataset.skill = slug;
    btn.setAttribute("role", "radio");
    btn.setAttribute("aria-checked", "false");
    btn.innerHTML = `
      <div class="quiz-domain-head">
        <span class="quiz-domain-title">${escapeHtml(formatDomainLabel(slug))}</span>
        ${recommended ? `<span class="quiz-domain-pill recommended">Recommended</span>` : ""}
      </div>
    `;
    btn.addEventListener("click", () => {
      startAssessmentFromRecommendation(slug);
    });
    grid.appendChild(btn);
  });
  const initial = slugs.includes(quizSelectedSkill) ? quizSelectedSkill : slugs[0];
  if (initial) setQuizDomainSelection(initial);
}

function buildQuizFetchUrl() {
  const diff = quizSelectedDifficulty;
  const params = new URLSearchParams();
  params.set("skill", quizSelectedSkill);
  if (diff) params.set("difficulty", diff);
  return `/api/quiz?${params.toString()}`;
}

function renderQuizQuestions() {
  const box = document.getElementById("quizContainer");
  const empty = document.getElementById("quizEmptyState");
  const loadEl = document.getElementById("quizLoadingState");
  const submitRow = document.getElementById("quizSubmitRow");
  const meta = document.getElementById("quizSessionMeta");
  const sessionTitle = document.getElementById("quizSessionTitle");
  if (!box) return;
  if (loadEl) loadEl.classList.add("hidden");
  box.classList.remove("hidden");
  box.innerHTML = "";

  if (!quizQuestions.length) {
    showQuizEmptyState(
      "No questions available",
      "Try another domain or set difficulty to <strong>All levels</strong>.",
      true
    );
    return;
  }
  assessmentInProgress = true;

  if (empty) empty.classList.add("hidden");
  if (submitRow) submitRow.classList.remove("hidden");
  setQuizQuestionsLoadedLabel(quizQuestions.length);
  if (sessionTitle) {
    sessionTitle.textContent = `${formatDomainLabel(quizSelectedSkill)} · ${quizQuestions.length} question${quizQuestions.length === 1 ? "" : "s"}`;
  }
  if (meta) {
    const diffLabel = quizSelectedDifficulty || "All levels";
    meta.textContent = `${formatDomainLabel(quizSelectedSkill)} · ${diffLabel} · ${quizQuestions.length} questions`;
    meta.classList.remove("hidden");
  }

  const total = quizQuestions.length;
  quizQuestions.forEach((q, idx) => {
    const article = document.createElement("article");
    article.className = "quiz-question-card";
    const metaBits = [q.skill, q.difficulty].filter(Boolean);
    const metaHtml = metaBits.length
      ? `<span class="quiz-q-meta">${metaBits.map((m) => `<span class="meta-chip">${escapeHtml(m)}</span>`).join(" ")}</span>`
      : "";

    const head = document.createElement("div");
    head.className = "quiz-question-head";
    head.innerHTML = `<span class="quiz-q-num">Question ${idx + 1} of ${total}</span>${metaHtml}`;

    const body = document.createElement("p");
    body.className = "quiz-q-text";
    body.textContent = q.question;

    const ul = document.createElement("ul");
    ul.className = "quiz-option-list";
    q.options.forEach((opt, i) => {
      const id = `q_${q.quizId}_${i}`;
      const li = document.createElement("li");
      li.className = "quiz-option-item";
      const label = document.createElement("label");
      label.className = "quiz-option";
      label.setAttribute("for", id);
      const input = document.createElement("input");
      input.type = "radio";
      input.name = `q_${q.quizId}`;
      input.id = id;
      input.value = String(i);
      const span = document.createElement("span");
      span.className = "quiz-option-text";
      span.textContent = opt;
      label.appendChild(input);
      label.appendChild(span);
      li.appendChild(label);
      ul.appendChild(li);
    });

    article.appendChild(head);
    article.appendChild(body);
    article.appendChild(ul);
    box.appendChild(article);
  });
}

/**
 * @param {{ scrollToSessionAfterStart?: boolean }} [options] — when true (e.g. launch from a recommendation card), scroll the session panel into view after the loading state is shown.
 */
async function startAssessment(options = {}) {
  const scrollToSessionAfterStart = options.scrollToSessionAfterStart === true;
  hideQuizResultPanel();
  const diffEl = document.getElementById("quizDifficultySelect");
  quizSelectedSkill = getSelectedDomainSkill();
  quizSelectedDifficulty = (diffEl && diffEl.value) || "";

  const empty = document.getElementById("quizEmptyState");
  const loadEl = document.getElementById("quizLoadingState");
  const box = document.getElementById("quizContainer");
  const submitRow = document.getElementById("quizSubmitRow");
  const startBtn = document.getElementById("startAssessmentBtn");
  const sessionTitle = document.getElementById("quizSessionTitle");
  const meta = document.getElementById("quizSessionMeta");

  if (empty) empty.classList.add("hidden");
  if (box) {
    box.innerHTML = "";
    box.classList.add("hidden");
  }
  if (submitRow) submitRow.classList.add("hidden");
  if (meta) meta.classList.add("hidden");
  setQuizQuestionsLoadedLabel(0);
  if (sessionTitle) sessionTitle.textContent = "Loading…";
  if (loadEl) loadEl.classList.remove("hidden");
  if (scrollToSessionAfterStart) {
    document.querySelector(".quiz-session-card")?.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }
  if (startBtn) {
    startBtn.disabled = true;
    startBtn.dataset.label = startBtn.textContent;
    startBtn.textContent = "Starting…";
  }

  try {
    const attemptRes = await apiFetch("/api/quiz/attempt/start", {
      method: "POST",
      body: JSON.stringify({
        userId: Number(userId),
        skill: quizSelectedSkill,
        difficulty: quizSelectedDifficulty || undefined
      })
    });
    if (!attemptRes.ok) {
      throw new Error("Could not start protected attempt session");
    }
    const attempt = await attemptRes.json();
    currentQuizAttemptId = attempt && attempt.attemptId != null ? Number(attempt.attemptId) : null;
    quizHeartbeatIntervalSeconds =
      attempt && Number.isFinite(Number(attempt.heartbeatIntervalSeconds))
        ? Number(attempt.heartbeatIntervalSeconds)
        : 5;

    const res = await apiFetch(buildQuizFetchUrl());
    quizQuestions = await res.json();
    if (!Array.isArray(quizQuestions)) {
      quizQuestions = [];
    }
    quizStartTime = Date.now();
    resetQuizEmptyStateCopy();
    renderQuizQuestions();
    startQuizHeartbeat();
  } catch (e) {
    quizQuestions = [];
    currentQuizAttemptId = null;
    stopQuizHeartbeat();
    if (loadEl) loadEl.classList.add("hidden");
    showQuizEmptyState(
      "Could not load questions",
      "Check your connection and try again.",
      true
    );
  } finally {
    if (startBtn) {
      startBtn.disabled = false;
      startBtn.textContent = startBtn.dataset.label || "Start Assessment";
    }
  }
}

document.getElementById("submitQuizBtn")?.addEventListener("click", async () => {
  if (!quizQuestions.length) {
    showToast("Load an assessment first.", "error");
    return;
  }
  const answers = quizQuestions.map((q) => {
    const selected = document.querySelector(`input[name="q_${q.quizId}"]:checked`);
    return { quizId: q.quizId, selectedIndex: selected ? Number(selected.value) : -1 };
  });
  const unanswered = answers.filter((a) => a.selectedIndex < 0).length;
  if (unanswered > 0) {
    showToast(`Please answer all questions (${unanswered} left).`, "error");
    return;
  }
  const res = await apiFetch("/api/quiz/submit", {
    method: "POST",
    body: JSON.stringify({
      userId: Number(userId),
      attemptId: currentQuizAttemptId != null ? Number(currentQuizAttemptId) : null,
      skill: quizSelectedSkill,
      difficulty: quizSelectedDifficulty || undefined,
      durationSeconds: Math.round((Date.now() - quizStartTime) / 1000),
      answers
    })
  });
  const out = await res.json();
  const perfLabel = out.performanceLevel || out.skillLevel || "";
  showToast(`Assessment saved — ${Math.round(Number(out.score) || 0)}% (${perfLabel}).`);
  quizQuestions = [];
  assessmentInProgress = false;
  currentQuizAttemptId = null;
  stopQuizHeartbeat();
  showQuizResultPanel(out);
  loadDashboard();
  const learnLine = document.getElementById("latestQuizLearningLine");
  if (learnLine && typeof out.nextLearningSuggestion === "string" && out.nextLearningSuggestion.trim()) {
    const t = out.nextLearningSuggestion.trim();
    learnLine.textContent = t.length > 140 ? `${t.slice(0, 137)}…` : t;
    learnLine.style.display = "block";
  }
});

document.getElementById("quizRetakeBtn")?.addEventListener("click", () => {
  startAssessment();
});

document.getElementById("quizTryAnotherDomainBtn")?.addEventListener("click", () => {
  tryAnotherDomain();
});

setupTabs("dashboard-tab-btn", "tab-panel");
document.getElementById("applyMatchFiltersBtn")?.addEventListener("click", () => {
  loadMatchedJobsWithFilters();
});
document.getElementById("clearMatchFiltersBtn")?.addEventListener("click", () => {
  const ids = ["filterKeyword", "filterCompany", "filterLocation", "filterMinSalary", "filterMaxSalary"];
  ids.forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.value = "";
  });
  const remote = document.getElementById("filterRemote");
  if (remote) remote.value = "";
  loadMatchedJobsWithFilters();
});
document.getElementById("startAssessmentBtn")?.addEventListener("click", () => startAssessment());

document.addEventListener("visibilitychange", () => {
  if (document.visibilityState === "hidden" && assessmentInProgress) {
    void clearAuthSessionAndRedirectForTabSwitch();
  }
});

/**
 * Load domain list for the assessment UI only when the Assessment tab is shown (not on every dashboard load).
 */
function setupAssessmentTabLazyInit() {
  let domainsLoaded = false;
  const run = async () => {
    if (!domainsLoaded) {
      domainsLoaded = true;
      await loadQuizDomains();
    }
    await loadQuizRecommended();
  };

  const tabBtn = document.querySelector('.dashboard-tab-btn[data-target="tabQuiz"]');
  const panel = document.getElementById("tabQuiz");
  if (tabBtn) {
    tabBtn.addEventListener("click", () => {
      void run();
    });
  }
  if (panel && !panel.classList.contains("hidden")) {
    void run();
  }
}

setupAssessmentTabLazyInit();
loadDashboard();
loadActivity();
