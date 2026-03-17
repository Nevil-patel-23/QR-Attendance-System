-- =============================================================
-- V1__create_tables.sql
-- Flyway migration — creates all 14 V1 tables + 1 V2 table (commented out)
-- Tables are created in dependency order: parents before children
-- =============================================================

-- ─────────────────────────────────────────────────────────────
-- DOMAIN 1 — ACADEMIC STRUCTURE
-- ─────────────────────────────────────────────────────────────

-- 1. faculties
CREATE TABLE faculties (
    faculty_id  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(10)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_faculties_name UNIQUE (name),
    CONSTRAINT uk_faculties_code UNIQUE (code)
);

-- 2. courses
CREATE TABLE courses (
    course_id      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    faculty_id     UUID         NOT NULL,
    name           VARCHAR(150) NOT NULL,
    code           VARCHAR(10)  NOT NULL,
    duration_years INT          NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_courses_code UNIQUE (code),
    CONSTRAINT chk_courses_duration CHECK (duration_years BETWEEN 1 AND 6),
    CONSTRAINT fk_courses_faculty FOREIGN KEY (faculty_id)
        REFERENCES faculties (faculty_id)
);

-- 3. semesters
CREATE TABLE semesters (
    semester_id     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id       UUID        NOT NULL,
    semester_number INT         NOT NULL,
    label           VARCHAR(30) NOT NULL,

    CONSTRAINT uk_semesters_course_number UNIQUE (course_id, semester_number),
    CONSTRAINT chk_semesters_number CHECK (semester_number >= 1),
    CONSTRAINT fk_semesters_course FOREIGN KEY (course_id)
        REFERENCES courses (course_id)
);

-- 4. subjects
CREATE TABLE subjects (
    subject_id  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    semester_id UUID         NOT NULL,
    name        VARCHAR(150) NOT NULL,
    code        VARCHAR(20)  NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    credits     INT          NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_subjects_code UNIQUE (code),
    CONSTRAINT chk_subjects_type CHECK (type IN ('COMPULSORY', 'ELECTIVE')),
    CONSTRAINT chk_subjects_credits CHECK (credits BETWEEN 1 AND 6),
    CONSTRAINT fk_subjects_semester FOREIGN KEY (semester_id)
        REFERENCES semesters (semester_id)
);

-- ─────────────────────────────────────────────────────────────
-- DOMAIN 2 — USER MANAGEMENT
-- ─────────────────────────────────────────────────────────────

-- 5. users
CREATE TABLE users (
    user_id       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'TEACHER', 'STUDENT'))
);

-- 6. students
CREATE TABLE students (
    student_id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL,
    enrollment_no       VARCHAR(20) NOT NULL,
    first_name          VARCHAR(80) NOT NULL,
    last_name           VARCHAR(80) NOT NULL,
    phone               VARCHAR(15),
    course_id           UUID        NOT NULL,
    current_semester_id UUID        NOT NULL,
    batch_year          INT         NOT NULL,
    created_at          TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_students_user    UNIQUE (user_id),
    CONSTRAINT uk_students_enroll  UNIQUE (enrollment_no),
    CONSTRAINT uk_students_phone   UNIQUE (phone),
    CONSTRAINT fk_students_user    FOREIGN KEY (user_id)
        REFERENCES users (user_id),
    CONSTRAINT fk_students_course  FOREIGN KEY (course_id)
        REFERENCES courses (course_id),
    CONSTRAINT fk_students_semester FOREIGN KEY (current_semester_id)
        REFERENCES semesters (semester_id)
);

-- 7. teachers
CREATE TABLE teachers (
    teacher_id  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    employee_id VARCHAR(20)  NOT NULL,
    first_name  VARCHAR(80)  NOT NULL,
    last_name   VARCHAR(80)  NOT NULL,
    phone       VARCHAR(15),
    faculty_id  UUID         NOT NULL,
    designation VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_teachers_user     UNIQUE (user_id),
    CONSTRAINT uk_teachers_employee UNIQUE (employee_id),
    CONSTRAINT uk_teachers_phone    UNIQUE (phone),
    CONSTRAINT fk_teachers_user     FOREIGN KEY (user_id)
        REFERENCES users (user_id),
    CONSTRAINT fk_teachers_faculty  FOREIGN KEY (faculty_id)
        REFERENCES faculties (faculty_id)
);

