-- Run against your HireNest database (e.g. hirenest_db) if Hibernate ddl-auto did not widen columns.
-- Verify: SHOW FULL COLUMNS FROM recruiter_profile;

ALTER TABLE recruiter_profile MODIFY company_name VARCHAR(512);
ALTER TABLE recruiter_profile MODIFY company_description LONGTEXT;
