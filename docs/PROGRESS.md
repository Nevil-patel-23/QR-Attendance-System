# Progress Tracker

> This file is updated after every completed task. Paste this file into the Antigravity agent at the start of every session so it knows exactly what is done and what to build next.

---

## Current Status

**Phase: Step 2 — Database Migrations (complete, ready for Step 3)**

- Login is PRN + password for ALL users including Admin
- `User.java` is clean and verified correct
- V3 migration (`V3__remove_username.sql`) will clean `username` column from Neon

---

## Completed ✅

### Planning & Design
- [x] Project idea and scope defined
- [x] Tech stack decided (Java 21, Spring Boot 3, Vaadin 24, Spring Security 6 + JWT, Spring Data JPA, PostgreSQL 16, ZXing, Maven, Lombok, Apache POI)
- [x] System architecture designed (3-tier: Vaadin UI → Spring Boot → PostgreSQL)
- [x] All 15 database tables designed with full column definitions, constraints, and sample data
- [x] QR scan 5-check validation chain designed
- [x] Proxy prevention strategy fully designed
- [x] Semester numbering corrected to absolute (1–8) instead of year+semester
- [x] All 26 screens mapped by role (Admin 13, Teacher 6, Student 5, Shared 2)
- [x] Folder structure defined
- [x] Layer rules and coding standards defined
- [x] Documentation files created (this file + 5 others in docs/)
- [x] V2 features identified and deferred (lecture_adjustments, geolocation, elective window)

### Step 0 — Project Setup ✅
- [x] Add all dependencies to pom.xml (Spring Boot 3.4.3, Vaadin 24.6.5, all libs)
- [x] Create QrAttendanceApplication.java entry point (with dotenv loading + @EnableScheduling)
- [x] Create application.properties with all environment variable references
- [x] Create application-local.properties (empty, for local overrides)
- [x] Create AppConfig.java (centralised config — JWT, QR, attendance properties)
- [x] Create PasswordEncoderConfig.java (BCrypt bean in standalone config)
- [x] ~~Set up folder structure~~ — SKIPPED: creating folders as we build each file
- [x] ~~Create .env and .gitignore~~ — SKIPPED: already existed
- [x] ~~Create PostgreSQL database~~ — SKIPPED: using Neon.tech cloud PostgreSQL, database already exists
- [x] Verify app starts without errors on port 8080 ✅

### Step 1 — Models (14 entities + 6 enums) ✅
- [x] Enum files (6): Role, SubjectType, AttendanceStatus, HolidayType, DayOfWeek, AdjustmentType
- [x] Domain 1 — Academic Structure: Faculty, Course, Semester, Subject
- [x] Domain 2 — User Management: User, Student, Teacher
- [x] Domain 3 — Enrollment & Allocation: StudentSubjectEnrollment, TeacherSubjectAllocation
- [x] Domain 4 — Calendar & Timetable: AcademicCalendar, Holiday, TimetableSlot
- [x] Domain 5 — Attendance: AttendanceSession, AttendanceRecord

### Step 2 — Database Migrations ✅
- [x] V1__create_tables.sql — all 14 V1 tables created in dependency order
- [x] All constraints verified: PKs, FKs, UNIQUE, CHECK
- [x] 9 performance indexes created
- [x] lecture_adjustments (V2) included but commented out
- [x] Flyway migration ran successfully — all 14 tables live in Neon database ✅
- [x] V2__update_prn_login.sql — switched login from email to PRN-based, renamed enrollment_no/employee_id to prn

---

## In Progress 🔄

*Nothing in progress — session ended*

---

## Next Up ⏭️

### Step 3 — Repository interfaces (14 files, one per model)

---

## Not Started 📋
- [ ] Step 3 — Repository interfaces
- [ ] Step 4 — Security (JWT + Spring Security)
- [ ] Step 5 — DTOs
- [ ] Step 6 — Exception handling
- [ ] Step 7 — Service layer (business logic)
- [ ] Step 8 — Controllers (API endpoints)
- [ ] Step 9 — Vaadin UI (26 screens)
- [ ] Step 10 — Testing

---

## V2 Backlog 🔮

- [ ] Lecture adjustment screen (T6)
- [ ] lecture_adjustments table activation
- [ ] Geolocation scan validation
- [ ] Student elective self-selection window
- [ ] Push notifications for lecture changes

---

## Session Log

| Date | What Was Done |
|---|---|
| March 2025 | Full planning phase completed — database design, screens, architecture, folder structure |
| 16 Mar 2025 | Step 0 completed — pom.xml, entry point, properties, config classes, app verified on port 8080 |
| 16 Mar 2025 | Step 1 completed — 6 enums + 14 entity classes across all 5 domains |
| 16 Mar 2025 | Step 2 completed — V1__create_tables.sql with 14 tables, constraints, indexes; all tables live in Neon |
| 17 Mar 2025 | PRN migration — login switched from email to PRN-based; V2__update_prn_login.sql created; User/Student/Teacher models + all docs updated |

---

## How to Use This File

**At the start of every Antigravity agent session, paste:**
1. This PROGRESS.md file
2. The relevant section from IMPLEMENTATION_PLAN.md for what you are building today
3. The relevant table definitions from DATABASE_DESIGN.md if building DB-related features
4. The screen definition from SCREENS.md if building a UI screen

**After completing a task:**
1. Move it from "Not Started" to "Completed"
2. Update "Current Status" at the top
3. Update "Next Up" to the next task
4. Add a row to Session Log

---

## Quick Reference

### Key constraint to always remember
`UNIQUE(session_id, student_id)` on `attendance_records` — this is the race-condition safety net

### Semester numbering rule
Absolute numbers 1–8 (for 4-year course). Year = `CEIL(semester_number / 2)`. Never store year separately.

### Subject code format
`MCA2309` = course(MCA) + year(2) + semester(3) + subject_number(09)

### QR expiry default
60 seconds — configured in `.env` as `QR_EXPIRY_SECONDS=60`

### Package base
`com.university.attendance`
### API base path
`/api/v1/`

### Database
Using Neon.tech cloud PostgreSQL — no local PostgreSQL needed. Connection via DB_URL in .env file with ?sslmode=require

### Login
Admin uses PRN + password. Teachers and students use PRN (10 digits) + password. All users must have a PRN.
