# QR-Based University Attendance System

A web application that digitalizes university attendance using live expiring QR codes — replacing paper registers with a secure, real-time, mobile-responsive system built entirely in Java.

---

## What This Project Does

- Teachers generate a live QR code during their lecture that expires in seconds
- Students scan the QR on their phone to mark attendance
- Proxy attendance is prevented — a screenshot is useless once the QR expires
- Students track their attendance percentage in real time from their dashboard
- Admins manage the entire university structure — faculties, courses, semesters, subjects, timetables, and students

---

## Tech Stack

| Layer | Technology | Purpose |
|---|---|---|
| Language | Java 21 LTS | Everything is written in Java |
| Framework | Spring Boot 3 | Core application framework |
| UI | Vaadin 24 | All screens built in pure Java — no HTML/CSS/JS |
| Security | Spring Security 6 + JJWT | Login, JWT tokens, role-based access |
| Database ORM | Spring Data JPA + Hibernate | Java classes map to DB tables — no raw SQL |
| Database | PostgreSQL 16 | Stores all 15 tables |
| QR Generation | ZXing 3.5 | Generates QR code images from tokens |
| Build Tool | Maven | Manages all dependencies (Java's requirements.txt) |
| Boilerplate | Lombok | Auto-generates getters, setters, constructors |
| Excel Import | Apache POI | Reads .xlsx files for bulk data upload |
| Env Secrets | dotenv-java | Loads .env file for DB credentials and JWT secret |

---

## Three User Roles

| Role | What They Do |
|---|---|
| Admin | Manages entire university structure — faculties, courses, semesters, subjects, students, teachers, timetables, holidays, academic calendar |
| Teacher | Allocated subjects by admin. Generates live QR during lectures. Views attendance reports per subject and class. |
| Student | Logs in with university credentials. Scans QR to mark attendance. Tracks attendance percentage per subject. |

---

## Key Features

- **Expiring QR codes** — token valid for configurable seconds (default 60). After expiry all scans rejected.
- **Proxy prevention** — 5-layer validation chain on every scan (token validity, semester match, subject eligibility, duplicate check, DB unique constraint)
- **Elective subject support** — compulsory subjects auto-assigned, elective subjects require student opt-in
- **Excel bulk upload** — students, subjects, timetable, and holidays can be imported via .xlsx
- **Mobile responsive** — QR scanner works on any phone browser, no app install needed
- **Soft delete** — accounts deactivated not deleted, preserving historical attendance data
- **Background job** — ABSENT records automatically inserted after each session expires

---

## Project Scope

### V1 (This Build)
- All 26 screens across 3 roles
- Live QR generation and scanning
- Full attendance tracking and reporting
- Admin CRUD for all university data
- Excel bulk upload for students, subjects, timetable, holidays
- JWT authentication with role-based access

### V2 (Planned — Not In This Build)
- Lecture adjustments (cancel, reschedule, add extra lecture)
- Geolocation-based scan validation
- Student elective self-selection window
- Push notifications for cancelled lectures

---

## How To Run Locally

> Prerequisites: Java 21, Maven, PostgreSQL 16

1. Clone the repository
2. Copy `.env.example` to `.env` and fill in your database credentials and JWT secret
3. Create a PostgreSQL database named `qr_attendance`
4. Run `mvn spring-boot:run`
5. Open `http://localhost:8080` in your browser

---

## Documentation

All detailed documentation is in the `docs/` folder:

| File | Contents |
|---|---|
| `docs/DATABASE_DESIGN.md` | All 15 tables — columns, constraints, relationships, sample data |
| `docs/ARCHITECTURE.md` | Folder structure, layer rules, coding standards |
| `docs/SCREENS.md` | All 26 screens by role with descriptions |
| `docs/IMPLEMENTATION_PLAN.md` | Step-by-step build order |
| `docs/PROGRESS.md` | What is done, what is in progress, what is next |

---

## Security Design

- Passwords stored as BCrypt hashes only — never plain text
- JWT tokens are stateless — no server-side session storage
- QR tokens are UUID v4 (122 bits of entropy) — not guessable
- Database has UNIQUE(session_id, student_id) constraint — duplicate scans physically impossible
- PostgreSQL never exposed to internet — only the app server can connect
- `.env` file contains all secrets and is never committed to Git

---

*Built as a university project — Java 21, Spring Boot 3, Vaadin 24, PostgreSQL 16*
