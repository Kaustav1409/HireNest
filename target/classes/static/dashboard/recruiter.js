const userId = localStorage.getItem("hirenest_userId");
const role = localStorage.getItem("hirenest_role");
if (!userId || role !== "RECRUITER") location.href = "/login.html";

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

async function ensureRecruiterProfile() {
  const recruiterId = localStorage.getItem("hirenest_recruiterId");
  if (recruiterId) return recruiterId;
  return null;
}

let jobPerformanceChart = null;
let candidateSkillDoughnutChart = null;
let hiringFunnelChart = null;

const recruiterChartState = {
  jobs: [],
  candidatesByJob: {},
  applications: []
};

function escapeHtml(str) {
  if (str == null) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function formatTs(iso) {
  if (!iso) return "—";
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? "—" : d.toLocaleString();
}

function applicationTimelineHtml(a) {
  const applied = formatTs(a.appliedAt);
  const shortl = formatTs(a.shortlistedAt);
  const rej = formatTs(a.rejectedAt);
  const st = safeUpper(a.status);
  return `
    <div class="timeline">
      <div class="timeline-step ${st === "APPLIED" || st === "SHORTLISTED" || st === "REJECTED" ? "done" : ""}">
        <span class="dot"></span>
        <div><b>Applied</b><div class="muted small">${applied}</div></div>
      </div>
      <div class="timeline-step ${st === "SHORTLISTED" || st === "REJECTED" ? "done" : ""}">
        <span class="dot"></span>
        <div><b>Shortlisted</b><div class="muted small">${shortl}</div></div>
      </div>
      <div class="timeline-step ${st === "REJECTED" ? "done" : ""}">
        <span class="dot"></span>
        <div><b>Rejected</b><div class="muted small">${rej}</div></div>
      </div>
    </div>
  `;
}

function statusBadge(status) {
  const st = safeUpper(status);
  if (st === "UNDER_REVIEW") return `<span class="status-badge status-under-review">Under Review</span>`;
  if (st === "INTERVIEW") return `<span class="status-badge status-interview">Interview</span>`;
  if (st === "HIRED") return `<span class="status-badge status-hired">Hired</span>`;
  if (st === "SHORTLISTED") return `<span class="status-badge status-shortlisted">Shortlisted</span>`;
  if (st === "REJECTED") return `<span class="status-badge status-rejected">Rejected</span>`;
  return `<span class="status-badge status-applied">Applied</span>`;
}

function pipelineGroups(applications) {
  const groups = {
    APPLIED: [],
    SHORTLISTED: [],
    REJECTED: []
  };
  applications.forEach((a) => {
    const st = safeUpper(a.status);
    if (groups[st]) {
      groups[st].push(a);
    } else {
      groups.APPLIED.push(a);
    }
  });
  return groups;
}

function candidatesQueryString() {
  const skill = document.getElementById("candFilterSkill")?.value?.trim();
  const minExp = document.getElementById("candFilterMinExp")?.value?.trim();
  const minMatch = document.getElementById("candFilterMinMatch")?.value?.trim();
  const params = new URLSearchParams();
  if (skill) params.set("skill", skill);
  if (minExp !== "" && minExp != null && !Number.isNaN(Number(minExp))) params.set("minExperience", String(Number(minExp)));
  if (minMatch !== "" && minMatch != null && !Number.isNaN(Number(minMatch))) params.set("minMatchScore", String(Number(minMatch)));
  const q = params.toString();
  return q ? `?${q}` : "";
}

async function openResumeWithAuth(urlPath) {
  try {
    const res = await apiFetch(urlPath);
    if (!res.ok) {
      showToast("Could not open resume", "error");
      return;
    }
    const blob = await res.blob();
    const u = URL.createObjectURL(blob);
    window.open(u, "_blank", "noopener,noreferrer");
    setTimeout(() => URL.revokeObjectURL(u), 60_000);
  } catch (e) {
    console.error(e);
    showToast("Could not open resume", "error");
  }
}

function truncateSummary(text, maxLen) {
  const s = text == null ? "" : String(text);
  if (s.length <= maxLen) return s;
  return s.slice(0, maxLen).trim() + "…";
}

function showCandidateModal(c, jobTitle) {
  const modal = document.getElementById("candidateModal");
  const body = document.getElementById("candidateModalBody");
  const title = document.getElementById("candidateModalTitle");
  if (!modal || !body || !title) return;
  title.textContent = c.name || "Candidate";
  const quiz =
    c.latestQuizScorePercent != null && c.latestQuizScorePercent !== undefined
      ? `${c.latestQuizScorePercent}%`
      : "—";
  const resumeBlock =
    c.resumeUploaded && c.resumeDownloadUrl
      ? `<button type="button" class="action-btn resume-open-btn">Open resume (PDF)</button>`
      : `<span class="muted">No resume on file</span>`;
  const summaryBlock =
    c.candidateSummary && String(c.candidateSummary).trim()
      ? `<div class="candidate-summary-box"><div class="candidate-summary-label">AI summary</div><p class="candidate-summary-text">${escapeHtml(c.candidateSummary)}</p></div>`
      : "";
  const missingBlock =
    Array.isArray(c.missingSkills) && c.missingSkills.length
      ? `<p><b>Missing vs job requirements</b><br><span class="muted">${escapeHtml(c.missingSkills.join(", "))}</span></p>`
      : "";
  body.innerHTML = `
    <p class="muted">For job: <b>${escapeHtml(jobTitle || "")}</b></p>
    ${summaryBlock}
    <p><b>Skills</b><br><span class="muted">${escapeHtml(c.skills || "—")}</span></p>
    <p><b>Experience</b> ${c.experienceYears != null ? c.experienceYears + " yr" : "—"}</p>
    <p><b>Match score</b> ${c.matchScore != null ? c.matchScore + "%" : "—"} <span class="muted">(${c.skillsOverlapCount != null ? c.skillsOverlapCount : 0} of ${c.jobRequiredSkillCount != null ? c.jobRequiredSkillCount : "—"} required skills)</span></p>
    ${missingBlock}
    <p><b>Latest quiz score</b> ${quiz}</p>
    <p>${resumeBlock}</p>
  `;
  const btn = body.querySelector(".resume-open-btn");
  if (btn && c.resumeDownloadUrl) {
    btn.addEventListener("click", () => openResumeWithAuth(c.resumeDownloadUrl));
  }
  modal.classList.remove("hidden");
}

function wireCandidateModal() {
  const modal = document.getElementById("candidateModal");
  const closeBtn = document.getElementById("candidateModalClose");
  if (closeBtn) closeBtn.addEventListener("click", () => modal && modal.classList.add("hidden"));
  if (modal) {
    modal.addEventListener("click", (e) => {
      if (e.target === modal) modal.classList.add("hidden");
    });
  }
  const applyF = document.getElementById("candFilterApply");
  const resetF = document.getElementById("candFilterReset");
  if (applyF) applyF.addEventListener("click", () => loadRecruiterData());
  if (resetF) {
    resetF.addEventListener("click", () => {
      const s = document.getElementById("candFilterSkill");
      const e = document.getElementById("candFilterMinExp");
      const m = document.getElementById("candFilterMinMatch");
      if (s) s.value = "";
      if (e) e.value = "";
      if (m) m.value = "";
      loadRecruiterData();
    });
  }
}
wireCandidateModal();

function renderRecruiterAiInsights(ai = {}) {
  const host = document.getElementById("recruiterAiInsights");
  if (!host) return;
  const top = ai.topCandidateForJob || {};
  const topLine =
    top.candidateName && top.jobTitle
      ? `${escapeHtml(top.candidateName)} · ${escapeHtml(top.jobTitle)} — ${top.matchScore != null ? top.matchScore + "%" : "—"} match`
      : "No ranked candidates yet — post jobs and build your pool.";
  const common = ai.mostCommonMissingSkill ? escapeHtml(ai.mostCommonMissingSkill) : "—";
  const cluster = ai.strongestSkillCluster ? escapeHtml(ai.strongestSkillCluster) : "—";
  const bottleneck = ai.hiringBottleneck ? escapeHtml(ai.hiringBottleneck) : "—";
  host.innerHTML = `
    <div class="card ai-insight-card">
      <div class="ai-insight-kicker">Top candidate (by match)</div>
      <p class="ai-insight-body">${topLine}</p>
    </div>
    <div class="card ai-insight-card">
      <div class="ai-insight-kicker">Most common missing skill</div>
      <p class="ai-insight-body">${common}</p>
    </div>
    <div class="card ai-insight-card">
      <div class="ai-insight-kicker">Strongest skill cluster (top match)</div>
      <p class="ai-insight-body">${cluster}</p>
    </div>
    <div class="card ai-insight-card">
      <div class="ai-insight-kicker">Hiring bottleneck</div>
      <p class="ai-insight-body">${bottleneck}</p>
    </div>
  `;
}

function safeUpper(s) {
  return (s || "").toString().trim().toUpperCase();
}

function getJobIdFromApplication(a) {
  if (!a) return null;
  if (a.job && a.job.id != null) return Number(a.job.id);
  if (a.jobId != null) return Number(a.jobId);
  return null;
}

function parseSkillsCsv(csv) {
  if (!csv) return [];
  return String(csv)
    .split(",")
    .map((x) => x.trim().toLowerCase())
    .filter((x) => x.length > 0);
}

function renderRecruiterCharts() {
  const jobFilterEl = document.getElementById("recruiterJobFilter");
  const filterVal = jobFilterEl ? jobFilterEl.value : "ALL";
  const jobs = recruiterChartState.jobs || [];
  const byJob = recruiterChartState.candidatesByJob || {};
  const applications = recruiterChartState.applications || [];

  const jobIds = filterVal === "ALL" ? jobs.map((j) => Number(j.id)) : [Number(filterVal)];
  const jobById = new Map(jobs.map((j) => [Number(j.id), j]));

  // 1) Job performance bar: Views vs Applications
  const barCanvas = document.getElementById("jobPerformanceBar");
  if (barCanvas) {
    const labels = jobIds.map((jid) => (jobById.get(jid)?.title || `Job ${jid}`));
    const appsByJobId = new Map();
    applications.forEach((a) => {
      const jid = getJobIdFromApplication(a);
      if (jid == null) return;
      appsByJobId.set(jid, (appsByJobId.get(jid) || 0) + 1);
    });

    const views = jobIds.map((jid) => (byJob[jid] ? byJob[jid].length : 0));
    const appsCounts = jobIds.map((jid) => appsByJobId.get(jid) || 0);

    if (!jobPerformanceChart) {
      jobPerformanceChart = new Chart(barCanvas, {
        type: "bar",
        data: {
          labels,
          datasets: [
            {
              label: "Views (ranked candidate rows)",
              data: views,
              backgroundColor: "rgba(79, 70, 229, 0.65)"
            },
            {
              label: "Applications",
              data: appsCounts,
              backgroundColor: "rgba(6, 182, 212, 0.65)"
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          animation: { duration: 700, easing: "easeOutQuart" },
          plugins: {
            legend: { position: "bottom" },
            tooltip: { mode: "index", intersect: false }
          },
          scales: {
            y: { beginAtZero: true, grid: { color: "rgba(15, 23, 42, 0.08)" } }
          }
        }
      });
    } else {
      jobPerformanceChart.data.labels = labels;
      jobPerformanceChart.data.datasets[0].data = views;
      jobPerformanceChart.data.datasets[1].data = appsCounts;
      jobPerformanceChart.update({ duration: 650 });
    }
  }

  // 2) Candidate skill distribution (Doughnut)
  const doughnutCanvas = document.getElementById("candidateSkillDoughnut");
  if (doughnutCanvas) {
    const freq = new Map(); // skill -> count

    jobIds.forEach((jid) => {
      const list = byJob[jid] || [];
      list.forEach((c) => {
        const uniqueSkills = new Set(parseSkillsCsv(c.skills));
        uniqueSkills.forEach((skill) => {
          freq.set(skill, (freq.get(skill) || 0) + 1);
        });
      });
    });

    const top = Array.from(freq.entries()).sort((a, b) => b[1] - a[1]).slice(0, 8);
    const labels = top.length ? top.map((x) => x[0]) : ["No skill data"];
    const data = top.length ? top.map((x) => x[1]) : [0];
    const palette = ["#4f46e5", "#06b6d4", "#8b5cf6", "#22c55e", "#f59e0b", "#f97316", "#0ea5e9", "#ef4444"];

    if (!candidateSkillDoughnutChart) {
      candidateSkillDoughnutChart = new Chart(doughnutCanvas, {
        type: "doughnut",
        data: {
          labels,
          datasets: [
            {
              data,
              backgroundColor: labels.map((_, i) => palette[i % palette.length]),
              borderColor: "rgba(255,255,255,0.9)",
              borderWidth: 2
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          animation: { duration: 700, easing: "easeOutQuart" },
          plugins: {
            legend: { position: "bottom" },
            tooltip: { callbacks: { label: (ctx) => ` ${ctx.label}: ${ctx.raw}` } }
          }
        }
      });
    } else {
      candidateSkillDoughnutChart.data.labels = labels;
      candidateSkillDoughnutChart.data.datasets[0].data = data;
      candidateSkillDoughnutChart.data.datasets[0].backgroundColor = labels.map((_, i) => palette[i % palette.length]);
      candidateSkillDoughnutChart.update({ duration: 650 });
    }
  }

  // 3) Hiring funnel (bar)
  const funnelCanvas = document.getElementById("hiringFunnelBar");
  if (funnelCanvas) {
    const statuses = ["APPLIED", "SHORTLISTED", "REJECTED"];
    const appsFiltered = filterVal === "ALL"
      ? applications
      : applications.filter((a) => getJobIdFromApplication(a) === Number(filterVal));

    const counts = statuses.map((st) => appsFiltered.filter((a) => safeUpper(a.status) === st).length);
    const labels = ["Applied", "Shortlisted", "Rejected"];
    const colors = ["rgba(202, 138, 4, 0.85)", "rgba(37, 99, 235, 0.85)", "rgba(220, 38, 38, 0.85)"];

    if (!hiringFunnelChart) {
      hiringFunnelChart = new Chart(funnelCanvas, {
        type: "bar",
        data: {
          labels,
          datasets: [
            {
              label: "Applicants",
              data: counts,
              backgroundColor: colors
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          indexAxis: "y",
          animation: { duration: 700, easing: "easeOutQuart" },
          plugins: {
            legend: { display: false },
            tooltip: { callbacks: { label: (ctx) => ` ${ctx.raw}` } }
          },
          scales: {
            x: { beginAtZero: true, grid: { color: "rgba(15, 23, 42, 0.08)" } },
            y: { grid: { display: false } }
          }
        }
      });
    } else {
      hiringFunnelChart.data.labels = labels;
      hiringFunnelChart.data.datasets[0].data = counts;
      hiringFunnelChart.update({ duration: 650 });
    }
  }
}

function setStatSkeleton(id) {
  const el = document.getElementById(id);
  if (!el) return;
  el.innerHTML = `<div class="skeleton skeleton-line long"></div>`;
}

/** Prefer Spring REST error `message`, then RFC7807 `detail`, then `error`. */
function pickApiErrorMessage(body) {
  if (!body || typeof body !== "object") return null;
  if (typeof body.message === "string" && body.message.trim()) return body.message.trim();
  if (typeof body.detail === "string" && body.detail.trim()) return body.detail.trim();
  if (typeof body.error === "string" && body.error.trim()) return body.error.trim();
  return null;
}

const COMPANY_DESCRIPTION_MAX_WORDS = 5000;

function countWords(text) {
  const t = String(text ?? "").trim();
  if (!t) return 0;
  return t.split(/\s+/).length;
}

function companyDescriptionWordCount() {
  return countWords(document.getElementById("companyDescription")?.value);
}

function syncCompanyDescriptionWordLimitUi() {
  const ta = document.getElementById("companyDescription");
  const counterEl = document.getElementById("companyDescriptionWordCount");
  const errEl = document.getElementById("companyDescriptionWordLimitError");
  const form = document.getElementById("recruiterProfileForm");
  const submitBtn = form?.querySelector('button[type="submit"]');
  if (!ta) return;

  const n = companyDescriptionWordCount();
  const over = n > COMPANY_DESCRIPTION_MAX_WORDS;

  if (counterEl) {
    counterEl.textContent = `${n} / ${COMPANY_DESCRIPTION_MAX_WORDS} words`;
  }
  ta.classList.toggle("company-description-over-limit", over);
  if (errEl) {
    errEl.hidden = !over;
  }
  if (submitBtn && submitBtn.textContent !== "Saving...") {
    submitBtn.disabled = over;
  }
}

const companyDescriptionEl = document.getElementById("companyDescription");
if (companyDescriptionEl) {
  companyDescriptionEl.addEventListener("input", syncCompanyDescriptionWordLimitUi);
  syncCompanyDescriptionWordLimitUi();
}

document.getElementById("recruiterProfileForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  if (companyDescriptionWordCount() > COMPANY_DESCRIPTION_MAX_WORDS) {
    showToast("Word limit exceeded (Max 5000 words)", "error");
    syncCompanyDescriptionWordLimitUi();
    return;
  }
  const form = e.target;
  const submitBtn = form.querySelector('button[type="submit"]');
  const originalBtnText = submitBtn ? submitBtn.textContent : "";
  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.textContent = "Saving...";
  }
  try {
    const res = await apiFetch("/api/profile/recruiter", {
      method: "POST",
      body: JSON.stringify({
        userId: Number(userId),
        companyName: document.getElementById("companyName").value,
        companyDescription: document.getElementById("companyDescription").value
      })
    });
    const rawText = await res.text();
    let body = null;
    try {
      body = rawText ? JSON.parse(rawText) : null;
    } catch (_) {
      body = null;
    }
    if (!res.ok) {
      let msg = pickApiErrorMessage(body);
      if (!msg && rawText && rawText.trim()) {
        const t = rawText.trim();
        msg = t.length <= 900 ? t : `${t.slice(0, 900)}…`;
      }
      console.error("Recruiter profile save failed", res.status, body ?? rawText);
      showToast(msg || "Failed to save recruiter profile", "error");
      return;
    }
    if (body && body.user && body.user.id != null) {
      localStorage.setItem("hirenest_recruiterId", String(body.user.id));
    }
    showToast("Recruiter profile saved successfully", "success");
    await loadRecruiterData();
  } catch (err) {
    console.error(err);
    showToast("Failed to save recruiter profile", "error");
  } finally {
    if (submitBtn) {
      submitBtn.textContent = originalBtnText || "Save Recruiter Profile";
    }
    syncCompanyDescriptionWordLimitUi();
  }
});

document.getElementById("jobForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  await apiFetch("/api/jobs", {
    method: "POST",
    body: JSON.stringify({
      recruiterId: Number(userId),
      title: document.getElementById("title").value,
      description: document.getElementById("description").value,
      requiredSkills: document.getElementById("requiredSkills").value,
      location: document.getElementById("location").value,
      salaryMin: Number(document.getElementById("salaryMin").value || 0),
      salaryMax: Number(document.getElementById("salaryMax").value || 0)
    })
  });
  showToast("Job posted");
  await loadRecruiterData();
});

async function loadRecruiterData() {
  [
    "jobsPostedCount",
    "candidateRowsCount",
    "avgSkillsPerJob",
    "appliedCount",
    "shortlistedCount",
    "rejectedCount",
    "recTotalJobs",
    "recTotalApplicants",
    "recShortlistedHighlight",
    "recRejectedHighlight",
    "recActiveOpenings"
  ].forEach(setStatSkeleton);
  setLoading("jobs", "Fetching jobs...");
  setLoading("candidates", "Fetching candidates...");
  setLoading("applicationsPipeline", "Fetching applications...");
  const jobsRes = await apiFetch(`/api/jobs/recruiter/${userId}`);
  const jobs = await jobsRes.json();
  const insightsRes = await apiFetch(`/api/dashboard/recruiter/${userId}/insights`);
  const insights = await insightsRes.json();
  renderRecruiterAiInsights(insights.aiInsights || {});
  document.getElementById("jobsPostedCount").textContent = jobs.length;
  document.getElementById("avgSkillsPerJob").textContent = insights.avgSkillsPerJob || 0;
  document.getElementById("appliedCount").textContent = insights.appliedCount || 0;
  document.getElementById("shortlistedCount").textContent = insights.shortlistedCount || 0;
  document.getElementById("rejectedCount").textContent = insights.rejectedCount || 0;
  const elTJ = document.getElementById("recTotalJobs");
  const elTA = document.getElementById("recTotalApplicants");
  const elSH = document.getElementById("recShortlistedHighlight");
  const elRJ = document.getElementById("recRejectedHighlight");
  const elAO = document.getElementById("recActiveOpenings");
  if (elTJ) elTJ.textContent = insights.totalJobs != null ? insights.totalJobs : jobs.length;
  if (elTA) elTA.textContent = insights.totalApplicants != null ? insights.totalApplicants : 0;
  if (elSH) elSH.textContent = insights.shortlistedCandidates != null ? insights.shortlistedCandidates : insights.shortlistedCount || 0;
  if (elRJ) elRJ.textContent = insights.rejectedCount != null ? insights.rejectedCount : 0;
  if (elAO) elAO.textContent = insights.openPositions != null ? insights.openPositions : jobs.length;
  const jobsEl = document.getElementById("jobs");
  jobsEl.innerHTML = jobs.map((j) => `<div class="card"><b>${j.title}</b><br>${j.requiredSkills}</div>`).join("");
  if (!jobs.length) {
    setEmpty("jobs", "No hiring activity yet. Post your first job to start receiving candidates.");
  }

  const candRes = await apiFetch(`/api/recruiter/candidates/${userId}${candidatesQueryString()}`);
  const byJob = await candRes.json();
  const totalRows = Object.values(byJob).reduce((acc, arr) => acc + arr.length, 0);
  document.getElementById("candidateRowsCount").textContent = totalRows;
  const candidatesEl = document.getElementById("candidates");
  candidatesEl.innerHTML = "";
  const jobTitleById = new Map(jobs.map((j) => [String(j.id), j.title]));
  if (!Object.keys(byJob).length) {
    const msg = !jobs.length
      ? "Post a job first to see ranked candidates."
      : "No candidates match these filters. Try resetting filters.";
    setEmpty("candidates", msg);
  } else {
    Object.keys(byJob).forEach((jobId) => {
      const list = byJob[jobId];
      const block = document.createElement("div");
      block.className = "card candidate-job-block";
      const jt = jobTitleById.get(String(jobId)) || `Job ${jobId}`;
      block.innerHTML = `<h4 class="candidate-job-title">${escapeHtml(jt)}</h4>
        <div class="muted small" style="margin-bottom:10px;">Sorted by match score, then skill overlap.</div>`;
      const rows = document.createElement("div");
      rows.className = "candidate-rows";
      list.forEach((c, idx) => {
        const row = document.createElement("button");
        row.type = "button";
        row.className = "candidate-row-btn";
        const quiz =
          c.latestQuizScorePercent != null && c.latestQuizScorePercent !== undefined
            ? `${c.latestQuizScorePercent}%`
            : "—";
        const sumLine =
          c.candidateSummary && String(c.candidateSummary).trim()
            ? `<div class="candidate-ai-summary">${escapeHtml(truncateSummary(c.candidateSummary, 160))}</div>`
            : "";
        row.innerHTML = `
          <span class="cand-rank">#${idx + 1}</span>
          <div class="cand-row-main">
            <div class="cand-line-top">
              <span class="cand-name">${escapeHtml(c.name)}</span>
              <span class="cand-meta"><b>${c.matchScore != null ? c.matchScore : 0}%</b> match · ${c.skillsOverlapCount != null ? c.skillsOverlapCount : 0} of ${c.jobRequiredSkillCount != null ? c.jobRequiredSkillCount : "—"} skills · Quiz ${quiz}</span>
            </div>
            ${sumLine}
          </div>
        `;
        row.addEventListener("click", () => showCandidateModal(c, jt));
        rows.appendChild(row);
      });
      block.appendChild(rows);
      candidatesEl.appendChild(block);
    });
  }

  const appsRes = await apiFetch(`/api/recruiter/applications/${userId}`);
  const applications = await appsRes.json();

  // Cache data for charts and filters
  recruiterChartState.jobs = jobs;
  recruiterChartState.candidatesByJob = byJob;
  recruiterChartState.applications = applications;

  // Update job filter options
  const jobFilterEl = document.getElementById("recruiterJobFilter");
  if (jobFilterEl) {
    jobFilterEl.innerHTML = `<option value="ALL" selected>All Jobs</option>`;
    jobs.forEach((j) => {
      jobFilterEl.insertAdjacentHTML(
        "beforeend",
        `<option value="${j.id}">${j.title}</option>`
      );
    });
    jobFilterEl.onchange = () => renderRecruiterCharts();
  }
  const pipeEl = document.getElementById("applicationsPipeline");
  pipeEl.innerHTML = "";
  if (!applications.length) {
    setEmpty("applicationsPipeline", "No applications yet.");
    renderRecruiterCharts();
    return;
  }
  const grouped = pipelineGroups(applications);
  const columns = [
    { key: "APPLIED", title: "Applied", empty: "No candidates in Applied stage." },
    { key: "SHORTLISTED", title: "Shortlisted", empty: "No candidates shortlisted yet." },
    { key: "REJECTED", title: "Rejected", empty: "No rejected candidates." }
  ];
  pipeEl.className = "pipeline-board";
  columns.forEach((col) => {
    const colWrap = document.createElement("div");
    colWrap.className = "pipeline-column";
    const items = grouped[col.key] || [];
    colWrap.innerHTML = `
      <div class="pipeline-column-head">
        <b>${col.title}</b>
        <span class="meta-chip">${items.length}</span>
      </div>
      <div class="pipeline-column-body"></div>
    `;
    const body = colWrap.querySelector(".pipeline-column-body");
    if (!items.length) {
      body.innerHTML = `<div class="tracking-empty">${col.empty}</div>`;
    } else {
      items.forEach((a) => {
        const card = document.createElement("div");
        const statusKey = safeUpper(a.status).toLowerCase();
        card.className = `card pipeline-card pipeline-card-${statusKey}`;
        const jobTitle = (a.job && a.job.title) || "Job";
        const candName = (a.user && a.user.fullName) || "Candidate";
        const match = a.matchScore != null ? `${a.matchScore}%` : "—";
        const hasResume = Boolean(a.resumeDownloadUrl);
        const appliedAt = formatTs(a.appliedAt);
        card.innerHTML = `
          <div class="pipeline-head">
            <div>
              <b>${escapeHtml(candName)}</b>
              <div class="tracking-subhead">${escapeHtml(jobTitle)}</div>
            </div>
            ${statusBadge(a.status)}
          </div>
          <div class="job-meta-row" style="margin-top:6px;">
            <span class="meta-chip">Match ${match}</span>
            <span class="meta-chip ${hasResume ? "remote" : "onsite"}">${hasResume ? "Resume Available" : "No Resume"}</span>
            <span class="meta-chip">Applied ${appliedAt}</span>
          </div>
          ${applicationTimelineHtml(a)}
          <div class="pipeline-actions">
            <button type="button" class="status-btn action-btn" data-id="${a.id}" data-status="SHORTLISTED">Shortlist</button>
            <button type="button" class="status-btn action-btn" data-id="${a.id}" data-status="REJECTED">Reject</button>
          </div>
        `;
        body.appendChild(card);
      });
    }
    pipeEl.appendChild(colWrap);
  });
  document.querySelectorAll(".status-btn").forEach((btn) => {
    btn.addEventListener("click", async () => {
      await apiFetch(`/api/recruiter/applications/${btn.dataset.id}/status?status=${btn.dataset.status}`, {
        method: "PATCH"
      });
      showToast(`Status updated to ${btn.dataset.status}`);
      loadRecruiterData();
    });
  });

  // Render charts after data is loaded
  renderRecruiterCharts();
}

setupTabs("dashboard-tab-btn", "tab-panel");
ensureRecruiterProfile().then(loadRecruiterData);
