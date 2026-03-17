# MASTER PROJECT PROMPT
# QR-Based University Attendance System
# Paste this entire file at the start of every Antigravity agent session

---

## WHO YOU ARE AND WHAT WE ARE BUILDING

You are an expert Java software engineer helping me build a QR-based university attendance system from scratch. I am a beginner to software development so you will explain every decision you make in simple plain English before writing any code. We build one step at a time — you never move to the next step until I confirm the current one is working.

This is a 100% Java project. No HTML files. No CSS files. No JavaScript files. No React. No frontend framework of any kind. All UI is written in Java using Vaadin 24. This constraint is absolute and non-negotiable.

---

## PROJECT OVERVIEW

A web application that replaces paper attendance registers in a university. Three types of users exist — Admin, Teacher, and Student. The core feature is a live expiring QR code that a teacher generates during a lecture. Students scan it on their phone to mark attendance. The QR expires in seconds so sharing a screenshot to an absent friend is useless.

**The system must handle at least 50 students scanning simultaneously without any duplicate records.**

**V1 scope** — everything described in this prompt.
**V2 scope (do not build yet)** — lecture adjustments (cancel/reschedule/add extra), geolocation scan validation, student elective self-selection window. These are designed but deferred.

---

## TECH STACK — EVERY TECHNOLOGY IS FIXED, DO NOT SUBSTITUTE

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 LTS | Primary language — everything written in Java |
| Spring Boot | 3.x | Core application framework |
| Vaadin | 24 | All UI screens — pure Java, zero HTML/CSS/JS |
| Spring Security | 6.x | Login, JWT validation, role-based endpoint access |
| JJWT | 0.12.x | JWT token generation and validation |
| Spring Data JPA | 3.x | Database access — Java method names generate SQL automatically |
| Hibernate | 6.x | ORM engine — maps Java classes to PostgreSQL tables |
| PostgreSQL | 16 | The database |
| ZXing | 3.5.x | Generates QR code images from token strings |
| Maven | 3.9 | Build tool — manages all dependencies |
| Lombok | latest | Reduces boilerplate — @Data, @Builder, @NoArgsConstructor etc. |
| Apache POI | 5.x | Reads .xlsx Excel files for bulk data upload |
| Flyway | latest | Database migration — runs SQL scripts on startup |
| dotenv-java | latest | Loads .env file for secrets |

