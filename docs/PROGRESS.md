# Progress Tracker

> This file is updated after every completed task. Paste this file into the Antigravity agent at the start of every session so it knows exactly what is done and what to build next.

---

## Current Status

Phase: Slice 6 complete, ready for Slice 7

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

### Login Vertical Slice (Backend & UI) ✅
- [x] UserRepository with PRN queries
- [x] LoginRequest, AuthResponse, ErrorResponse DTOs
- [x] GlobalExceptionHandler and custom exceptions
- [x] JWT utility, filter, and stateless SecurityConfig
- [x] AuthService for PRN/BCrypt verification
- [x] AuthController exposing POST /api/v1/auth/login
- [x] SecurityConfig updated to permit Vaadin UI routes
- [x] `ui/views/shared/LoginView.java` built (PRN auth + role redirect)
- [x] `ui/views/admin/AdminDashboardView.java` placeholder
- [x] `ui/views/teacher/TeacherDashboardView.java` placeholder
- [x] `ui/views/student/StudentDashboardView.java` placeholder

### Vertical Slice 2 — Faculty Management ✅
- [x] FacultyRepository & DTOs
- [x] AdminService methods for Faculty CRUD
- [x] `FacultyManagementView` UI (CRUD + Validation)
- [x] `AdminDashboardView` updated with actual stat counts 

### Vertical Slice 3 — Course & Subject Management ✅
- [x] CourseRepository, SemesterRepository, SubjectRepository & DTOs
- [x] AdminService methods for Course & Subject CRUD
- [x] Course duration update validation logic (prevent shrinking if subjects exist)
- [x] Automatic Semester generation based on Course duration parameters
- [x] Subject code auto-generation logic implemented (e.g., Prefix: MCA23, editable number)
- [x] `CourseManagementView` UI with nested grids for Semesters
- [x] `SubjectManagementView` UI with cascading dropdowns
- [x] Service layer `@Transactional` boundaries updated to resolve `LazyInitializationException`
- [x] `AdminLayout` created as a shared wrapper (horizontal top bar with breadcrumb path, eliminating sidebar)

### Security & Infrastructure Fixes ✅
- [x] JWT storage moved from localStorage to HTTP-only cookie
      (fixes browser refresh 403 error)
- [x] JwtFilter updated to read token from cookie with
      null-safe getCookies() check
- [x] JwtFilter clears invalid/expired cookie automatically
      by setting maxAge=0
- [x] SecurityConfig updated with authenticationEntryPoint
      and accessDeniedHandler — redirects to login page
      instead of returning 403
- [x] vaadin.devmode.enabled=false added to
      application.properties — fixes too many cookies
      error in development mode
- [x] V3__remove_username.sql — dropped username column,
      made prn NOT NULL in users table

### Vertical Slice 4 — Student + Teacher Management ✅
- [x] StudentRepository and TeacherRepository with PRN
      queries and course/faculty lookups
- [x] CreateStudentRequest and CreateTeacherRequest DTOs
      (no password field — auto-generated)
- [x] StudentResponse and TeacherResponse DTOs with
      isActive field
- [x] ExcelImportResponse DTO for bulk import results
- [x] StudentImportRow and TeacherImportRow DTOs with
      phone field included
- [x] AdminService — createStudent() creates User +
      Student in one @Transactional method
- [x] AdminService — createTeacher() creates User +
      Teacher in one @Transactional method
- [x] Default password auto-generated as:
      firstName + "@" + last4DigitsOfPRN
      using prn.substring(prn.length() - 4) — robust
      for any PRN length
- [x] AdminService — deactivateStudent() and
      deactivateTeacher() — soft delete only,
      sets user.isActive = false, never hard deletes
- [x] AdminService — updateStudent() and updateTeacher()
      — never changes PRN or password
- [x] Excel import with two-step preview → confirm flow
      for both students and teachers
- [x] Excel header matching: case-insensitive, ignores
      underscores, hyphens, and spaces
- [x] Empty trailing rows silently skipped in all
      Excel imports
- [x] Phone field included in StudentImportRow and
      TeacherImportRow — saves correctly to DB
- [x] AuthService name fix — returns actual
      firstName + lastName instead of role name
- [x] AdminController — all student and teacher endpoints
      including preview-import and confirm-import
- [x] StudentManagementView — CRUD + Excel import,
      default semester=1, batch year auto-calculated
      as 202627 format, edit dropdowns pre-populated
      using ID-based matching
- [x] TeacherManagementView — CRUD + Excel import,
      edit faculty dropdown pre-populated using
      ID-based matching
- [x] AdminDashboardView — Total Students and Total
      Teachers stat cards connected to real counts
