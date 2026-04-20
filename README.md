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

### 3) Configure database and app settings

Update `src/main/resources/application.properties` with your local values.

At minimum, verify:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `app.jwt.secret`
- `google.client.id` / `app.google.client-id` (if using Google login)

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

- Add `.gitignore` to exclude generated artifacts (`target/`) and local uploads
- Move secrets from `application.properties` to environment variables
- Add API documentation (OpenAPI/Swagger)
- Add tests for key services and authentication flows

## License

No license specified yet. Add one if you plan to open-source usage rights clearly.