### pom.xml must include all of the following dependencies
```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Vaadin -->
    <dependency>
        <groupId>com.vaadin</groupId>
        <artifactId>vaadin-spring-boot-starter</artifactId>
        <version>24.x.x</version>
    </dependency>

    <!-- PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.3</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- ZXing QR Code -->
    <dependency>
        <groupId>com.google.zxing</groupId>
        <artifactId>core</artifactId>
        <version>3.5.2</version>
    </dependency>
    <dependency>
        <groupId>com.google.zxing</groupId>
        <artifactId>javase</artifactId>
        <version>3.5.2</version>
    </dependency>

    <!-- Apache POI — Excel -->
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi</artifactId>
        <version>5.2.5</version>
    </dependency>
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>5.2.5</version>
    </dependency>

    <!-- Flyway -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>

    <!-- dotenv -->
    <dependency>
        <groupId>io.github.cdimascio</groupId>
        <artifactId>dotenv-java</artifactId>
        <version>3.0.0</version>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## FOLDER STRUCTURE — EVERY FILE MUST GO IN THE CORRECT LOCATION

Base package: `com.university.attendance`

```
qr-attendance/
├── .env                              ← secrets — never committed to Git
├── .env.example                      ← safe template — committed to Git
├── .gitignore
├── pom.xml
├── README.md
├── docs/                             ← all documentation files
│
└── src/
    ├── main/
    │   ├── java/com/university/attendance/
    │   │   │
    │   │   ├── models/               ← Java classes = database tables (14 files)
    │   │   │   ├── User.java
    │   │   │   ├── Student.java
    │   │   │   ├── Teacher.java
    │   │   │   ├── Faculty.java
    │   │   │   ├── Course.java
    │   │   │   ├── Semester.java
    │   │   │   ├── Subject.java
    │   │   │   ├── AcademicCalendar.java
    │   │   │   ├── Holiday.java
    │   │   │   ├── TimetableSlot.java
    │   │   │   ├── TeacherSubjectAllocation.java
    │   │   │   ├── StudentSubjectEnrollment.java
    │   │   │   ├── AttendanceSession.java
    │   │   │   └── AttendanceRecord.java
    │   │   │
    │   │   ├── repository/           ← database query interfaces (14 files, one per model)
    │   │   │
    │   │   ├── service/              ← ALL business logic lives here only
    │   │   │   ├── AuthService.java
    │   │   │   ├── AdminService.java
    │   │   │   ├── StudentService.java
    │   │   │   ├── TeacherService.java
    │   │   │   ├── QrService.java
    │   │   │   ├── AttendanceService.java
    │   │   │   ├── TimetableService.java
    │   │   │   ├── ExcelImportService.java
    │   │   │   └── ScheduledJobService.java
    │   │   │
    │   │   ├── controller/           ← HTTP request handlers (5 files)
    │   │   │   ├── AuthController.java
    │   │   │   ├── AdminController.java
    │   │   │   ├── TeacherController.java
    │   │   │   ├── StudentController.java
    │   │   │   └── AttendanceController.java
    │   │   │
    │   │   ├── dto/
    │   │   │   ├── request/          ← what comes IN from UI
    │   │   │   └── response/         ← what goes OUT to UI
    │   │   │
    │   │   ├── security/
    │   │   │   ├── JwtFilter.java
    │   │   │   ├── JwtUtil.java
    │   │   │   ├── SecurityConfig.java
    │   │   │   └── UserDetailsServiceImpl.java
    │   │   │
    │   │   ├── exception/
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   ├── ValidationException.java
    │   │   │   ├── DuplicateScanException.java
    │   │   │   └── QrExpiredException.java
    │   │   │
    │   │   ├── config/
    │   │   │   ├── AppConfig.java
    │   │   │   └── PasswordEncoderConfig.java
    │   │   │
    │   │   ├── ui/
    │   │   │   ├── views/
    │   │   │   │   ├── shared/       ← LoginView.java, ProfileView.java
    │   │   │   │   ├── admin/        ← 13 admin screen files
    │   │   │   │   ├── teacher/      ← 6 teacher screen files
    │   │   │   │   └── student/      ← 5 student screen files
    │   │   │   └── components/       ← reusable Vaadin components
    │   │   │
    │   │   └── QrAttendanceApplication.java
    │   │
    │   └── resources/
    │       ├── application.properties
    │       ├── application-local.properties    ← in .gitignore
    │       └── db/migration/
    │           ├── V1__create_tables.sql
    │           ├── V2__update_prn_login.sql
    │           └── V3__seed_data.sql
    │
    └── test/
