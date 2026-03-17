# Screens

**26 total screens across 3 roles + shared**

| Count | Role |
|---|---|
| 2 | Shared (all roles) |
| 13 | Admin |
| 6 | Teacher |
| 5 | Student |

Legend: `[CRUD]` = Create, Read, Update, Delete  `[Excel]` = bulk upload supported  `[V2]` = deferred to version 2  `[Core]` = most important feature

---

## Shared Screens (2)

### S1 — Login Screen
- University email + password form
- JWT token issued on successful login
- Automatically redirects to role-specific dashboard (Admin → A1, Teacher → T1, Student → ST1)
- Single login page for all three roles — backend detects role from credentials
- Shows error message for invalid credentials or deactivated accounts

### S2 — My Profile Screen
- View your own name, email, role
- Students see: enrollment number, course, current semester, batch year
- Teachers see: employee ID, faculty, designation
- All roles: change password form (old password + new password + confirm)
- Email and role are admin-managed — not editable here

---

## Admin Screens (13)

### A1 — Admin Dashboard `[Home]`
- Overview stat cards: total faculties, courses, students, teachers
- Cards for: active academic year, current semester period
- Quick navigation links to all admin sections
- No data entry on this screen — pure navigation hub

### A2 — Faculty Management `[CRUD]`
- List of all faculties in a table (name, code, number of courses)
- Add new faculty via inline form — name and code fields only
- Edit and delete per row
- Cannot delete a faculty that has courses assigned to it
- Foundation of everything — built first

### A3 — Course Management `[CRUD]` `[Excel]`
- List of all courses with faculty name, code, duration
- Filter by faculty
- Add/edit/delete course — name, code, duration_years, faculty
- Excel upload for bulk import of courses
- Clicking a course shows its auto-generated semester list based on duration_years

### A4 — Subject Management `[CRUD]` `[Excel]`
- Filter by course → then by semester to see subjects
- Add/edit/delete subjects — name, code, type (COMPULSORY/ELECTIVE), credits
- Subject code follows format: `MCA2309` (course + year + semester + number)
- Excel upload for bulk subject setup at semester start
- This is where MCA2309 (Advance Java Technologies) and MCA2310 (Big Data Analytics) are added

### A5 — Academic Calendar Management `[CRUD]`
- Set semester start and end dates per course per academic year
- Form: course dropdown + academic year + semester (odd/even) + start date + end date
- Critical — attendance % calculation depends on these dates
- List view shows all calendars filterable by course and year

### A6 — Holiday Management `[CRUD]` `[Excel]`
- List of all holidays for the academic year
- Add holiday: name, date, type (NATIONAL/UNIVERSITY/REGIONAL)
- Delete holidays
- Excel upload for importing full year holiday list in one shot
- System skips QR generation and attendance calculation on holiday dates

### A7 — Student Management `[CRUD]` `[Excel]` `[Most Used]`
- List all students — filterable by course, semester, batch year
- Search by name or enrollment number
- Add individual student: creates User + Student records together in one action
- **Excel upload** — most important Excel feature. Bulk onboarding 200+ students at semester start
- Excel columns: enrollment_no, first_name, last_name, email, phone, course_code, batch_year
- Edit student: can update current_semester_id for semester promotion
- Soft-deactivate (is_active = false) instead of hard delete — preserves attendance history
- View any student's full attendance record

### A8 — Teacher Management `[CRUD]`
- List all teachers with employee ID, faculty, designation
- Add teacher: creates User + Teacher records together
- Edit teacher details and designation
- Soft-deactivate instead of delete
- No Excel upload needed — teachers are fewer in number

### A9 — Elective Enrollment Management `[CRUD]`
- View which students have opted for which elective subjects
- Filter by semester + subject
- Admin can manually assign or remove elective enrollments (edge cases e.g. student missed window)
- In V2 this becomes self-service for students with an admin-controlled opt-in window

### A10 — Teacher-Subject Allocation `[CRUD]`
- Assign teachers to subjects for each semester per academic year
- Form: pick teacher → pick subject → pick semester → pick academic year
- System prevents duplicate allocation (same teacher, same subject, same semester, same year)
- View existing allocations in filterable table
- Done at the start of each semester before timetable is built

### A11 — Timetable Builder `[CRUD]` `[Excel]`
- Build the weekly recurring timetable
- Form: pick allocation (teacher+subject+semester) → pick day → start time → end time → room
- System warns if the selected teacher is already scheduled at that time (double-booking check)
- set effective_from = semester start date
- **Excel upload** — most practical since coordinators usually prepare timetables in Excel already
- Excel columns: teacher_employee_id, subject_code, semester_label, day, start_time, end_time, room
- View full weekly timetable per semester

