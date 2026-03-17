# Implementation Plan

Build order follows good software engineering principles — each step builds on the previous. Never skip ahead.

---

## Step 0 — Project Setup

- [ ] Create Spring Boot project using Spring Initializr (start.spring.io)
- [ ] Java 21, Maven, Spring Boot 3.x
- [ ] Add all dependencies to pom.xml
- [ ] Create folder structure as defined in ARCHITECTURE.md
- [ ] Create `.env` file and `.env.example`
- [ ] Create `.gitignore`
- [ ] Create PostgreSQL database named `qr_attendance`
- [ ] Verify app starts without errors

### pom.xml Dependencies
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
flyway-core (DB migrations)
io.github.cdimascio:dotenv-java
spring-boot-starter-test
```

---

## Step 1 — Models (Database Tables as Java Classes)

- [ ] Create all 14 model classes in `models/` package
- [ ] Each class annotated with `@Entity`, `@Table`, `@Id`, `@Column`
- [ ] All primary keys are UUID type with `@GeneratedValue`
- [ ] All relationships mapped with `@ManyToOne`, `@OneToMany` etc.
- [ ] Lombok annotations: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`
- [ ] Enums defined: `Role`, `SubjectType`, `AttendanceStatus`, `HolidayType`, `DayOfWeek`, `AdjustmentType`
- [ ] Verify Hibernate creates all tables correctly on startup (`spring.jpa.hibernate.ddl-auto=validate`)

### Files to create
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

## Step 2 — Database Migration (SQL Setup)

- [ ] Create `resources/db/migration/V1__create_tables.sql`
- [ ] Create all 15 tables with correct columns and constraints
- [ ] Add all UNIQUE constraints (especially UNIQUE(session_id, student_id) on attendance_records)
- [ ] Create `resources/db/migration/V2__seed_data.sql` with sample admin user
- [ ] Verify Flyway runs migrations cleanly on startup

---

## Step 3 — Repository Interfaces

- [ ] Create one repository interface per model class
- [ ] All extend `JpaRepository<ModelClass, UUID>`
- [ ] Add custom query methods needed for business logic:
  - `UserRepository`: `findByEmail`, `findByEmailAndIsActiveTrue`
  - `StudentRepository`: `findByCurrentSemesterId`, `findByCourseId`
  - `AttendanceSessionRepository`: `findByQrTokenAndIsActiveTrueAndExpiresAtAfter`
  - `AttendanceRecordRepository`: `findBySessionIdAndStudentId`, `existsBySessionIdAndStudentId`
  - `StudentSubjectEnrollmentRepository`: `existsByStudentIdAndSubjectIdAndAcademicYear`

---

## Step 4 — Security (JWT + Spring Security)

