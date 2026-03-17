# Database Design

**15 tables across 5 domains**  
PostgreSQL 16 — all primary keys are UUID v4 — all passwords BCrypt hashed

---

## Key Design Decisions

- **Semester numbering** — absolute numbering (1, 2, 3 ... 8 for a 4-year course). Year is always derivable as `CEIL(semester_number / 2)`. Never stored separately.
- **Subject code format** — `MCA2309` = course(MCA) + year(2) + semester(3) + subject number(09)
- **Compulsory vs elective** — compulsory subjects are inferred from `students.current_semester_id`. Only elective opt-ins are stored in `student_subject_enrollments`.
- **Attendance ABSENT records** — not written live. A `@Scheduled` background job inserts ABSENT rows for all enrolled students with no PRESENT record after each session expires.
- **Lecture adjustments** — table fully designed, deferred to V2. Base `timetable_slots` is never mutated.
- **Denormalization in attendance_sessions** — `teacher_id`, `subject_id`, `semester_id` are repeated from the allocation chain intentionally for fast report queries.

---

## Domain 1 — Academic Structure (4 tables)

### faculties

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| faculty_id | UUID | PK | Unique row identifier |
| name | VARCHAR(100) | NOT NULL, UNIQUE | Full faculty name |
| code | VARCHAR(10) | NOT NULL, UNIQUE | Short code used in subject codes e.g. CS, ENG |
| created_at | TIMESTAMP | DEFAULT NOW() | Audit trail |

**Sample data**

| faculty_id | name | code |
|---|---|---|
| fac-001 | Faculty of Computer Science | CS |
| fac-002 | Faculty of Engineering | ENG |
| fac-003 | Faculty of Business | BUS |

---

### courses

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| course_id | UUID | PK | Unique row identifier |
| faculty_id | UUID | FK → faculties, NOT NULL | Which faculty offers this course |
| name | VARCHAR(150) | NOT NULL | Full course name |
| code | VARCHAR(10) | NOT NULL, UNIQUE | Short code e.g. BCA, MCA, BTCE |
| duration_years | INT | NOT NULL, CHECK(1-6) | Drives how many semester rows to generate |
| created_at | TIMESTAMP | DEFAULT NOW() | Audit trail |

**Sample data**

| course_id | faculty_id | name | code | duration_years |
|---|---|---|---|---|
| crs-001 | fac-001 | Bachelor of Computer Applications | BCA | 3 |
| crs-002 | fac-001 | Master of Computer Applications | MCA | 2 |
| crs-003 | fac-002 | B.Tech Computer Engineering | BTCE | 4 |

---

### semesters

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| semester_id | UUID | PK | Unique row identifier |
| course_id | UUID | FK → courses, NOT NULL | Which course this semester belongs to |
| semester_number | INT | NOT NULL, CHECK(≥1) | Absolute number 1–8. Year = CEIL(semester_number / 2) |
| label | VARCHAR(30) | NOT NULL | Human-readable label e.g. BCA — Semester 5 |

> **UNIQUE(course_id, semester_number)** — prevents duplicate semester rows per course

**Sample data**

| semester_id | course_id | semester_number | label |
|---|---|---|---|
| sem-001 | crs-001 | 1 | BCA — Semester 1 |
| sem-002 | crs-001 | 2 | BCA — Semester 2 |
| sem-003 | crs-001 | 3 | BCA — Semester 3 |
| sem-004 | crs-001 | 4 | BCA — Semester 4 |
| sem-005 | crs-001 | 5 | BCA — Semester 5 |
| sem-006 | crs-001 | 6 | BCA — Semester 6 |
| sem-007 | crs-002 | 1 | MCA — Semester 1 |
| sem-008 | crs-002 | 2 | MCA — Semester 2 |
| sem-009 | crs-002 | 3 | MCA — Semester 3 |
| sem-010 | crs-002 | 4 | MCA — Semester 4 |

---