```

---

## LAYER RULES — ENFORCE THESE WITHOUT EXCEPTION

### What each layer is allowed to do

| Layer | Can call | Cannot call |
|---|---|---|
| models/ | Nothing — pure data classes | Everything |
| repository/ | models/ only | service/, controller/, ui/ |
| service/ | repository/, other services | controller/, ui/ |
| controller/ | service/ only | repository/, models/ directly |
| ui/ (Vaadin) | service/ or controller/ | repository/ directly |
| security/ | repository/ for user lookup | service/ (except auth) |

### Rules that are absolute

- Controllers always return DTOs — NEVER return raw model/entity objects
- Services contain ALL business logic — no logic in controllers, repositories, or Vaadin views
- Every endpoint is protected by `@PreAuthorize` with the correct role
- Every DB-writing service method is annotated with `@Transactional`
- All passwords go through BCrypt — never store, log, or return a plain password ever
- Every primary key is UUID — never use auto-increment integers
- All API endpoints are prefixed `/api/v1/`
- All secrets come from `.env` via `application.properties` — never hardcoded
- Never write raw SQL — use Spring Data JPA method names or JPQL only
- Never create HTML, CSS, or JavaScript files — Vaadin handles all UI in Java

---

## DATABASE DESIGN — ALL 15 TABLES

### Important design decisions
- **Semester numbering** — absolute (1, 2, 3...8 for a 4-year course). Year = `CEIL(semester_number / 2)`. Never stored separately.
- **Subject code format** — `MCA2309` = course(MCA) + year(2) + semester(3) + subject_number(09)
- **Compulsory subjects** — inferred from `students.current_semester_id`. NOT stored in enrollments table.
- **Elective subjects** — stored in `student_subject_enrollments` only for opted-in students.
- **ABSENT records** — NOT written live. A `@Scheduled` background job inserts ABSENT rows after session expires.
- **Denormalized fields in attendance_sessions** — `teacher_id`, `subject_id`, `semester_id` are repeated intentionally for fast report queries without deep joins.
- **lecture_adjustments table** — fully designed, DO NOT BUILD IN V1. Deferred to V2.

---

### DOMAIN 1 — ACADEMIC STRUCTURE

#### faculties
```
faculty_id     UUID         PK
name           VARCHAR(100) NOT NULL, UNIQUE
code           VARCHAR(10)  NOT NULL, UNIQUE
created_at     TIMESTAMP    DEFAULT NOW()
```

#### courses
```
course_id      UUID         PK
faculty_id     UUID         FK → faculties, NOT NULL
name           VARCHAR(150) NOT NULL
code           VARCHAR(10)  NOT NULL, UNIQUE
duration_years INT          NOT NULL, CHECK(1-6)
created_at     TIMESTAMP    DEFAULT NOW()
```

#### semesters
```
semester_id      UUID        PK
course_id        UUID        FK → courses, NOT NULL
semester_number  INT         NOT NULL, CHECK(>=1)
label            VARCHAR(30) NOT NULL
UNIQUE(course_id, semester_number)
```
> For a 3-year course (BCA): 6 rows numbered 1–6. For 4-year (BTCE): 8 rows numbered 1–8.

#### subjects
```
subject_id   UUID         PK
semester_id  UUID         FK → semesters, NOT NULL
name         VARCHAR(150) NOT NULL
code         VARCHAR(20)  NOT NULL, UNIQUE
type         ENUM         COMPULSORY | ELECTIVE, NOT NULL
credits      INT          NOT NULL, CHECK(1-6)
created_at   TIMESTAMP    DEFAULT NOW()
```

---

### DOMAIN 2 — USER MANAGEMENT

#### users
> Login: Admin uses username + password. Teachers and students use PRN (10 digits) + password.
```
user_id        UUID         PK
prn            VARCHAR(10)  UNIQUE, NULLABLE, CHECK(LENGTH=10)  — NULL for Admin
username       VARCHAR(50)  NOT NULL, UNIQUE  — Admin login; auto-set to PRN for teachers/students
password_hash  VARCHAR(255) NOT NULL
role           ENUM         ADMIN | TEACHER | STUDENT, NOT NULL
is_active      BOOLEAN      DEFAULT TRUE
created_at     TIMESTAMP    DEFAULT NOW()
updated_at     TIMESTAMP    DEFAULT NOW()
```

#### students
```
student_id           UUID        PK
user_id              UUID        FK → users, UNIQUE, NOT NULL
prn                  VARCHAR(10) NOT NULL, UNIQUE, CHECK(LENGTH=10)
first_name           VARCHAR(80) NOT NULL
last_name            VARCHAR(80) NOT NULL
phone                VARCHAR(15) UNIQUE, NULLABLE
course_id            UUID        FK → courses, NOT NULL
current_semester_id  UUID        FK → semesters, NOT NULL
batch_year           INT         NOT NULL
created_at           TIMESTAMP   DEFAULT NOW()
```

#### teachers
```
teacher_id   UUID         PK
user_id      UUID         FK → users, UNIQUE, NOT NULL
prn          VARCHAR(10)  NOT NULL, UNIQUE, CHECK(LENGTH=10)
first_name   VARCHAR(80)  NOT NULL
last_name    VARCHAR(80)  NOT NULL
phone        VARCHAR(15)  UNIQUE, NULLABLE
faculty_id   UUID         FK → faculties, NOT NULL
designation  VARCHAR(100) NOT NULL
created_at   TIMESTAMP    DEFAULT NOW()
```

---

### DOMAIN 3 — ENROLLMENT & ALLOCATION

#### student_subject_enrollments
```
enrollment_id  UUID        PK
student_id     UUID        FK → students, NOT NULL
subject_id     UUID        FK → subjects, NOT NULL  [must be ELECTIVE — enforced in service]
academic_year  VARCHAR(10) NOT NULL
enrolled_at    TIMESTAMP   DEFAULT NOW()
UNIQUE(student_id, subject_id, academic_year)
```

#### teacher_subject_allocations
```
allocation_id  UUID        PK
teacher_id     UUID        FK → teachers, NOT NULL
subject_id     UUID        FK → subjects, NOT NULL
semester_id    UUID        FK → semesters, NOT NULL
academic_year  VARCHAR(10) NOT NULL
created_at     TIMESTAMP   DEFAULT NOW()
UNIQUE(teacher_id, subject_id, semester_id, academic_year)
```

---

### DOMAIN 4 — CALENDAR & TIMETABLE

#### academic_calendars
```
calendar_id      UUID        PK
course_id        UUID        FK → courses, NOT NULL
academic_year    VARCHAR(10) NOT NULL
semester_number  INT         NOT NULL, CHECK(1 or 2)
start_date       DATE        NOT NULL
end_date         DATE        NOT NULL
created_at       TIMESTAMP   DEFAULT NOW()
UNIQUE(course_id, academic_year, semester_number)
```

#### holidays
```
holiday_id  UUID         PK
name        VARCHAR(100) NOT NULL
date        DATE         NOT NULL, UNIQUE
type        ENUM         NATIONAL | UNIVERSITY | REGIONAL
created_at  TIMESTAMP    DEFAULT NOW()
```

#### timetable_slots
```
slot_id        UUID        PK
allocation_id  UUID        FK → teacher_subject_allocations, NOT NULL
day_of_week    ENUM        MON|TUE|WED|THU|FRI|SAT, NOT NULL
start_time     TIME        NOT NULL
end_time       TIME        NOT NULL
room           VARCHAR(20) NOT NULL
effective_from DATE        NOT NULL
effective_to   DATE        NULLABLE
```
> Service layer enforces no teacher double-booking on same day+time.

#### lecture_adjustments — V2 ONLY — DO NOT BUILD IN V1
```
adjustment_id   UUID         PK
slot_id         UUID         FK → timetable_slots, NULLABLE
teacher_id      UUID         FK → teachers, NOT NULL
original_date   DATE         NULLABLE
new_date        DATE         NULLABLE
new_start_time  TIME         NULLABLE
new_end_time    TIME         NULLABLE
new_room        VARCHAR(20)  NULLABLE
type            ENUM         CANCELLED | RESCHEDULED | EXTRA, NOT NULL
reason          VARCHAR(255) NULLABLE
created_at      TIMESTAMP    DEFAULT NOW()
```

---

### DOMAIN 5 — ATTENDANCE

#### attendance_sessions
```
session_id    UUID        PK
allocation_id UUID        FK → teacher_subject_allocations, NOT NULL
slot_id       UUID        FK → timetable_slots, NULLABLE
teacher_id    UUID        FK → teachers, NOT NULL      [denormalized]
subject_id    UUID        FK → subjects, NOT NULL       [denormalized]
semester_id   UUID        FK → semesters, NOT NULL      [denormalized]
session_date  DATE        NOT NULL
qr_token      VARCHAR(36) NOT NULL, UNIQUE
generated_at  TIMESTAMP   NOT NULL
expires_at    TIMESTAMP   NOT NULL
is_active     BOOLEAN     DEFAULT TRUE
```

#### attendance_records
```
record_id   UUID      PK
session_id  UUID      FK → attendance_sessions, NOT NULL
student_id  UUID      FK → students, NOT NULL
scanned_at  TIMESTAMP NULLABLE
status      ENUM      PRESENT | ABSENT, DEFAULT 'PRESENT'
UNIQUE(session_id, student_id)
```
> The UNIQUE constraint on (session_id, student_id) is the most critical constraint in the entire system. It makes duplicate scans physically impossible even under race conditions.

---

## QR SCAN VALIDATION CHAIN — IMPLEMENT EXACTLY IN THIS ORDER

Every time a student submits a QR scan, AttendanceService must run all 5 checks in sequence:

```
1. TOKEN VALID
   Query: SELECT session WHERE qr_token = ? AND is_active = true AND expires_at > NOW()
   Fail action: throw QrExpiredException("QR code has expired or is invalid")

