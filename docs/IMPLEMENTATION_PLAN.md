# Implementation Plan

Build order follows good software engineering principles — each step builds on the previous. Never skip ahead.

**Build approach — vertical slice development.** Build one complete screen end to end (UI → Controller → Service → Repository → DB) before moving to the next. Never build horizontally across all layers first. The first vertical slice is the Login screen (S1).

---

## Step 0 — Project Setup ✅ COMPLETE

- [x] Create Spring Boot project
- [x] Java 21, Maven, Spring Boot 3.x
- [x] Add all dependencies to pom.xml (including flyway-database-postgresql)
- [x] Create `.env` file and `.env.example`
- [x] Create `.gitignore`
- [x] Database: Neon.tech cloud PostgreSQL — no local install needed
- [x] Create AppConfig.java — loads dotenv at startup
- [x] Create PasswordEncoderConfig.java — BCrypt bean
- [x] Verify app starts on port 8080 without errors

### pom.xml Dependencies (all added)
```xml
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation
vaadin-spring-boot-starter (Vaadin 24)
postgresql
jjwt-api + jjwt-impl + jjwt-jackson (0.12.x)
lombok
zxing core + javase (3.5.x)
apache-poi + apache-poi-ooxml (Excel)
flyway-core + flyway-database-postgresql (DB migrations)
io.github.cdimascio:dotenv-java
spring-boot-starter-test
```

---

## Step 1 — Models (Database Tables as Java Classes) ✅ COMPLETE

