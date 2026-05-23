const role = localStorage.getItem("hirenest_role");
const userId = localStorage.getItem("hirenest_userId");
const token = localStorage.getItem("hirenest_token");

if (!role || !userId || !token) {
  location.href = "/login.html";
}

const isRecruiter = role === "RECRUITER";
const totalSteps = isRecruiter ? 1 : 3;
let currentStep = 1;

const onboardingMsg = document.getElementById("onboardingMsg");
const progressEl = document.getElementById("onboardingProgress");
const subheadEl = document.getElementById("onboardingSubhead");
const skipLink = document.getElementById("skipOnboardingLink");
const prevStepBtn = document.getElementById("prevStepBtn");
const nextStepBtn = document.getElementById("nextStepBtn");
const finishStepBtn = document.getElementById("finishStepBtn");
const form = document.getElementById("onboardingForm");

function showOnboardingMsg(text, isError = true) {
  onboardingMsg.textContent = text;
  onboardingMsg.classList.remove("hidden", "success", "error");
  onboardingMsg.classList.add(isError ? "error" : "success");
}

function renderProgress() {
  progressEl.innerHTML = "";
  for (let i = 1; i <= totalSteps; i += 1) {
    const pill = document.createElement("span");
    pill.className = `step-pill ${i <= currentStep ? "active" : ""}`;
    pill.textContent = `Step ${i}`;
    progressEl.appendChild(pill);
  }
}

function applyStepUi() {
  const allJobSeekerSteps = document.querySelectorAll('.onboarding-step[data-step]');
  allJobSeekerSteps.forEach((el) => el.classList.add("hidden"));

  if (isRecruiter) {
    document.getElementById("recruiterQuickStep").classList.remove("hidden");
    subheadEl.textContent = "Quick profile setup for recruiters.";
    skipLink.href = "/dashboard/recruiter.html";
    prevStepBtn.classList.add("hidden");
    nextStepBtn.classList.add("hidden");
    finishStepBtn.classList.remove("hidden");
    return;
  }

  subheadEl.textContent = `Job Seeker setup: Step ${currentStep} of ${totalSteps}`;
  skipLink.href = "/dashboard/job-seeker.html";
  const current = document.querySelector(`.onboarding-step[data-step="${currentStep}"]`);
  if (current) current.classList.remove("hidden");
  prevStepBtn.classList.toggle("hidden", currentStep === 1);
  nextStepBtn.classList.toggle("hidden", currentStep === totalSteps);
  finishStepBtn.classList.toggle("hidden", currentStep !== totalSteps);
}

function validateCurrentStep() {
  if (isRecruiter) {
    const companyName = document.getElementById("companyName").value.trim();
    if (!companyName) return "Company name is required.";
    return "";
  }
  if (currentStep === 1) {
    const preferredRoles = document.getElementById("preferredRoles").value.trim();
    const experienceLevel = document.getElementById("experienceLevel").value.trim();
    if (!preferredRoles) return "Preferred roles are required.";
    if (!experienceLevel) return "Please select experience level.";
  }
  if (currentStep === 3) {
    const location = document.getElementById("location").value.trim();
    if (!location) return "Location is required.";
  }
  return "";
}

async function saveRecruiterProfile() {
  const payload = {
    userId: Number(userId),
    companyName: document.getElementById("companyName").value.trim(),
    companyDescription: document.getElementById("companyDescription").value.trim()
  };
  const res = await apiFetch("/api/profile/recruiter", {
    method: "POST",
    body: JSON.stringify(payload)
  });
  if (!res.ok) {
    throw new Error("Failed to save recruiter profile.");
  }
  showOnboardingMsg("Onboarding completed. Redirecting...", false);
  setTimeout(() => (location.href = "/dashboard/recruiter.html"), 800);
}

async function saveJobSeekerProfile() {
  const payload = {
    userId: Number(userId),
    preferredRoles: document.getElementById("preferredRoles").value.trim(),
    experienceLevel: document.getElementById("experienceLevel").value.trim(),
    bio: document.getElementById("bio").value.trim(),
    skills: document.getElementById("skills").value.trim(),
    location: document.getElementById("location").value.trim(),
    remotePreferred: document.getElementById("remotePreferred").checked
  };
  const profileRes = await apiFetch("/api/profile/candidate", {
    method: "POST",
    body: JSON.stringify(payload)
  });
  if (!profileRes.ok) {
    throw new Error("Failed to save candidate profile.");
  }

  const resumeFile = document.getElementById("resumeFile").files?.[0];
  if (resumeFile) {
    const fd = new FormData();
    fd.append("file", resumeFile);
    const resumeRes = await apiFetch(`/api/profile/candidate/${userId}/resume`, {
      method: "POST",
      body: fd
    });
    if (!resumeRes.ok) {
      throw new Error("Profile saved, but resume upload failed.");
    }
  }
  showOnboardingMsg("Onboarding completed. Redirecting...", false);
  setTimeout(() => (location.href = "/dashboard/job-seeker.html"), 850);
}

nextStepBtn.addEventListener("click", () => {
  onboardingMsg.classList.add("hidden");
  const err = validateCurrentStep();
  if (err) {
    showOnboardingMsg(err);
    return;
  }
  if (currentStep < totalSteps) {
    currentStep += 1;
    renderProgress();
    applyStepUi();
  }
});

prevStepBtn.addEventListener("click", () => {
  onboardingMsg.classList.add("hidden");
  if (currentStep > 1) {
    currentStep -= 1;
    renderProgress();
    applyStepUi();
  }
});

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  onboardingMsg.classList.add("hidden");
  const err = validateCurrentStep();
  if (err) {
    showOnboardingMsg(err);
    return;
  }
  try {
    if (isRecruiter) {
      await saveRecruiterProfile();
    } else {
      await saveJobSeekerProfile();
    }
  } catch (ex) {
    showOnboardingMsg(ex.message || "Onboarding failed. Please try again.");
  }
});

renderProgress();
applyStepUi();