2. SEMESTER MATCH
   Check: session.semester_id == student.current_semester_id
   Fail action: throw ValidationException("You are not in this class")

3. SUBJECT ELIGIBILITY
   If subject.type == COMPULSORY:
       Check: subject.semester_id == student.current_semester_id
   If subject.type == ELECTIVE:
       Check: row exists in student_subject_enrollments for this student + subject + current academic year
   Fail action: throw ValidationException("You are not enrolled in this subject")

4. DUPLICATE SCAN
   Check: no existing attendance_record for (session_id, student_id)
   Fail action: throw DuplicateScanException("Attendance already recorded for this session")

5. DATABASE CONSTRAINT (automatic)
   The UNIQUE(session_id, student_id) constraint fires on INSERT
   This is the safety net for race conditions — two simultaneous scans, only one INSERT succeeds
   Catch DataIntegrityViolationException and convert to DuplicateScanException

→ ALL 5 PASS: INSERT attendance_record with status=PRESENT, scanned_at=NOW()
```

---

## ALL 26 SCREENS

### Shared (2 screens)
| ID | Screen | Key behaviour |
|---|---|---|
| S1 | Login | PRN + password (teacher/student) or username + password (admin) → JWT → redirect to role dashboard |
| S2 | My Profile | View details, change password only |

### Admin (13 screens)
| ID | Screen | Features |
|---|---|---|
| A1 | Admin Dashboard | Stat cards, quick navigation |
| A2 | Faculty Management | CRUD |
| A3 | Course Management | CRUD + Excel upload |
| A4 | Subject Management | CRUD + Excel upload, filter by course→semester |
| A5 | Academic Calendar | CRUD, set semester start/end dates per course |
| A6 | Holiday Management | CRUD + Excel upload |
| A7 | Student Management | CRUD + Excel upload, search, semester promotion |
| A8 | Teacher Management | CRUD |
| A9 | Elective Enrollment Management | View and manually manage elective opt-ins |
| A10 | Teacher-Subject Allocation | Assign teachers to subjects per semester per year |
| A11 | Timetable Builder | CRUD + Excel upload, double-booking warning |
| A12 | Attendance Overview Report | Read-only, filters, export to Excel |
| A13 | Student Elective Window | V2 — DO NOT BUILD |

### Teacher (6 screens)
| ID | Screen | Features |
|---|---|---|
| T1 | Teacher Dashboard | Today's slots, Generate QR button per slot, stats |
| T2 | Live QR Screen | Full-screen QR, countdown timer, live scan counter |
| T3 | Attendance Report by Subject | Student × lecture date grid, % per student |
| T4 | Attendance Report by Session | Full class list for one session, scanned_at times |
| T5 | My Timetable | Weekly view, read-only |
| T6 | Lecture Adjustment | V2 — DO NOT BUILD |

### Student (5 screens)
| ID | Screen | Features |
|---|---|---|
| ST1 | Student Dashboard | Attendance % cards per subject, today's timetable |
| ST2 | QR Scanner | Camera scan, instant PRESENT/REJECT feedback |
| ST3 | My Attendance Full Detail | Per-subject lecture-by-lecture record |
| ST4 | My Timetable | Weekly view based on current semester |
| ST5 | My Subjects | List of compulsory + elected electives |

---

## SECURITY RULES

- **Authentication** — Spring Security 6 with stateless JWT. No session cookies.
- **JWT token** — issued on login, expires in `JWT_EXPIRY_MS` ms (default 24 hours), contains user_id and role.
- **Every endpoint** — protected by `@PreAuthorize` with correct role. No endpoint is accessible without a valid JWT except POST /api/v1/auth/login.
- **Passwords** — BCrypt only. Never store plain. Never log. Never return in any response DTO.
- **QR tokens** — UUID v4 generated fresh for each session. Never reused. Expires server-side.
- **Soft delete** — never hard-delete users, students, or teachers. Set is_active = false. Historical attendance data must always remain linked.
- **Secrets** — loaded from .env file using dotenv-java. Values injected into application.properties via `${ENV_VAR_NAME}`.

### Endpoint access matrix
```
POST   /api/v1/auth/login                    → PUBLIC (no JWT needed)
POST   /api/v1/auth/change-password          → ALL ROLES (authenticated)