### subjects

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| subject_id | UUID | PK | Unique row identifier |
| semester_id | UUID | FK → semesters, NOT NULL | Which semester this subject belongs to |
| name | VARCHAR(150) | NOT NULL | Full subject name |
| code | VARCHAR(20) | NOT NULL, UNIQUE | University subject code e.g. MCA2309 |
| type | ENUM | COMPULSORY \| ELECTIVE, NOT NULL | Drives enrollment logic |
| credits | INT | NOT NULL, CHECK(1-6) | Credit hours |
| created_at | TIMESTAMP | DEFAULT NOW() | Audit trail |

**Subject code format:** `MCA2309` = MCA(course) + 2(year) + 3(semester) + 09(subject number)

**Sample data**

| subject_id | semester_id | name | code | type | credits |
|---|---|---|---|---|---|
| sub-001 | sem-009 | Advance Java Technologies | MCA2309 | COMPULSORY | 4 |
| sub-002 | sem-009 | Big Data Analytics | MCA2310 | COMPULSORY | 4 |
| sub-003 | sem-003 | Data Structures | BCA2301 | COMPULSORY | 4 |
| sub-004 | sem-003 | Database Management | BCA2302 | COMPULSORY | 4 |
| sub-005 | sem-003 | Web Development | BCA2303E | ELECTIVE | 3 |
| sub-006 | sem-003 | Mobile App Dev | BCA2304E | ELECTIVE | 3 |

---

## Domain 2 — User Management (3 tables)

### users

> **Login strategy:** Admin logs in with username + password. Teachers and students log in with PRN (10 digits) + password.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| user_id | UUID | PK | Single auth identity for all roles |
| prn | VARCHAR(10) | UNIQUE, NULLABLE, CHECK(LENGTH=10) | 10-digit Permanent Registration Number — login credential for teachers and students. NULL for Admin. |
| username | VARCHAR(50) | NOT NULL, UNIQUE | Login credential for Admin. For teachers/students, auto-set to their PRN. |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt hashed — raw password NEVER stored |
| role | ENUM | ADMIN \| TEACHER \| STUDENT, NOT NULL | Controls Spring Security endpoint access |
| is_active | BOOLEAN | DEFAULT TRUE | Soft disable — blocks login without deleting data |
| created_at | TIMESTAMP | DEFAULT NOW() | Audit trail |
| updated_at | TIMESTAMP | DEFAULT NOW() | Tracks last profile or password change |

**Sample data**

| user_id | prn | username | password_hash | role | is_active |
|---|---|---|---|---|---|
| usr-001 | NULL | superadmin | $2a$12$hK9mN... | ADMIN | true |
| usr-002 | 1234567801 | 1234567801 | $2a$12$xP3qL... | TEACHER | true |
| usr-003 | 2212345001 | 2212345001 | $2a$12$dW7nM... | STUDENT | true |
| usr-004 | 2212345002 | 2212345002 | $2a$12$eR2kJ... | STUDENT | true |

---

### students

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| student_id | UUID | PK | Unique row identifier |
| user_id | UUID | FK → users, UNIQUE, NOT NULL | 1:1 link to login credentials |
| prn | VARCHAR(10) | NOT NULL, UNIQUE, CHECK(LENGTH=10) | 10-digit Permanent Registration Number |
| first_name | VARCHAR(80) | NOT NULL | Student first name |
| last_name | VARCHAR(80) | NOT NULL | Student last name |
| phone | VARCHAR(15) | UNIQUE, NULLABLE | Contact number |
| course_id | UUID | FK → courses, NOT NULL | Which course the student is enrolled in |
| current_semester_id | UUID | FK → semesters, NOT NULL | Updated on promotion — drives timetable and subject display |
| batch_year | INT | NOT NULL | Year of admission e.g. 2022 |
| created_at | TIMESTAMP | DEFAULT NOW() | Audit trail |

**Sample data**

| student_id | user_id | prn | first_name | last_name | course_id | current_semester_id | batch_year |
|---|---|---|---|---|---|---|---|
| std-001 | usr-003 | 2212345001 | Rohan | Sharma | crs-001 | sem-003 | 2022 |
| std-002 | usr-004 | 2212345002 | Priya | Patel | crs-002 | sem-009 | 2022 |

