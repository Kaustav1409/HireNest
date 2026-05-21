-- Clears all application data but keeps tables/columns (schema) intact.
-- Safe for H2 and MySQL-compatible databases used by HireNest.
-- Run from H2 Console (http://localhost:8080/h2-console) or your SQL client.

SET REFERENTIAL_INTEGRITY FALSE;

DELETE FROM saved_job;
DELETE FROM job_application;
DELETE FROM quiz_attempt;
DELETE FROM feedback;
DELETE FROM password_reset_token;
DELETE FROM candidate_profile;
DELETE FROM recruiter_profile;
DELETE FROM job;
DELETE FROM quiz_question;
DELETE FROM users;

SET REFERENTIAL_INTEGRITY TRUE;

-- Optional: reset H2 identity columns after delete
ALTER TABLE users ALTER COLUMN id RESTART WITH 1;
ALTER TABLE job ALTER COLUMN id RESTART WITH 1;
ALTER TABLE job_application ALTER COLUMN id RESTART WITH 1;
ALTER TABLE candidate_profile ALTER COLUMN id RESTART WITH 1;
ALTER TABLE recruiter_profile ALTER COLUMN id RESTART WITH 1;
ALTER TABLE saved_job ALTER COLUMN id RESTART WITH 1;
ALTER TABLE quiz_attempt ALTER COLUMN id RESTART WITH 1;
ALTER TABLE quiz_question ALTER COLUMN id RESTART WITH 1;
ALTER TABLE feedback ALTER COLUMN id RESTART WITH 1;
ALTER TABLE password_reset_token ALTER COLUMN id RESTART WITH 1;