### A12 — Attendance Overview Report `[Read Only]`
- University-wide attendance view
- Filters: faculty → course → semester → subject → date range
- Table: each student row shows present count, total classes, percentage per subject
- Highlight students below minimum attendance threshold (e.g. below 75% shown in red)
- Export filtered results to Excel

### A13 — Student Elective Selection Window `[V2]`
- Admin controls the window during which students can self-select elective subjects
- Open and close the elective selection period per semester
- In V1 admin manually assigns electives via A9

---

## Teacher Screens (6)

### T1 — Teacher Dashboard `[Home]`
- Today's timetable slots shown prominently
- Each slot has a "Generate QR" button — active only during the lecture's scheduled time window
- Quick stats: total lectures this week, average attendance % per subject
- Upcoming schedule for rest of the week
- Respects holidays — no slots shown on holiday dates

### T2 — Live QR Screen `[Core Feature]`
- Full-screen QR code display — large enough to photograph or project
- Large countdown timer showing seconds remaining (e.g. 00:45)
- Live counter: "X students have scanned" — updates in real time
- Auto-closes when timer hits zero — shows session summary (total present, total absent)
- Teacher can project this on classroom screen or hold up their laptop/phone
- QR token is UUID v4, expires at configured seconds (default 60)

### T3 — Attendance Report by Subject `[Read Only]`
- Pick one of the teacher's allocated subjects
- Table: each student row × each lecture date — shows P (present) or A (absent) per cell
- Last column shows overall attendance % per student
- Filter by date range
- Shows ONLY subjects this teacher is allocated to — no access to other teachers' data

### T4 — Attendance Report by Session `[Read Only]`
- Pick a specific lecture session (date + subject)
- Full class list with PRESENT or ABSENT per student
- Shows exact scanned_at timestamp for each present student
- Useful right after a lecture to confirm all scans registered correctly

### T5 — My Timetable `[Read Only]`
- Weekly view of all slots across all subjects and semesters this teacher teaches
- Shows day, time, room, subject name, semester label for each slot
- Respects holidays
- Read-only in V1

### T6 — Lecture Adjustment Screen `[V2]`
- Cancel, reschedule, or add an extra lecture
- Writes to lecture_adjustments table
- Notifies affected students
- Deferred to V2 as planned — lecture_adjustments table is designed and ready

---

## Student Screens (5)

### ST1 — Student Dashboard `[Home]`
- Attendance summary cards — one card per subject
- Each card shows: subject name, code, lectures attended / total, percentage
- Color indicator: green if above threshold (≥75%), red if below
- At-risk warning banner if any subject drops below minimum
- Today's timetable below the cards
- Profile summary strip: name, enrollment number, course, current semester

### ST2 — QR Scanner `[Core Feature]`
- Opens device camera via browser — no app install needed
- Scans QR code displayed by teacher
- Sends token to server
- Instant feedback:
  - Green tick + "Attendance marked" — PRESENT recorded
  - Red cross + reason — rejected (expired / wrong class / not enrolled / already scanned)
- Works on any mobile browser (Chrome, Safari etc.)
- Student must be logged in — JWT token links the scan to their identity

### ST3 — My Attendance Full Detail `[Read Only]`
- Select a subject from dropdown
- Table: each lecture date — PRESENT or ABSENT
- Running attendance % shown at top
- Shows exactly which lectures were missed
- Helps student calculate how many more absences they can have before hitting the threshold
- Formula shown: `Required to attend = CEIL(total_classes × 0.75)`

### ST4 — My Timetable `[Read Only]`
- Weekly schedule based on student's current_semester_id
- Shows all subjects — compulsory + elected electives
- Each slot: subject name, time, room, teacher name
- Respects holidays — no classes shown on holiday dates

### ST5 — My Subjects `[Read Only]`
- List of all subjects for the current semester
- Compulsory subjects section + Elected Electives section (separate)
- Each subject: name, code, credits, teacher name
- In V2 this screen gets an "Opt into elective" button during the admin-controlled window

---

## Screen Count Summary

| Category | Count | Notes |
|---|---|---|
| Shared | 2 | Login + Profile |
| Admin — Structure setup | 6 | A1–A6 |
| Admin — People management | 3 | A7–A9 |
| Admin — Timetable | 2 | A10–A11 |
| Admin — Reports | 1 | A12 |
| Admin — V2 | 1 | A13 |
| Teacher — Core | 5 | T1–T5 |
| Teacher — V2 | 1 | T6 |
| Student | 5 | ST1–ST5 |
| **Total** | **26** | |

## Excel Upload Screens

| Screen | What the Excel imports |
|---|---|
| A3 — Course Management | Bulk course list |
| A4 — Subject Management | Full semester subject list |
| A6 — Holiday Management | Full year holiday list |
| A7 — Student Management | Bulk student onboarding (most important) |
| A11 — Timetable Builder | Full weekly timetable |
| A12 — Attendance Report | Export only (not import) |