-- ─────────────────────────────────────────────────────────────
-- DOMAIN 3 — ENROLLMENT & ALLOCATION
-- ─────────────────────────────────────────────────────────────

-- 8. student_subject_enrollments
CREATE TABLE student_subject_enrollments (
    enrollment_id UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id    UUID        NOT NULL,
    subject_id    UUID        NOT NULL,
    academic_year VARCHAR(10) NOT NULL,
    enrolled_at   TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_enrollment_student_subject_year
        UNIQUE (student_id, subject_id, academic_year),
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id)
        REFERENCES students (student_id),
    CONSTRAINT fk_enrollment_subject FOREIGN KEY (subject_id)
        REFERENCES subjects (subject_id)
);

-- 9. teacher_subject_allocations
CREATE TABLE teacher_subject_allocations (
    allocation_id UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id    UUID        NOT NULL,
    subject_id    UUID        NOT NULL,
    semester_id   UUID        NOT NULL,
    academic_year VARCHAR(10) NOT NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_allocation_teacher_subject_sem_year
        UNIQUE (teacher_id, subject_id, semester_id, academic_year),
    CONSTRAINT fk_allocation_teacher  FOREIGN KEY (teacher_id)
        REFERENCES teachers (teacher_id),
    CONSTRAINT fk_allocation_subject  FOREIGN KEY (subject_id)
        REFERENCES subjects (subject_id),
    CONSTRAINT fk_allocation_semester FOREIGN KEY (semester_id)
        REFERENCES semesters (semester_id)
);

-- ─────────────────────────────────────────────────────────────
-- DOMAIN 4 — CALENDAR & TIMETABLE
-- ─────────────────────────────────────────────────────────────

-- 10. academic_calendars
CREATE TABLE academic_calendars (
    calendar_id     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id       UUID        NOT NULL,
    academic_year   VARCHAR(10) NOT NULL,
    semester_number INT         NOT NULL,
    start_date      DATE        NOT NULL,
    end_date        DATE        NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_calendar_course_year_sem
        UNIQUE (course_id, academic_year, semester_number),
    CONSTRAINT chk_calendar_sem_number CHECK (semester_number IN (1, 2)),
    CONSTRAINT chk_calendar_dates CHECK (end_date > start_date),
    CONSTRAINT fk_calendar_course FOREIGN KEY (course_id)
        REFERENCES courses (course_id)
);

-- 11. holidays
CREATE TABLE holidays (
    holiday_id UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    date       DATE         NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_holidays_date UNIQUE (date),
    CONSTRAINT chk_holidays_type CHECK (type IN ('NATIONAL', 'UNIVERSITY', 'REGIONAL'))
);

-- 12. timetable_slots
CREATE TABLE timetable_slots (
    slot_id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    allocation_id  UUID        NOT NULL,
    day_of_week    VARCHAR(10) NOT NULL,
    start_time     TIME        NOT NULL,
    end_time       TIME        NOT NULL,
    room           VARCHAR(20) NOT NULL,
    effective_from DATE        NOT NULL,
    effective_to   DATE,

    CONSTRAINT chk_slots_day CHECK (day_of_week IN ('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT')),
    CONSTRAINT chk_slots_time CHECK (end_time > start_time),
    CONSTRAINT fk_slots_allocation FOREIGN KEY (allocation_id)
        REFERENCES teacher_subject_allocations (allocation_id)
);

-- ─────────────────────────────────────────────────────────────
-- DOMAIN 5 — ATTENDANCE
-- ─────────────────────────────────────────────────────────────