- [x] TeacherDashboardView route fixed to "teacher"
- [x] StudentDashboardView route fixed to "student"

### Vertical Slice 5 — Academic Calendar + Holiday Management ✅
- [x] AcademicCalendarRepository and HolidayRepository
- [x] CreateAcademicCalendarRequest,
      CreateHolidayRequest DTOs
- [x] AcademicCalendarResponse — semesterLabel shows
      "Odd Semester" or "Even Semester" only
- [x] HolidayResponse, HolidayImportRow,
      AcademicCalendarImportRow DTOs
- [x] AdminService — Academic Calendar CRUD with
      duplicate validation and date range validation
- [x] AdminService — Holiday CRUD with duplicate
      date validation
- [x] Multi-format date parser — accepts yyyy-MM-dd,
      dd/MM/yyyy, dd-MM-yyyy, d-M-yyyy, MM/dd/yyyy,
      yyyy/MM/dd and Excel native date cells
      dd-MM-yyyy tried before MM-dd-yyyy to avoid
      day/month swap bug
- [x] Holiday Excel import — two-step preview →
      confirm flow with validation
- [x] Academic Calendar Excel import — two-step
      preview → confirm flow with validation
- [x] AdminController — all calendar and holiday
      endpoints including preview-import and
      confirm-import
- [x] AcademicCalendarView — CRUD + Excel import,
      import button positioned below form matching
      Teacher Management layout style
- [x] HolidayManagementView — CRUD + Excel import
      with two-step preview flow
- [x] AdminDashboardView — Calendar and Holiday
      navigation buttons added

### Vertical Slice 6 — Teacher Allocation + Timetable Management ✅
- [x] Teacher Allocation + Timetable Management done.
- [x] Single combined form creates allocation and slot together.
- [x] Auto effectiveFrom from academic calendar.
- [x] Hard double booking block.
- [x] Excel import with preview.
- [x] Timezone fixed to Asia/Kolkata.

---

## In Progress 🔄

Nothing in progress — session ended

---

## Next Up ⏭️

Vertical Slice 7 — Live QR Generation (T1, T2)

---

## Not Started 📋
- [ ] Slice 7 — Live QR Generation (T1, T2)
- [ ] Slice 8 — Elective Enrollment (A9)
- [ ] Slice 9 — QR Scanner + Attendance (ST2)
- [ ] Slice 10 — Student Dashboard (ST1, ST3, ST4, ST5)
- [ ] Slice 11 — Teacher Reports + Timetable (T3, T4, T5)
- [ ] Slice 12 — Attendance Overview Report (A12)
- [ ] Slice 13 — Profile Screen (S2)
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
| 18 Mar 2025 | Login Vertical Slice (Backend) completed — 13 files covering UserRepository, JWT security, Auth service, and Login API endpoint |
| 18 Mar 2025 | Login Vaadin UI completed — SecurityConfig Vaadin bypass, LoginView with JWT storage, and 3 Dashboard placeholders completed |
| 18 Mar 2025 | Faculty Management Vertical Slice completed — Faculty CRUD, AdminService logic, and stats integrated on Admin Dashboard |
| 18 Mar 2025 | Course & Subject Management Vertical Slice completed — Course/Semester/Subject CRUD, code auto-gen, nested grids, duration validation, and redesigned AdminLayout wrapper added |
| 18 Mar 2025 | Security fixes — JWT cookie, 403 redirect, devmode disabled |
| 18 Mar 2025 | Slice 4 complete — Student + Teacher Management, Excel import with preview, soft delete, auto-generated passwords |
| 20 Mar 2025 | Slice 5 complete — Academic Calendar + Holiday Management, Excel import with preview, multi-format date parser, layout fixes |
| 21 Mar 2025 | Vertical Slice 6 complete — Teacher Allocation + Timetable Management |

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

### Default password format
firstName + "@" + last4DigitsOfPRN
Example: Rohan Sharma PRN 2212345001 → Rohan@5001
Always use prn.substring(prn.length() - 4)

### Batch year format
202627 = academic year 2026-27
month >= 6 → currentYear*100 + (currentYear+1)%100
month < 6  → (currentYear-1)*100 + currentYear%100

### Excel import rules (apply to all imports)
- Read headers by name not position
- Case-insensitive + ignore underscores/hyphens/spaces
- Skip rows where all key fields are blank
- Two-step preview → confirm flow
- Only valid rows inserted on confirm

### Next migration number
Next migration number: V4
Never modify V1, V2, V3.

### Timetable entry flow
One form → get-or-create allocation → create slot → effectiveFrom auto from calendar → double booking is hard block → delete slot cleans up orphan allocation automatically