---

### teachers

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| teacher_id | UUID | PK | Unique row identifier |
| user_id | UUID | FK → users, UNIQUE, NOT NULL | 1:1 link to login credentials |
| prn | VARCHAR(10) | NOT NULL, UNIQUE, CHECK(LENGTH=10) | 10-digit Permanent Registration Number |
| first_name | VARCHAR(80) | NOT NULL | Teacher first name |
| last_name | VARCHAR(80) | NOT NULL | Teacher last name |
| phone | VARCHAR(15) | UNIQUE, NULLABLE | Contact number |
| faculty_id | UUID | FK → faculties, NOT NULL | Primary faculty the teacher belongs to |
| designation | VARCHAR(100) | NOT NULL | e.g. Assistant Professor, Associate Professor |
| created_at | TIMESTAMP | DEFAULT NOW() | Audit trail |

**Sample data**

| teacher_id | user_id | prn | first_name | last_name | faculty_id | designation |
|---|---|---|---|---|---|---|
| tch-001 | usr-002 | 1234567801 | Rajesh | Mehta | fac-001 | Associate Professor |
| tch-002 | usr-005 | 1234567802 | Sunita | Joshi | fac-001 | Assistant Professor |

---

## Domain 3 — Enrollment & Allocation (2 tables)

### student_subject_enrollments

> Only stores ELECTIVE opt-ins. Compulsory subjects are inferred from `students.current_semester_id`.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| enrollment_id | UUID | PK | Unique row identifier |
| student_id | UUID | FK → students, NOT NULL | Which student opted in |
| subject_id | UUID | FK → subjects, NOT NULL | Must be type=ELECTIVE — enforced at service layer |
| academic_year | VARCHAR(10) | NOT NULL | e.g. 2024-25 |
| enrolled_at | TIMESTAMP | DEFAULT NOW() | When the student opted for the elective |

> **UNIQUE(student_id, subject_id, academic_year)** — prevents double opt-in

**Sample data**

| enrollment_id | student_id | subject_id | academic_year |
|---|---|---|---|
| enr-001 | std-001 | sub-005 | 2024-25 |
| enr-002 | std-002 | sub-006 | 2024-25 |

---

### teacher_subject_allocations

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| allocation_id | UUID | PK | Unique row identifier |
| teacher_id | UUID | FK → teachers, NOT NULL | Which teacher is assigned |
| subject_id | UUID | FK → subjects, NOT NULL | Which subject they teach |
| semester_id | UUID | FK → semesters, NOT NULL | Which semester batch they teach |
| academic_year | VARCHAR(10) | NOT NULL | e.g. 2024-25 |
| created_at | TIMESTAMP | DEFAULT NOW() | Audit trail |

> **UNIQUE(teacher_id, subject_id, semester_id, academic_year)** — prevents double assignment

**Sample data**

| allocation_id | teacher_id | subject_id | semester_id | academic_year |
|---|---|---|---|---|
| alc-001 | tch-001 | sub-001 | sem-009 | 2024-25 |
| alc-002 | tch-002 | sub-002 | sem-009 | 2024-25 |
| alc-003 | tch-001 | sub-003 | sem-003 | 2024-25 |

---

## Domain 4 — Calendar & Timetable (4 tables)

### academic_calendars

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| calendar_id | UUID | PK | Unique row identifier |
| course_id | UUID | FK → courses, NOT NULL | Different courses may have different semester dates |
| academic_year | VARCHAR(10) | NOT NULL | e.g. 2024-25 |
| semester_number | INT | NOT NULL, CHECK(1 or 2) | Which half of the year (odd=1, even=2) |
| start_date | DATE | NOT NULL | First teaching day — used for attendance % |
| end_date | DATE | NOT NULL | Last teaching day — attendance locked after this |
| created_at | TIMESTAMP | DEFAULT NOW() | Audit trail |

> **UNIQUE(course_id, academic_year, semester_number)**