- [x] Create all 14 model classes in `models/` package
- [x] Each class annotated with `@Entity`, `@Table`, `@Id`, `@Column`
- [x] All primary keys are UUID type with `@GeneratedValue`
- [x] All relationships mapped with `@ManyToOne`, `@OneToMany` etc.
- [x] Lombok annotations: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`
- [x] Enums defined: `Role`, `SubjectType`, `AttendanceStatus`, `HolidayType`, `DayOfWeek`, `AdjustmentType`
- [x] User.java uses `prn` field (VARCHAR 10, NOT NULL, UNIQUE) — no email, no username
- [x] Student.java uses `prn` field (renamed from enrollment_no)
- [x] Teacher.java uses `prn` field (renamed from employee_id)

### Files created
```
models/User.java
models/Student.java
models/Teacher.java
models/Faculty.java
models/Course.java
models/Semester.java
models/Subject.java
models/AcademicCalendar.java
models/Holiday.java
models/TimetableSlot.java
models/TeacherSubjectAllocation.java
models/StudentSubjectEnrollment.java
models/AttendanceSession.java
models/AttendanceRecord.java
```

---

## Step 2 — Database Migration (SQL Setup) ✅ COMPLETE

- [x] V1__create_tables.sql — created all 14 tables, verified in Neon
- [x] V2__update_prn_login.sql — added prn column, renamed enrollment_no → prn in students, renamed employee_id → prn in teachers
- [x] V3__remove_username.sql — dropped username column, made prn NOT NULL in users
- [x] All 14 tables verified live in Neon database

> New migrations must start from V4. Never modify V1, V2, or V3.

---

## Vertical Slice 1 — Login Screen (S1) ← CURRENT

Build everything needed for login to work completely end to end, then test it, then move on.

### Files needed for Login slice

**Repository**
- [ ] `repository/UserRepository.java`
  - `findByPrn(String prn)`
  - `findByPrnAndIsActiveTrue(String prn)`

**DTOs**
- [ ] `dto/request/LoginRequest.java` — prn (@Size min=10 max=10), password (@NotBlank)
- [ ] `dto/response/AuthResponse.java` — token, role, name, prn
- [ ] `dto/response/ErrorResponse.java` — status, error, message, path, timestamp

**Exception handling**
- [ ] `exception/ResourceNotFoundException.java`
- [ ] `exception/ValidationException.java`
- [ ] `exception/GlobalExceptionHandler.java` — always returns ErrorResponse JSON shape

**Security**
- [ ] `security/JwtUtil.java` — generate token (reads jwt.secret + jwt.expiry.ms), validate, extract claims
- [ ] `security/UserDetailsServiceImpl.java` — loadUserByUsername receives PRN, calls findByPrn
- [ ] `security/JwtFilter.java` — OncePerRequestFilter, validates Bearer token, sets SecurityContext
- [ ] `security/SecurityConfig.java` — stateless, only /api/v1/auth/login is public, @EnableMethodSecurity

**Service**
- [ ] `service/AuthService.java`
  - Takes LoginRequest (prn + password)
  - findByPrn → if not found throw ResourceNotFoundException
  - if is_active false → throw ValidationException("Account is deactivated")
  - if password doesn't match BCrypt → throw ValidationException("Invalid credentials")
  - generate JWT → return AuthResponse

**Controller**
- [ ] `controller/AuthController.java`
  - POST /api/v1/auth/login — public endpoint, no @PreAuthorize

**Vaadin UI**
- [ ] `ui/views/shared/LoginView.java`
  - PRN field (10 digit input)
  - Password field
  - Login button → calls /api/v1/auth/login
  - On success → store JWT, redirect to role dashboard
  - On failure → show error message
  - Mobile responsive

### Test checklist for Login slice
- [ ] App starts without errors after all files created
- [ ] Insert test user into Neon with BCrypt hashed password
- [ ] POST /api/v1/auth/login with correct PRN + password → returns JWT token
- [ ] POST /api/v1/auth/login with wrong password → returns 400 "Invalid credentials"
- [ ] POST /api/v1/auth/login with unknown PRN → returns 404 "User not found"
- [ ] Login via browser at localhost:8080 → redirects to correct dashboard

---

## Vertical Slice 2 — Admin Dashboard (A1) + Faculty Management (A2)

> Start only after Login slice is fully tested and working.

- [ ] `repository/FacultyRepository.java`
- [ ] `dto/request/CreateFacultyRequest.java`, `dto/response/FacultyResponse.java`
- [ ] `service/AdminService.java` (faculty CRUD only)
- [ ] `controller/AdminController.java` (faculty endpoints only)
- [ ] `ui/views/admin/AdminDashboardView.java`
- [ ] `ui/views/admin/FacultyManagementView.java`
- [ ] Test full CRUD via browser as Admin

---

## Vertical Slice 3 — Course + Semester + Subject Management (A3, A4)

> Start only after Slice 2 is fully tested.

- [ ] Repositories: CourseRepository, SemesterRepository, SubjectRepository
- [ ] DTOs for Course, Semester, Subject
- [ ] AdminService — course/semester/subject CRUD + Excel import
- [ ] ExcelImportService — Apache POI for bulk subject/course upload
- [ ] AdminController — course/subject endpoints
- [ ] Vaadin views: CourseManagementView, SubjectManagementView

---

## Vertical Slice 4 — Student + Teacher Management (A7, A8)

> Start only after Slice 3 is fully tested.

- [ ] Repositories: StudentRepository, TeacherRepository
- [ ] DTOs for Student, Teacher (request uses prn — not email, not enrollment_no)
- [ ] AdminService — student/teacher CRUD + Excel import
- [ ] Vaadin views: StudentManagementView, TeacherManagementView
- [ ] Test: create student via form, login with their PRN + password

---

## Vertical Slice 5 — Academic Calendar + Holidays (A5, A6)

- [ ] Repositories: AcademicCalendarRepository, HolidayRepository
- [ ] AdminService — calendar/holiday CRUD
- [ ] Vaadin views: AcademicCalendarView, HolidayManagementView

---

## Vertical Slice 6 — Teacher Allocation + Timetable (A10, A11)

- [ ] Repositories: TeacherSubjectAllocationRepository, TimetableSlotRepository
- [ ] TimetableService — slot management, double-booking check
- [ ] Vaadin views: TeacherAllocationView, TimetableBuilderView + Excel upload

---

## Vertical Slice 7 — Live QR Generation (T1, T2)

- [ ] AttendanceSessionRepository
- [ ] QrService — generate UUID token, set expires_at = NOW() + QR_EXPIRY_SECONDS
- [ ] AttendanceController — POST /api/v1/attendance/generate (TEACHER only)
- [ ] Vaadin views: TeacherDashboardView, LiveQrView (countdown timer + live counter)
- [ ] ScheduledJobService — @Scheduled job inserts ABSENT records after expiry
- [ ] Test: generate QR, verify session row in Neon, verify expiry

---

## Vertical Slice 8 — QR Scanner (ST2) + Attendance Validation

- [ ] AttendanceRecordRepository
- [ ] StudentSubjectEnrollmentRepository
- [ ] AttendanceService — full 5-check validation chain
  ```
  Check 1: token valid + not expired
  Check 2: semester match
  Check 3: subject eligibility (compulsory or elective)
  Check 4: no duplicate scan
  Check 5: DB UNIQUE constraint safety net
  ```
- [ ] AttendanceController — POST /api/v1/attendance/scan (STUDENT only)
- [ ] DuplicateScanException, QrExpiredException
- [ ] Vaadin views: QrScannerView (camera, instant feedback)
- [ ] Test: scan valid QR → PRESENT recorded. Scan twice → rejected. Scan expired → rejected.

---

## Vertical Slice 9 — Student Dashboard + Attendance Views (ST1, ST3, ST4, ST5)

- [ ] StudentService — attendance % per subject, today's timetable
- [ ] StudentController
- [ ] Vaadin views: StudentDashboardView, AttendanceDetailView, StudentTimetableView, SubjectsView

---

## Vertical Slice 10 — Teacher Reports + Timetable (T3, T4, T5)

- [ ] TeacherService — attendance reports, today's timetable
- [ ] TeacherController
- [ ] Vaadin views: AttendanceBySubjectView, AttendanceBySessionView, TeacherTimetableView

---

## Vertical Slice 11 — Remaining Admin Screens (A9, A12)

- [ ] AdminService — elective enrollment management, attendance overview report
- [ ] Vaadin views: ElectiveEnrollmentView, AttendanceOverviewReport (with Excel export)

---

## Vertical Slice 12 — Profile Screen (S2)

- [ ] AuthService — change password
- [ ] Vaadin views: ProfileView (all roles)

---

## Step 10 — Testing

- [ ] Unit tests for `AttendanceService` — all 5 scan validation checks
- [ ] Unit tests for `QrService` — token generation and expiry
- [ ] Unit tests for `ExcelImportService` — valid file, missing columns, duplicate rows
- [ ] Integration test for full QR scan flow (generate → scan → PRESENT recorded)
- [ ] Integration test for duplicate scan rejection
- [ ] Integration test for expired token rejection
- [ ] Test wrong semester scan rejection
- [ ] Test non-enrolled elective scan rejection

---

## V2 Features (After V1 Is Complete and Working)

- [ ] Lecture adjustment screen (T6) + uncomment lecture_adjustments table in SQL
- [ ] Geolocation scan validation (add room_lat, room_lng, radius_meters to attendance_sessions)
- [ ] Student elective self-selection window (A13 + update ST5)
- [ ] Push notifications for cancelled/rescheduled lectures

---

## Build Principles to Always Follow

1. **Vertical slice** — build one complete screen end to end before starting the next
2. **Test before moving on** — confirm the current slice works fully before touching the next
3. **Update PROGRESS.md** after every completed slice
4. **Never hardcode secrets** — all sensitive values in `.env`
5. **New migrations only** — never modify V1, V2, V3. New changes = V4, V5 etc.
6. **Commit to Git** after every working feature — small commits, clear messages
7. **If something breaks** — fix it before moving forward, never carry broken code
8. **PRN everywhere** — login is always PRN + password. No email. No username.