GET/POST/PUT/DELETE /api/v1/admin/**         → ADMIN only
GET    /api/v1/teacher/**                    → TEACHER only
GET    /api/v1/student/**                    → STUDENT only

POST   /api/v1/attendance/generate           → TEACHER only
POST   /api/v1/attendance/scan               → STUDENT only
```

---

## ENVIRONMENT VARIABLES

These must be loaded from `.env` using dotenv-java and referenced in `application.properties`:

```properties
# application.properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
jwt.expiry.ms=${JWT_EXPIRY_MS}
qr.expiry.seconds=${QR_EXPIRY_SECONDS}
app.minimum.attendance.percent=${MINIMUM_ATTENDANCE_PERCENT:75}
```

```
# .env (user fills this in locally — never committed to Git)
DB_URL=jdbc:postgresql://localhost:5432/qr_attendance
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=your_256_bit_secret
JWT_EXPIRY_MS=86400000
QR_EXPIRY_SECONDS=60
MINIMUM_ATTENDANCE_PERCENT=75
```

---

## BACKGROUND JOB — ABSENT RECORD INSERTION

A `@Scheduled` method in `ScheduledJobService` must run every minute. For every `attendance_session` where `expires_at < NOW()` and `is_active = true`:

1. Set `is_active = false` on the session
2. Get all students enrolled in that session's subject + semester
   - For COMPULSORY subjects: all students where `current_semester_id = session.semester_id`
   - For ELECTIVE subjects: all students in `student_subject_enrollments` for this subject
3. For each enrolled student that does NOT have an `attendance_record` for this session:
   - INSERT `attendance_record` with `status = ABSENT`, `scanned_at = null`
4. The UNIQUE constraint prevents duplicates if the job runs twice

---

## HOW WE WORK TOGETHER — RULES FOR THIS SESSION

1. **Explain before you code** — before writing any file, tell me in one or two plain English sentences what you are about to write and why. I am a beginner and need to understand every decision.

2. **One step at a time** — complete the current step fully before moving to the next. Ask me to confirm it is working before proceeding.

3. **Always show the full file** — when creating or editing a file, show the complete file content. Never show only the changed lines.

4. **Tell me where to put every file** — always state the exact file path before showing the code.

5. **Explain error messages** — if something fails, explain what the error means in plain English before suggesting a fix.

6. **Check PROGRESS.md** — at the start of each session I will tell you which step we are on. Never redo completed steps.

7. **Never assume a step is done** — always wait for me to confirm before moving forward.

8. **Mobile responsive** — all Vaadin views must be mobile responsive. Students scan QR on phones.

9. **Consistent error responses** — all errors return this exact JSON shape:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Human readable message here",
  "path": "/api/v1/attendance/scan",
  "timestamp": "2024-09-02T09:01:05"
}
```

---

## WHAT TO DO RIGHT NOW

Check my PROGRESS.md file (I will paste it below or tell you the current step) and continue from exactly where we left off. If this is the very first session, start with Step 0 — Project Setup.

**Step 0 checklist:**
1. Generate the complete `pom.xml` with all dependencies listed above
2. Create `QrAttendanceApplication.java` entry point
3. Create `application.properties` with all environment variable references
4. Create the complete folder structure with empty placeholder files
5. Create `AppConfig.java` that loads dotenv at startup
6. Confirm the app starts without errors before we move to Step 1

---

*End of Master Project Prompt*