**Sample data**

| calendar_id | course_id | academic_year | semester_number | start_date | end_date |
|---|---|---|---|---|---|
| cal-001 | crs-001 | 2024-25 | 1 | 2024-07-01 | 2024-11-30 |
| cal-002 | crs-001 | 2024-25 | 2 | 2025-01-06 | 2025-05-31 |
| cal-003 | crs-002 | 2024-25 | 1 | 2024-07-08 | 2024-11-30 |

---

### holidays

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| holiday_id | UUID | PK | Unique row identifier |
| name | VARCHAR(100) | NOT NULL | Holiday name shown in calendar |
| date | DATE | NOT NULL, UNIQUE | Exact holiday date — system skips attendance on this day |
| type | ENUM | NATIONAL \| UNIVERSITY \| REGIONAL | Category for display |
| created_at | TIMESTAMP | DEFAULT NOW() | Audit trail |

**Sample data**

| holiday_id | name | date | type |
|---|---|---|---|
| hol-001 | Independence Day | 2024-08-15 | NATIONAL |
| hol-002 | Diwali | 2024-11-01 | NATIONAL |
| hol-003 | University Foundation Day | 2024-09-20 | UNIVERSITY |

---

### timetable_slots

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| slot_id | UUID | PK | Unique row identifier |
| allocation_id | UUID | FK → teacher_subject_allocations, NOT NULL | Who teaches what to whom |
| day_of_week | ENUM | MON\|TUE\|WED\|THU\|FRI\|SAT, NOT NULL | Which day this slot recurs |
| start_time | TIME | NOT NULL | Lecture start time |
| end_time | TIME | NOT NULL | Lecture end time |
| room | VARCHAR(20) | NOT NULL | Classroom or lab identifier |
| effective_from | DATE | NOT NULL | Semester start date |
| effective_to | DATE | NULLABLE | NULL = ongoing. Set when slot is replaced. |

> Service layer enforces no teacher double-booking — same teacher cannot have two slots with overlapping day+time.

**Sample data**

| slot_id | allocation_id | day_of_week | start_time | end_time | room | effective_from |
|---|---|---|---|---|---|---|
| slt-001 | alc-001 | MON | 09:00 | 10:00 | A-101 | 2024-07-01 |
| slt-002 | alc-002 | MON | 10:00 | 11:00 | LAB-1 | 2024-07-01 |
| slt-003 | alc-001 | WED | 09:00 | 10:00 | A-101 | 2024-07-01 |

---

### lecture_adjustments ⭐ V2 — DEFERRED

> Fully designed, not built in V1. The base `timetable_slots` table is NEVER mutated — all overrides are stored here as delta rows.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| adjustment_id | UUID | PK | Unique row identifier |
| slot_id | UUID | FK → timetable_slots, NULLABLE | Null only for EXTRA type |
| teacher_id | UUID | FK → teachers, NOT NULL | Who made the adjustment |
| original_date | DATE | NULLABLE | Date being cancelled/rescheduled |
| new_date | DATE | NULLABLE | Null for CANCELLED |
| new_start_time | TIME | NULLABLE | Null if CANCELLED |
| new_end_time | TIME | NULLABLE | Null if CANCELLED |
| new_room | VARCHAR(20) | NULLABLE | Null if CANCELLED or same room |
| type | ENUM | CANCELLED \| RESCHEDULED \| EXTRA, NOT NULL | Type of override |
| reason | VARCHAR(255) | NULLABLE | Optional note shown to students |
| created_at | TIMESTAMP | DEFAULT NOW() | Audit trail |

---

## Domain 5 — Attendance (2 tables)

