-- Widen job text columns (H2 / MySQL-style). Run if job posts fail with "Value too long for column DESCRIPTION".
-- H2 file DB default path: ./data/hirenest_db

ALTER TABLE job ALTER COLUMN description SET DATA TYPE CLOB;
ALTER TABLE job ALTER COLUMN required_skills SET DATA TYPE VARCHAR(2000);
ALTER TABLE job ALTER COLUMN title SET DATA TYPE VARCHAR(512);
ALTER TABLE job ALTER COLUMN company_name SET DATA TYPE VARCHAR(512);
ALTER TABLE job ALTER COLUMN location SET DATA TYPE VARCHAR(512);