-- 13. attendance_sessions
CREATE TABLE attendance_sessions (
    session_id    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    allocation_id UUID        NOT NULL,
    slot_id       UUID,
    teacher_id    UUID        NOT NULL,
    subject_id    UUID        NOT NULL,
    semester_id   UUID        NOT NULL,
    session_date  DATE        NOT NULL,
    qr_token      VARCHAR(36) NOT NULL,
    generated_at  TIMESTAMP   NOT NULL,
    expires_at    TIMESTAMP   NOT NULL,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_sessions_qr_token UNIQUE (qr_token),
    CONSTRAINT fk_sessions_allocation FOREIGN KEY (allocation_id)
        REFERENCES teacher_subject_allocations (allocation_id),
    CONSTRAINT fk_sessions_slot FOREIGN KEY (slot_id)
        REFERENCES timetable_slots (slot_id),
    CONSTRAINT fk_sessions_teacher FOREIGN KEY (teacher_id)
        REFERENCES teachers (teacher_id),
    CONSTRAINT fk_sessions_subject FOREIGN KEY (subject_id)
        REFERENCES subjects (subject_id),
    CONSTRAINT fk_sessions_semester FOREIGN KEY (semester_id)
        REFERENCES semesters (semester_id)
);

-- 14. attendance_records
-- ⚠️  UNIQUE(session_id, student_id) is the MOST CRITICAL CONSTRAINT
--     in the entire system. It prevents duplicate attendance even when
--     50+ students scan simultaneously (race-condition safety net).
CREATE TABLE attendance_records (
    record_id  UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID      NOT NULL,
    student_id UUID      NOT NULL,
    scanned_at TIMESTAMP,
    status     VARCHAR(10) NOT NULL DEFAULT 'PRESENT',

    CONSTRAINT uk_attendance_session_student UNIQUE (session_id, student_id),
    CONSTRAINT chk_records_status CHECK (status IN ('PRESENT', 'ABSENT')),
    CONSTRAINT fk_records_session FOREIGN KEY (session_id)
        REFERENCES attendance_sessions (session_id),
    CONSTRAINT fk_records_student FOREIGN KEY (student_id)
        REFERENCES students (student_id)
);

-- ─────────────────────────────────────────────────────────────
-- PERFORMANCE INDEXES
-- ─────────────────────────────────────────────────────────────

-- Fast lookup of active, unexpired sessions by QR token (scan endpoint)
CREATE INDEX idx_sessions_token_active
    ON attendance_sessions (qr_token, is_active, expires_at);

-- Background job: find expired but still active sessions
CREATE INDEX idx_sessions_active_expiry
    ON attendance_sessions (is_active, expires_at)
    WHERE is_active = TRUE;

-- Attendance reports: records by session
CREATE INDEX idx_records_session
    ON attendance_records (session_id);

-- Attendance reports: records by student
CREATE INDEX idx_records_student
    ON attendance_records (student_id);

-- Student lookup by semester (for absent-record insertion job)
CREATE INDEX idx_students_semester
    ON students (current_semester_id);

-- Timetable: slots by allocation and day
CREATE INDEX idx_slots_allocation_day
    ON timetable_slots (allocation_id, day_of_week);

-- Teacher allocations: lookup by teacher + academic year
CREATE INDEX idx_allocations_teacher_year
    ON teacher_subject_allocations (teacher_id, academic_year);

-- Student enrollments: lookup by student + academic year
CREATE INDEX idx_enrollments_student_year
    ON student_subject_enrollments (student_id, academic_year);

-- ─────────────────────────────────────────────────────────────
-- 15. lecture_adjustments — V2 ONLY — DO NOT ACTIVATE IN V1
-- ─────────────────────────────────────────────────────────────

/*
CREATE TABLE lecture_adjustments (
    adjustment_id  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    slot_id        UUID,
    teacher_id     UUID         NOT NULL,
    original_date  DATE,
    new_date       DATE,
    new_start_time TIME,
    new_end_time   TIME,
    new_room       VARCHAR(20),
    type           VARCHAR(20)  NOT NULL,
    reason         VARCHAR(255),
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_adjustments_type CHECK (type IN ('CANCELLED', 'RESCHEDULED', 'EXTRA')),
    CONSTRAINT fk_adjustments_slot FOREIGN KEY (slot_id)
        REFERENCES timetable_slots (slot_id),
    CONSTRAINT fk_adjustments_teacher FOREIGN KEY (teacher_id)
        REFERENCES teachers (teacher_id)
);
*/