### attendance_sessions

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| session_id | UUID | PK | Unique row identifier |
| allocation_id | UUID | FK → teacher_subject_allocations, NOT NULL | Teacher+subject+semester context |
| slot_id | UUID | FK → timetable_slots, NULLABLE | Null for extra sessions |
| teacher_id | UUID | FK → teachers, NOT NULL | Denormalized for fast queries |
| subject_id | UUID | FK → subjects, NOT NULL | Denormalized for report queries |
| semester_id | UUID | FK → semesters, NOT NULL | Scope which students are eligible |
| session_date | DATE | NOT NULL | Actual lecture date |
| qr_token | VARCHAR(36) | NOT NULL, UNIQUE | UUID v4 — 122 bits entropy — what the QR encodes |
| generated_at | TIMESTAMP | NOT NULL | When teacher clicked Generate QR |
| expires_at | TIMESTAMP | NOT NULL | generated_at + QR_EXPIRY_SECONDS |
| is_active | BOOLEAN | DEFAULT TRUE | Flipped false by @Scheduled job after expiry |

**Sample data**

| session_id | teacher_id | subject_id | semester_id | session_date | qr_token | generated_at | expires_at | is_active |
|---|---|---|---|---|---|---|---|---|
| ses-001 | tch-001 | sub-001 | sem-009 | 2024-09-02 | 550e8400-e29b | 09:01:05 | 09:02:05 | false |
| ses-002 | tch-002 | sub-002 | sem-009 | 2024-09-02 | 6ba7b810-9dad | 10:00:58 | 10:01:58 | false |

---

### attendance_records

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| record_id | UUID | PK | Unique row identifier |
| session_id | UUID | FK → attendance_sessions, NOT NULL | Which QR session |
| student_id | UUID | FK → students, NOT NULL | Which student |
| scanned_at | TIMESTAMP | NULLABLE | Null for ABSENT records inserted by background job |
| status | ENUM | PRESENT \| ABSENT, DEFAULT 'PRESENT' | PRESENT = scanned live. ABSENT = inserted by @Scheduled job post-expiry |

> **UNIQUE(session_id, student_id)** — one student can only have one record per session. Makes duplicate scans physically impossible even under race conditions at 50+ concurrent students.

**Sample data**

| record_id | session_id | student_id | scanned_at | status |
|---|---|---|---|---|
| rec-001 | ses-001 | std-001 | 2024-09-02 09:01:18 | PRESENT |
| rec-002 | ses-001 | std-002 | 2024-09-02 09:01:33 | PRESENT |
| rec-003 | ses-001 | std-003 | null | ABSENT |
| rec-004 | ses-002 | std-001 | 2024-09-02 10:01:10 | PRESENT |
| rec-005 | ses-002 | std-003 | null | ABSENT |

---

## QR Scan Validation Chain

Every scan request goes through these 5 checks in order. All 5 must pass:

```
1. Token valid + not expired
   → SELECT from attendance_sessions WHERE qr_token = ? AND is_active = true AND expires_at > NOW()
   → FAIL: REJECT — token expired or invalid

2. Semester match
   → session.semester_id == student.current_semester_id
   → FAIL: REJECT — student is not in this class

3. Subject eligibility
   → If COMPULSORY: subject.semester_id == student.current_semester_id
   → If ELECTIVE: row exists in student_subject_enrollments
   → FAIL: REJECT — student has not enrolled in this subject

4. Duplicate scan check
   → SELECT from attendance_records WHERE session_id = ? AND student_id = ?
   → FAIL: REJECT — student already marked present

5. Database unique constraint (race condition safety net)
   → UNIQUE(session_id, student_id) on attendance_records
   → FAIL: REJECT — concurrent duplicate blocked at DB level

→ ALL PASS: INSERT attendance_record with status = PRESENT
```

---

## Security Summary

| Concern | Solution |
|---|---|
| Password storage | BCrypt hash only — plain password never stored |
| Proxy attendance | 5-check validation chain on every scan |
| QR screenshot sharing | UUID v4 token + expires in seconds |
| Duplicate scan race condition | UNIQUE(session_id, student_id) at DB level |
| Wrong subject scan | Check 3 — compulsory inference + elective enrollment check |
| Disabled account | is_active = false blocks at Spring Security level |
| Database exposed | PostgreSQL only accessible from app server — never open to internet |
| Secrets in code | .env file — never committed to Git |
