# HireNest

HireNest is a full-stack, skill-first hiring platform built with Spring Boot and a static HTML/CSS/JavaScript frontend. It helps job seekers discover better-fit roles through intelligent matching and helps recruiters manage hiring workflows from a unified dashboard.

## Features

- JWT-based authentication (email/password + Google login flow)
- Candidate and recruiter profile management
- Resume upload and parsing support (PDF)
- Smart job matching with skill-gap insights
- Learning suggestions for missing skills
- Quiz-based assessment and skill profiling
- Recruiter-side candidate/application views
- Role-based dashboards and AI chat endpoint

## Tech Stack

- Backend: Java 17, Spring Boot 3, Spring Web, Spring Security, Spring Data JPA
- Database: MySQL
- Auth: JWT (`jjwt`)
- File Parsing: Apache PDFBox
- Frontend: Static HTML, CSS, JavaScript (served by Spring Boot)
- Build Tool: Maven

## Project Structure

```text
HireNest/
  src/main/java/com/hirenest/backend/   # Java backend (controllers, services, entities, security)
  src/main/resources/static/            # Frontend pages, JS, CSS, images
  src/main/resources/application.properties
  pom.xml
```

## Quick Start (Local)

### 1) Prerequisites

- Java 17+
- Maven 3.9+
- MySQL 8+

### 2) Clone and enter project

```bash
git clone https://github.com/Kaustav1409/HireNest.git
cd HireNest
```

### 3) Configure database and app settings (recommended: environment variables)

This project is already wired to read config from environment variables, so each collaborator can run the app without editing code files.

By default, the app now starts with an in-memory H2 database (good for quick local run and collaborator onboarding).

If you want MySQL (recommended for full realistic data behavior), set these variables before running:

- `SPRING_DATASOURCE_URL` (example: `jdbc:mysql://127.0.0.1:3306/hirenest_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`)
- `SPRING_DATASOURCE_USERNAME` (example: `root`)
- `SPRING_DATASOURCE_PASSWORD` (your local MySQL password)
- `SPRING_DATASOURCE_DRIVER_CLASS_NAME` = `com.mysql.cj.jdbc.Driver`
- `SPRING_JPA_DATABASE_PLATFORM` = `org.hibernate.dialect.MySQLDialect`
- `APP_JWT_SECRET` (use a long random secret key)
- `GOOGLE_CLIENT_ID` or `APP_GOOGLE_CLIENT_ID` (optional, only if Google login is used)

PowerShell (Windows):

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3306/hirenest_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="your_password"
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME="com.mysql.cj.jdbc.Driver"
$env:SPRING_JPA_DATABASE_PLATFORM="org.hibernate.dialect.MySQLDialect"
$env:APP_JWT_SECRET="replace_with_a_long_random_secret_key"
```

macOS/Linux (bash/zsh):

```bash
export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3306/hirenest_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD="your_password"
export SPRING_DATASOURCE_DRIVER_CLASS_NAME="com.mysql.cj.jdbc.Driver"
export SPRING_JPA_DATABASE_PLATFORM="org.hibernate.dialect.MySQLDialect"
export APP_JWT_SECRET="replace_with_a_long_random_secret_key"
```

### 4) Run the app

Using Maven:

```bash
mvn spring-boot:run
```

Or build and run jar:

```bash
mvn clean package
java -jar target/hirenest-backend-0.0.1-SNAPSHOT.jar
```

### 5) Open in browser

- App entry: `http://localhost:8080/`
- Login: `http://localhost:8080/login.html`
- Register: `http://localhost:8080/register.html`

## API Modules

Base routes currently include:

- `/api/auth` - register, login, google auth, password reset
- `/api/profile` - candidate/recruiter profiles + resume endpoints
- `/api/jobs` and `/api/job-seeker` - posting, saving, applying, tracking
- `/api/matching` (under `/api`) - job matching for users
- `/api/skills` - skill suggestions
- `/api/quiz` - quizzes, attempts, results, recommendations
- `/api/recruiter` - recruiter-side candidate and application views
- `/api/dashboard` - job seeker/recruiter dashboard insights
- `/api/ai` - AI chat endpoint

## Deployment Notes

- Use a managed MySQL instance (Railway, Render, PlanetScale, Aiven, etc.).
- Set environment-specific DB/JWT/Google values before deployment.
- Keep sensitive keys out of Git history; prefer environment variables for production.
- If deploying with Docker or cloud buildpacks, ensure Java 17 runtime is selected.

## Recommended Next Improvements

- Add API documentation (OpenAPI/Swagger)
- Add tests for key services and authentication flows
- Add CI checks (build + tests) on pull requests

## License

No license specified yet. Add one if you plan to open-source usage rights clearly.
