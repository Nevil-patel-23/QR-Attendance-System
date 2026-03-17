# Architecture

## Folder Structure

```
qr-attendance/
├── .env                          ← DB password, JWT secret — NEVER on GitHub
├── .env.example                  ← safe template showing required keys (no real values)
├── .gitignore                    ← tells Git what to ignore
├── pom.xml                       ← all dependencies (Java's version of requirements.txt)
├── README.md                     ← project overview and how to run

├── docs/
│   ├── DATABASE_DESIGN.md        ← all 15 tables, columns, sample data
│   ├── ARCHITECTURE.md           ← this file
│   ├── SCREENS.md                ← all 26 screens by role
│   ├── IMPLEMENTATION_PLAN.md    ← step-by-step build order
│   └── PROGRESS.md               ← what is done, in progress, next

└── src/
    ├── main/
    │   ├── java/com/university/attendance/
    │   │   │
    │   │   ├── models/           ← Java classes that map to DB tables (14 files)
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
    │   │   ├── repository/       ← interfaces that query the database
    │   │   │   ├── UserRepository.java
    │   │   │   ├── StudentRepository.java
    │   │   │   ├── TeacherRepository.java
    │   │   │   ├── FacultyRepository.java
    │   │   │   ├── CourseRepository.java
    │   │   │   ├── SemesterRepository.java
    │   │   │   ├── SubjectRepository.java
    │   │   │   ├── AcademicCalendarRepository.java
    │   │   │   ├── HolidayRepository.java
    │   │   │   ├── TimetableSlotRepository.java
    │   │   │   ├── TeacherSubjectAllocationRepository.java
    │   │   │   ├── StudentSubjectEnrollmentRepository.java
    │   │   │   ├── AttendanceSessionRepository.java
    │   │   │   └── AttendanceRecordRepository.java
    │   │   │
    │   │   ├── service/          ← ALL business logic lives here
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
    │   │   ├── controller/       ← receives HTTP requests, calls service, returns DTO
    │   │   │   ├── AuthController.java
    │   │   │   ├── AdminController.java
    │   │   │   ├── TeacherController.java
    │   │   │   ├── StudentController.java
    │   │   │   └── AttendanceController.java
    │   │   │
    │   │   ├── dto/              ← data shapes that travel between layers
    │   │   │   ├── request/      ← what comes IN from the UI
    │   │   │   └── response/     ← what goes OUT to the UI
    │   │   │
    │   │   ├── security/         ← JWT filter, Spring Security config
    │   │   │   ├── JwtFilter.java
    │   │   │   ├── JwtUtil.java
    │   │   │   ├── SecurityConfig.java
    │   │   │   └── UserDetailsServiceImpl.java
    │   │   │
    │   │   ├── exception/        ← custom error types and global handler
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   └── ValidationException.java
    │   │   │
    │   │   ├── config/           ← app-level configuration beans
    │   │   │   ├── AppConfig.java
    │   │   │   └── PasswordEncoderConfig.java
    │   │   │
    │   │   ├── ui/               ← all Vaadin screens — pure Java, no HTML/CSS
    │   │   │   ├── views/
    │   │   │   │   ├── admin/    ← 13 admin screens
    │   │   │   │   ├── teacher/  ← 6 teacher screens
    │   │   │   │   ├── student/  ← 5 student screens
    │   │   │   │   └── shared/   ← login + profile (2 screens)
    │   │   │   └── components/   ← reusable UI pieces (tables, forms, dialogs)
    │   │   │
    │   │   └── QrAttendanceApplication.java   ← app entry point — main() lives here
    │   │
    │   └── resources/
    │       ├── application.properties          ← safe config — on GitHub
    │       ├── application-local.properties    ← real secrets — in .gitignore
    │       └── db/migration/
    │           ├── V1__create_tables.sql       ← creates all 15 tables
    │           └── V2__seed_data.sql           ← optional seed data
    │
    └── test/                     ← mirrors main structure for unit + integration tests
```

---

## The Layer System — What Each Layer Does

Think of a request flowing top to bottom, and a response flowing back up.

```
UI (Vaadin screen)
      ↓  HTTP request
Security (JWT filter checks token first — before anything else)
      ↓  passes if valid
Controller (receives request, calls service, returns DTO)
      ↓
Service (all business logic — QR validation, attendance %, etc.)
      ↓
Repository (talks to database using method names)
      ↓
Database (PostgreSQL — stores everything)
```

