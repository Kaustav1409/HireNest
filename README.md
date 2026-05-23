# HireNest

**An intelligent skill-first hiring platform** that connects job seekers and recruiters through AI-assisted matching, assessments, and actionable career insights.

[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)]()
[![License](https://img.shields.io/badge/License-Academic-blue)]()

---

## Overview

HireNest helps candidates find roles that fit their skills and helps recruiters shortlist faster with data-backed signals—not just keyword filters. The platform combines:

- **Skill-based job matching** with explainable match scores  
- **Platform Partner Jobs** (175+ preloaded roles from the HireNest Partner Network)  
- **Skills assessments** (25-question quizzes with learning recommendations)  
- **Enhanced apply flow** (resume, projects, GitHub/portfolio links, application strength preview)  
- **Recruiter pipeline** (Applied → Shortlisted → Rejected) with strength % and project links  
- **AI chat assistant** for job seeker guidance  

---

## Features

### For Job Seekers

| Feature | Description |
|--------|-------------|
| **Profile & resume** | Upload PDF resume, auto-extract skills, track profile completion % |
| **Recommended jobs** | Matched roles with match %, missing skills, and learning roadmaps |
| **Enhanced apply** | Resume + project links + strength preview before submitting |
| **Assessments** | Domain quizzes to validate skills and improve match quality |
| **Applications tracker** | Saved jobs, application status, and analytics charts |
| **AI insights** | Personalized next steps based on profile, quizzes, and gaps |

### For Recruiters

| Feature | Description |
|--------|-------------|
| **Recruiter profile** | Company name and description |
| **Post custom jobs** | Create your own openings alongside platform jobs |
| **Platform Partner Jobs** | Browse 175+ network roles for analytics and context |
| **Candidate ranking** | Skill overlap, match %, quiz scores, filters |
| **Application pipeline** | Kanban-style Applied / Shortlisted / Rejected with strength & links |
| **Dashboard analytics** | Charts for job performance, skills distribution, hiring funnel |

---

## Tech Stack

| Layer | Technologies |
|-------|----------------|
| **Backend** | Java 17, Spring Boot 3.3, Spring Security, JWT, JPA/Hibernate |
| **Database** | H2 (file-backed, local dev) |
| **Frontend** | HTML, CSS, JavaScript (vanilla), Chart.js |
| **AI / parsing** | Resume PDF parsing, skill extraction, match explanations |
| **Auth** | Email/password + optional Google OAuth |

---

## Getting Started

### Prerequisites

- **Java 17** or newer (`java -version`)
- **Git** (optional, for clone)
- Pre-built JAR at `target/hirenest-backend-0.0.1-SNAPSHOT.jar` **or** run the compile script (see below)

### Run locally (Windows)

```powershell
# 1. Clone the repository
git clone https://github.com/Kaustav1409/HireNest.git
cd HireNest

# 2. Compile enhancements & patch the runnable JAR (if you have target/jar-libs setup)
powershell -ExecutionPolicy Bypass -File compile-enhancements.ps1

# 3. Start the server (creates ./data/ for the database)
.\start-hirenest.bat
```

Open **http://localhost:9090/** in your browser.

### Default entry points

| Page | URL |
|------|-----|
| Home | http://localhost:9090/ |
| Login | http://localhost:9090/login.html |
| Register | http://localhost:9090/register.html |
| Job Seeker Dashboard | http://localhost:9090/dashboard/job-seeker.html |
| Recruiter Dashboard | http://localhost:9090/dashboard/recruiter.html |

Register as **Job Seeker** or **Recruiter**, then complete your profile for better matching.

---

## Project Structure

```
HireNest/
├── src/main/java/com/hirenest/backend/   # Java sources (entities, services, APIs, seeders)
├── src/main/resources/
│   ├── static/                           # Frontend (HTML, CSS, JS)
│   ├── application.properties            # Server & DB config
│   └── db/                               # SQL migrations
├── compile-enhancements.ps1              # Build script for partial source compile
├── start-hirenest.bat                    # Start server on port 9090
├── DEPLOY.md                             # Cloud deploy guide (Render, env vars)
└── data/                                 # Local H2 database (not in Git)
```

---

## Configuration

Key settings in `src/main/resources/application.properties`:

| Property | Description | Default |
|----------|-------------|---------|
| `server.port` | HTTP port | `9090` |
| `spring.datasource.url` | H2 file database path | `./data/hirenest_db` |
| `app.jwt.secret` | JWT signing key | Set via `APP_JWT_SECRET` in production |
| `app.google.client-id` | Google Sign-In | Optional env var |

**Production:** Never use default JWT secrets. Set environment variables on your host (see [DEPLOY.md](DEPLOY.md)).

---

## API Highlights

| Area | Examples |
|------|----------|
| Auth | `POST /api/auth/login`, `POST /api/auth/register` |
| Jobs | `GET /api/jobs/platform`, `GET /api/jobs/recruiter/{id}` |
| Matching | `GET /api/matching/{userId}` |
| Apply | `POST /api/job-seeker/{userId}/apply/{jobId}/enhanced` |
| Profile | `POST /api/profile/candidate/{userId}/resume` |
| Dashboard | `GET /api/dashboard/job-seeker/{userId}` |

---

## Deploy to production

HireNest is a **full-stack Java app**—GitHub Pages cannot run it alone.

For step-by-step cloud deployment (Render, environment variables, database notes), see **[DEPLOY.md](DEPLOY.md)**.

---

## Course & context

Built as a **Design Thinking and Methodology** group project—focused on skill-first hiring, transparent matching, and practical dashboards for both candidates and recruiters.

---

## Contributors

**[@Kaustav1409](https://github.com/Kaustav1409)** — sole contributor
**[@Ashvini-2](https://github.com/Ashvini-2)** — sole contributor

---

## License

This project is for **academic and demonstration purposes**. Contact the authors for other use.