- [ ] `PasswordEncoderConfig.java` — BCrypt bean
- [ ] `JwtUtil.java` — generate token, validate token, extract claims
- [ ] `UserDetailsServiceImpl.java` — loads user by email for Spring Security
- [ ] `JwtFilter.java` — `OncePerRequestFilter` that validates JWT on every request
- [ ] `SecurityConfig.java` — defines which endpoints are public vs role-protected
  - Public: POST /api/v1/auth/login
  - ADMIN only: /api/v1/admin/**
  - TEACHER only: /api/v1/teacher/**
  - STUDENT only: /api/v1/student/**
  - Attendance scan: /api/v1/attendance/scan (STUDENT)
  - QR generate: /api/v1/attendance/generate (TEACHER)
- [ ] Test login endpoint returns valid JWT

---

## Step 5 — DTOs

- [ ] Create request DTOs (what comes IN from UI):
  - `LoginRequest`, `ChangePasswordRequest`
  - `CreateStudentRequest`, `CreateTeacherRequest`
  - `CreateFacultyRequest`, `CreateCourseRequest`
  - `CreateSubjectRequest`, `CreateTimetableSlotRequest`
  - `QrScanRequest`
- [ ] Create response DTOs (what goes OUT to UI):
  - `AuthResponse` (JWT token + role + user info)
  - `StudentResponse`, `TeacherResponse`
  - `QrResponse` (token + expires_at + session_id)
  - `AttendanceReportResponse`
  - `ErrorResponse` (status, error, message, path, timestamp)
- [ ] All request DTOs annotated with Bean Validation (`@NotBlank`, `@Email`, `@Size` etc.)

---

## Step 6 — Exception Handling

- [ ] `ResourceNotFoundException.java` — thrown when entity not found
- [ ] `ValidationException.java` — thrown for business rule violations
- [ ] `DuplicateScanException.java` — thrown when student already scanned
- [ ] `QrExpiredException.java` — thrown when token is expired
- [ ] `GlobalExceptionHandler.java` with `@ControllerAdvice`
  - Handles all custom exceptions
  - Always returns `ErrorResponse` JSON shape
  - Handles `MethodArgumentNotValidException` (Bean Validation failures)
  - Never returns Spring's default HTML error page

---

## Step 7 — Service Layer (Business Logic)

Build services in this order — each depends on the previous:

- [ ] `AuthService.java` — login, password change, JWT generation
- [ ] `AdminService.java` — CRUD for all university structure entities
- [ ] `ExcelImportService.java` — Apache POI reads .xlsx, calls AdminService per row, returns import result
- [ ] `TimetableService.java` — timetable slot management, double-booking check
- [ ] `QrService.java` — generate UUID token, create AttendanceSession with expiry
- [ ] `AttendanceService.java` — 5-check scan validation chain, insert PRESENT record
- [ ] `ScheduledJobService.java` — `@Scheduled` job that runs after session expiry, inserts ABSENT records
- [ ] `StudentService.java` — student dashboard data, attendance % per subject
- [ ] `TeacherService.java` — teacher report data, today's timetable

### Critical: Scan Validation in AttendanceService
```
Check 1: token exists + is_active = true + expires_at > NOW()    → throw QrExpiredException
Check 2: session.semester_id == student.current_semester_id       → throw ValidationException
Check 3a: if COMPULSORY → subject.semester_id == student semester → throw ValidationException
Check 3b: if ELECTIVE → row in student_subject_enrollments        → throw ValidationException
Check 4: no existing record for (session_id, student_id)          → throw DuplicateScanException
Check 5: INSERT — DB UNIQUE constraint is final safety net
```

---

## Step 8 — Controllers (API Endpoints)

- [ ] `AuthController` — POST /api/v1/auth/login, POST /api/v1/auth/change-password
- [ ] `AdminController` — all CRUD endpoints for Admin screens A2–A12
- [ ] `TeacherController` — timetable view, attendance reports for T1, T3–T5
- [ ] `StudentController` — dashboard data, attendance detail, timetable for ST1, ST3–ST5
- [ ] `AttendanceController` — POST generate (T2), POST scan (ST2)
- [ ] All endpoints annotated with `@PreAuthorize` for role enforcement
- [ ] All endpoints use DTOs — never return raw model objects
- [ ] All POST/PUT endpoints use `@Valid` for request DTO validation

---

## Step 9 — Vaadin UI (All 26 Screens)

Build screens in this order — login first, admin setup second, core features last:

### Phase A — Shared
- [ ] S1 — Login screen
- [ ] S2 — Profile screen

### Phase B — Admin setup screens
- [ ] A1 — Admin dashboard
- [ ] A2 — Faculty management
- [ ] A3 — Course management + Excel upload
- [ ] A4 — Subject management + Excel upload
- [ ] A5 — Academic calendar management
- [ ] A6 — Holiday management + Excel upload
- [ ] A7 — Student management + Excel upload (most complex admin screen)
- [ ] A8 — Teacher management
- [ ] A9 — Elective enrollment management
- [ ] A10 — Teacher-subject allocation
- [ ] A11 — Timetable builder + Excel upload
- [ ] A12 — Attendance overview report

### Phase C — Teacher screens
- [ ] T1 — Teacher dashboard (today's timetable + generate QR buttons)
- [ ] T2 — Live QR screen (full screen QR + countdown + live counter)
- [ ] T3 — Attendance report by subject
- [ ] T4 — Attendance report by session
- [ ] T5 — My timetable

### Phase D — Student screens
- [ ] ST1 — Student dashboard (attendance cards + today's timetable)
- [ ] ST2 — QR scanner (camera + scan + feedback)
- [ ] ST3 — My attendance full detail
- [ ] ST4 — My timetable
- [ ] ST5 — My subjects

---

## Step 10 — Testing

- [ ] Unit tests for `AttendanceService` — all 5 scan validation checks
- [ ] Unit tests for `QrService` — token generation and expiry
- [ ] Unit tests for `ExcelImportService` — valid file, missing columns, duplicate rows
- [ ] Integration test for the full QR scan flow (generate → scan → PRESENT recorded)
- [ ] Integration test for duplicate scan rejection
- [ ] Integration test for expired token rejection
- [ ] Test wrong semester scan rejection
- [ ] Test non-enrolled elective scan rejection

---

## V2 Features (After V1 Is Complete and Working)

- [ ] Lecture adjustment screen (T6) + lecture_adjustments table activation
- [ ] Geolocation scan validation (add room_lat, room_lng, radius_meters to attendance_sessions)
- [ ] Student elective self-selection window (A13 + update ST5)
- [ ] Push notifications for cancelled/rescheduled lectures

---

## Build Principles to Always Follow

1. **Never skip a step** — each step's output is the foundation of the next
2. **One feature at a time** — build, test, confirm working, then move to next
3. **Update PROGRESS.md** after every completed step
4. **Never hardcode secrets** — all sensitive values in `.env`
5. **Commit to Git** after every working feature — small commits, clear messages
6. **If something breaks** — fix it before moving forward, never carry broken code