### models/ (bottom layer)
Pure Java classes representing database tables. One class = one table. Fields = columns. No logic, no methods beyond getters/setters. Spring reads these and creates/manages the DB tables automatically.

### repository/
Java interfaces. You write method names like `findByEmail(String email)` or `findByQrTokenAndIsActiveTrue(String token)` and Spring Data automatically generates the SQL. You almost never write raw SQL.

### service/ (the heart)
Where all thinking happens. QR token generation, the 5-check scan validation chain, attendance percentage calculation, Excel file parsing, the @Scheduled ABSENT insertion job. Nothing else should contain business logic.

### controller/
Thin layer that receives HTTP requests from the UI. Calls the right service method. Returns a response DTO. Controllers should be dumb — they just route traffic. No business logic, no direct repository calls.

### dto/
Data Transfer Objects. Shields the UI from raw database entities. A `StudentResponseDto` contains only safe fields — never password_hash. A `QrScanRequestDto` contains only the token string. DTOs are the formal contract between UI and backend.

### security/
Runs before every request. `JwtFilter` validates the token, extracts user role. `SecurityConfig` declares which endpoints need which role. Spring Security rejects unauthenticated/unauthorised requests before they reach any controller.

### ui/ (top layer — Vaadin)
All 26 screens written in Java. `AdminDashboardView.java`, `LiveQrView.java`, `QrScannerView.java` etc. These are Java classes that use Vaadin components (Button, Grid, TextField etc.). Vaadin renders them as browser pages. No HTML, no CSS, no JavaScript files needed.

### exception/
`GlobalExceptionHandler` is annotated with `@ControllerAdvice` — it catches every exception from every layer and always returns the same clean JSON error shape: `{ status, error, message, path, timestamp }`. Without this, Spring returns ugly HTML error pages.

---

## Rules the Agent Must Follow

### Must Do
- Controllers only call services — never repositories directly
- Services contain all business logic — no logic in controllers or models
- Controllers always return DTOs — never raw model/entity objects
- Every endpoint protected by role — `@PreAuthorize("hasRole('ADMIN')")` etc.
- All DB-writing service methods annotated with `@Transactional`
- Passwords always hashed with BCrypt — never stored or logged plain
- Every model uses UUID as primary key — never auto-increment integers
- Vaadin views in `ui/views/` only — never mixed into other packages
- All API endpoints prefixed `/api/v1/` for versioning
- Secrets loaded from `.env` via dotenv-java — never hardcoded

### Must Not Do
- Write raw SQL queries — use Spring Data JPA methods or JPQL only
- Put business logic in a Vaadin view class — views call services only
- Return model/entity objects directly from controllers — always map to DTO first
- Create HTML, CSS, or JavaScript files — Vaadin handles all UI in Java
- Use integer auto-increment IDs — UUIDs only
- Skip `@Transactional` on methods that write to the database
- Hardcode DB password or JWT secret — use `.env` via application.properties

---

## How a Feature Touches Every Layer

Example: Teacher clicks "Generate QR" on the Live QR screen

```
1. ui/views/teacher/LiveQrView.java
   Button click fires POST request

2. security/JwtFilter.java
   Checks JWT token is valid and role is TEACHER

3. controller/AttendanceController.java
   Receives POST /api/v1/attendance/generate
   Calls qrService.generateSession(allocationId)

4. service/QrService.java
   Generates UUID v4 token
   Sets expires_at = NOW() + QR_EXPIRY_SECONDS
   Calls repository to save

5. repository/AttendanceSessionRepository.java
   Saves new AttendanceSession row to PostgreSQL

6. models/AttendanceSession.java
   The data shape that gets saved

7. dto/response/QrResponseDto.java
   Wraps token + expiry into safe response

8. Back to LiveQrView.java
   Renders QR image using ZXing
   Starts countdown timer
   Shows live scan counter
```

---

## Environment Variables (.env)

```properties
# Database
DB_URL=jdbc:postgresql://localhost:5432/qr_attendance
DB_USERNAME=postgres
DB_PASSWORD=your_password_here

# JWT
JWT_SECRET=your_256_bit_random_secret_here
JWT_EXPIRY_MS=86400000

# QR Settings
QR_EXPIRY_SECONDS=60
```

These are loaded into `application.properties` using dotenv-java. The `.env` file is never committed to Git.
