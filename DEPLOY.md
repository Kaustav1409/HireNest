# HireNest — GitHub push & deploy

## Important

- **GitHub Pages** only hosts static HTML. HireNest needs **Java (Spring Boot)** on the server, so use **Render**, **Railway**, or a VPS — not Pages alone.
- The runnable JAR (`target/hirenest-backend-0.0.1-SNAPSHOT.jar`, ~65 MB) is **too large for normal Git**. Do not commit it unless you use **Git LFS**.
- On the server, build with Maven **or** run the JAR you build locally and upload via your host’s file/deploy UI.

---

## Part 1 — Push code to GitHub (local)

### 1. Open terminal in project folder

```powershell
cd "D:\2nd Year 2025-26\4th Semester\21DCS201P - Design Thinking and Methodology\Group Activity\Project\HireNest"
```

### 2. Check remote (already set)

```powershell
git remote -v
```

Expected: `origin` → `https://github.com/Kaustav1409/HireNest.git`

### 3. Build locally (so deploy matches your machine)

```powershell
powershell -ExecutionPolicy Bypass -File compile-enhancements.ps1
```

### 4. Stage only source (`.gitignore` excludes DB, `target/`, logs)

```powershell
git add .gitignore DEPLOY.md README.md compile-enhancements.ps1 start-hirenest.bat start-hirenest.ps1
git add src/main/java src/main/resources
```

### 5. Commit

```powershell
git commit -m "Add platform jobs, enhanced apply, recruiter pipeline UI, and profile completion fixes."
```

### 6. Push

```powershell
git push origin main
```

If GitHub asks for login, use a **Personal Access Token** (not your account password):  
GitHub → Settings → Developer settings → Personal access tokens.

---

## Part 2 — Deploy website (recommended: Render)

1. Go to [https://render.com](https://render.com) and sign in with GitHub.
2. **New → Web Service** → connect repo `Kaustav1409/HireNest`.
3. Settings (example):
   - **Runtime:** Docker or Native (if you add a `pom.xml` later, use Maven build).
   - **Start command (if you deploy the JAR on a machine that has it):**  
     `java -jar target/hirenest-backend-0.0.1-SNAPSHOT.jar`
   - **Port:** `9090` (or set env `PORT` and match `application.properties`).
4. **Environment variables** (required in production):

   | Variable | Example |
   |----------|---------|
   | `APP_JWT_SECRET` | long random string |
   | `SPRING_DATASOURCE_URL` | hosted DB URL (not file H2 for production) |
   | `GOOGLE_CLIENT_ID` | your Google OAuth client ID (optional) |

5. After deploy, open the Render URL (e.g. `https://hirenest-xxxx.onrender.com`).

---

## Part 3 — Run locally after clone (any PC)

```powershell
git clone https://github.com/Kaustav1409/HireNest.git
cd HireNest
# Place hirenest-backend-0.0.1-SNAPSHOT.jar in target/ OR run compile-enhancements.ps1 with jar-libs setup
.\start-hirenest.bat
```

Open: http://localhost:9090/

---

## What not to push

- `data/` — your local database  
- `target/` — build output  
- `uploads/` — user résumés  
- `*.log`, `hs_err_pid*.log`  
- Default JWT secret in production (use env vars)
