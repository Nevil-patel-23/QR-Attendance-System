-- =============================================================
-- V2__update_prn_login.sql
-- Flyway migration — switches login from email to PRN-based
-- =============================================================
-- Login strategy:
--   Admin    → logs in with username + password (no PRN)
--   Teacher  → logs in with PRN (10 digits) + password
--   Student  → logs in with PRN (10 digits) + password
-- =============================================================

-- ─────────────────────────────────────────────────────────────
-- 1. ALTER users table
-- ─────────────────────────────────────────────────────────────

-- Drop email column and its unique constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS uk_users_email;
ALTER TABLE users DROP COLUMN IF EXISTS email;

-- Add PRN column — nullable because Admin has no PRN
ALTER TABLE users ADD COLUMN prn VARCHAR(10);
ALTER TABLE users ADD CONSTRAINT uk_users_prn UNIQUE (prn);
ALTER TABLE users ADD CONSTRAINT chk_users_prn_length
    CHECK (prn IS NULL OR LENGTH(prn) = 10);

-- Add username column — NOT NULL, used by Admin for login
-- For teachers/students this can be auto-set to their PRN
ALTER TABLE users ADD COLUMN username VARCHAR(50) NOT NULL DEFAULT 'temp';
ALTER TABLE users ALTER COLUMN username DROP DEFAULT;
ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username);

-- Drop old role CHECK and re-add (unchanged, just ensuring clean state)
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role
    CHECK (role IN ('ADMIN', 'TEACHER', 'STUDENT'));

-- ─────────────────────────────────────────────────────────────
-- 2. ALTER students table — rename enrollment_no to prn
-- ─────────────────────────────────────────────────────────────

ALTER TABLE students DROP CONSTRAINT IF EXISTS uk_students_enroll;
ALTER TABLE students RENAME COLUMN enrollment_no TO prn;
ALTER TABLE students ALTER COLUMN prn TYPE VARCHAR(10);
ALTER TABLE students ADD CONSTRAINT uk_students_prn UNIQUE (prn);
ALTER TABLE students ADD CONSTRAINT chk_students_prn_length
    CHECK (LENGTH(prn) = 10);

-- ─────────────────────────────────────────────────────────────
-- 3. ALTER teachers table — rename employee_id to prn
-- ─────────────────────────────────────────────────────────────

ALTER TABLE teachers DROP CONSTRAINT IF EXISTS uk_teachers_employee;
ALTER TABLE teachers RENAME COLUMN employee_id TO prn;
ALTER TABLE teachers ALTER COLUMN prn TYPE VARCHAR(10);
ALTER TABLE teachers ADD CONSTRAINT uk_teachers_prn UNIQUE (prn);
ALTER TABLE teachers ADD CONSTRAINT chk_teachers_prn_length
    CHECK (LENGTH(prn) = 10);
