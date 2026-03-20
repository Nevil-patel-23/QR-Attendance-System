package com.university.attendance.service;

import com.university.attendance.dto.request.CreateCourseRequest;
import com.university.attendance.dto.request.CreateFacultyRequest;
import com.university.attendance.dto.request.CreateStudentRequest;
import com.university.attendance.dto.request.CreateSubjectRequest;
import com.university.attendance.dto.request.CreateTeacherRequest;
import com.university.attendance.dto.response.*;
import com.university.attendance.exception.ResourceNotFoundException;
import com.university.attendance.exception.ValidationException;
import com.university.attendance.models.*;
import com.university.attendance.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    // ===== FACULTY MANAGEMENT =====

    @Transactional(readOnly = true)
    public List<FacultyResponse> getAllFaculties() {
        return facultyRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FacultyResponse createFaculty(CreateFacultyRequest request) {
        if (facultyRepository.existsByCode(request.getCode())) {
            throw new ValidationException("Faculty with code " + request.getCode() + " already exists");
        }
        if (facultyRepository.existsByName(request.getName())) {
            throw new ValidationException("Faculty with name " + request.getName() + " already exists");
        }

        Faculty faculty = Faculty.builder()
                .name(request.getName())
                .code(request.getCode())
                .build();

        return mapToResponse(facultyRepository.save(faculty));
    }

    @Transactional
    public FacultyResponse updateFaculty(UUID id, CreateFacultyRequest request) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found"));

        if (!faculty.getCode().equals(request.getCode()) && facultyRepository.existsByCode(request.getCode())) {
            throw new ValidationException("Faculty with code " + request.getCode() + " already exists");
        }
        if (!faculty.getName().equals(request.getName()) && facultyRepository.existsByName(request.getName())) {
            throw new ValidationException("Faculty with name " + request.getName() + " already exists");
        }

        faculty.setName(request.getName());
        faculty.setCode(request.getCode());

        return mapToResponse(facultyRepository.save(faculty));
    }

    @Transactional
    public void deleteFaculty(UUID id) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found"));

        Long courseCount = entityManager.createQuery(
                "SELECT COUNT(c) FROM Course c WHERE c.faculty.facultyId = :facultyId", Long.class)
                .setParameter("facultyId", id)
                .getSingleResult();

        if (courseCount > 0) {
            throw new ValidationException("Cannot delete faculty because it has associated courses");
        }

        facultyRepository.delete(faculty);
    }

    private FacultyResponse mapToResponse(Faculty faculty) {
        return FacultyResponse.builder()
                .facultyId(faculty.getFacultyId())
                .name(faculty.getName())
                .code(faculty.getCode())
                .createdAt(faculty.getCreatedAt())
                .build();
    }

    // ===== COURSE MANAGEMENT =====

    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll().stream().map(this::mapToCourseResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesByFaculty(UUID facultyId) {
        return courseRepository.findByFacultyFacultyId(facultyId).stream().map(this::mapToCourseResponse).collect(Collectors.toList());
    }

    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request) {
        if (courseRepository.existsByCode(request.getCode())) {
            throw new ValidationException("Course with code " + request.getCode() + " already exists");
        }

        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found"));

        Course course = Course.builder()
                .name(request.getName())
                .code(request.getCode())
                .durationYears(request.getDurationYears())
                .faculty(faculty)
                .build();

        Course savedCourse = courseRepository.save(course);

        // Auto-generate semesters
        int totalSemesters = request.getDurationYears() * 2;
        for (int i = 1; i <= totalSemesters; i++) {
            Semester semester = Semester.builder()
                    .course(savedCourse)
                    .semesterNumber(i)
                    .label(savedCourse.getCode() + " — Semester " + i)
                    .build();
            semesterRepository.save(semester);
        }

        return mapToCourseResponse(savedCourse);
    }

    @Transactional
    public CourseResponse updateCourse(UUID id, CreateCourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        if (!course.getCode().equals(request.getCode()) && courseRepository.existsByCode(request.getCode())) {
            throw new ValidationException("Course with code " + request.getCode() + " already exists");
        }

        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found"));

        int currentDuration = course.getDurationYears();
        int newDuration = request.getDurationYears();

        if (newDuration < currentDuration) {
            int maxAllowedSemesters = newDuration * 2;
            List<Semester> semesters = semesterRepository.findByCourseCourseId(id);
            for (Semester sem : semesters) {
                if (sem.getSemesterNumber() > maxAllowedSemesters) {
                    List<Subject> subjects = subjectRepository.findBySemesterSemesterId(sem.getSemesterId());
                    if (!subjects.isEmpty()) {
                        throw new ValidationException("Cannot reduce duration. Semester " + sem.getSemesterNumber() + " has existing subjects. Remove them first.");
                    }
                }
            }
            for (Semester sem : semesters) {
                if (sem.getSemesterNumber() > maxAllowedSemesters) {
                    semesterRepository.delete(sem);
                }
            }
        } else if (newDuration > currentDuration) {
            int startSemester = (currentDuration * 2) + 1;
            int totalSemesters = newDuration * 2;
            for (int i = startSemester; i <= totalSemesters; i++) {
                Semester semester = Semester.builder()
                        .course(course)
                        .semesterNumber(i)
                        .label(request.getCode() + " — Semester " + i)
                        .build();
                semesterRepository.save(semester);
            }
        }

        course.setName(request.getName());
        course.setCode(request.getCode());
        course.setDurationYears(request.getDurationYears());
        course.setFaculty(faculty);

        return mapToCourseResponse(courseRepository.save(course));
    }

    @Transactional
    public void deleteCourse(UUID id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        Long studentCount = entityManager.createQuery(
                "SELECT COUNT(s) FROM Student s WHERE s.course.courseId = :courseId", Long.class)
                .setParameter("courseId", id)
                .getSingleResult();

        if (studentCount > 0) {
            throw new ValidationException("Cannot delete course because it has enrolled students");
        }

        // Clean up downstream mapping safely in code if cascading is absent
        List<Semester> semesters = semesterRepository.findByCourseCourseId(id);
        for (Semester sem : semesters) {
            List<Subject> subjects = subjectRepository.findBySemesterSemesterId(sem.getSemesterId());
            subjectRepository.deleteAll(subjects);
        }
        semesterRepository.deleteAll(semesters);
        
        courseRepository.delete(course);
    }

    // ===== SEMESTER MANAGEMENT =====

    @Transactional(readOnly = true)
    public List<SemesterResponse> getSemestersByCourse(UUID courseId) {
        return semesterRepository.findByCourseCourseIdOrderBySemesterNumberAsc(courseId).stream()
                .map(this::mapToSemesterResponse).collect(Collectors.toList());
    }

    // ===== SUBJECT MANAGEMENT =====

    @Transactional(readOnly = true)
    public List<SubjectResponse> getSubjectsBySemester(UUID semesterId) {
        return subjectRepository.findBySemesterSemesterId(semesterId).stream()
                .map(this::mapToSubjectResponse).collect(Collectors.toList());
    }

    @Transactional
    public SubjectResponse createSubject(CreateSubjectRequest request) {
        if (subjectRepository.existsByCode(request.getCode())) {
            throw new ValidationException("Subject with code " + request.getCode() + " already exists");
        }

        Semester semester = semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));

        Subject subject = Subject.builder()
                .name(request.getName())
                .code(request.getCode())
                .type(request.getType())
                .credits(request.getCredits())
                .semester(semester)
                .build();

        return mapToSubjectResponse(subjectRepository.save(subject));
    }

    @Transactional
    public SubjectResponse updateSubject(UUID id, CreateSubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        if (!subject.getCode().equals(request.getCode()) && subjectRepository.existsByCode(request.getCode())) {
            throw new ValidationException("Subject with code " + request.getCode() + " already exists");
        }

        Semester semester = semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));

        subject.setName(request.getName());
        subject.setCode(request.getCode());
        subject.setType(request.getType());
        subject.setCredits(request.getCredits());
        subject.setSemester(semester);

        return mapToSubjectResponse(subjectRepository.save(subject));
    }

    @Transactional
    public void deleteSubject(UUID id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        subjectRepository.delete(subject);
    }

    // ===== STUDENT MANAGEMENT =====

    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAll().stream()
                .map(this::mapToStudentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
        if (studentRepository.existsByPrn(request.getPrn())) {
            throw new ValidationException("Student with PRN " + request.getPrn() + " already exists");
        }
        if (userRepository.findByPrn(request.getPrn()).isPresent()) {
            throw new ValidationException("User with PRN " + request.getPrn() + " already exists");
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        // Always default to semester 1 for the selected course
        Semester semester = semesterRepository.findByCourseCourseIdAndSemesterNumber(
                course.getCourseId(), 1)
                .orElseThrow(() -> new ValidationException(
                        "Course has no Semester 1. Please set up semesters first."));

        // Auto-generate password: firstName + "@" + last 4 digits of PRN
        String rawPassword = request.getFirstName() + "@" + request.getPrn().substring(request.getPrn().length() - 4);
        String hashedPassword = passwordEncoder.encode(rawPassword);

        User user = User.builder()
                .prn(request.getPrn())
                .passwordHash(hashedPassword)
                .role(Role.STUDENT)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        Student student = Student.builder()
                .user(user)
                .prn(request.getPrn())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .course(course)
                .currentSemester(semester)
                .batchYear(request.getBatchYear())
                .build();

        return mapToStudentResponse(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse updateStudent(UUID studentId, CreateStudentRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        // Update fields only — never change PRN, password, or semester
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setPhone(request.getPhone());
        student.setCourse(course);
        student.setBatchYear(request.getBatchYear());

        return mapToStudentResponse(studentRepository.save(student));
    }

    @Transactional
    public void deactivateStudent(UUID studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        User user = student.getUser();
        user.setIsActive(false);
        userRepository.save(user);
    }

    // ===== TEACHER MANAGEMENT =====

    @Transactional(readOnly = true)
    public List<TeacherResponse> getAllTeachers() {
        return teacherRepository.findAll().stream()
                .map(this::mapToTeacherResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TeacherResponse createTeacher(CreateTeacherRequest request) {
        if (teacherRepository.existsByPrn(request.getPrn())) {
            throw new ValidationException("Teacher with PRN " + request.getPrn() + " already exists");
        }
        if (userRepository.findByPrn(request.getPrn()).isPresent()) {
            throw new ValidationException("User with PRN " + request.getPrn() + " already exists");
        }

        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found"));

        // Auto-generate password: firstName + "@" + last 4 digits of PRN
        String rawPassword = request.getFirstName() + "@" + request.getPrn().substring(6);
        String hashedPassword = passwordEncoder.encode(rawPassword);

        User user = User.builder()
                .prn(request.getPrn())
                .passwordHash(hashedPassword)
                .role(Role.TEACHER)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        Teacher teacher = Teacher.builder()
                .user(user)
                .prn(request.getPrn())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .faculty(faculty)
                .designation(request.getDesignation())
                .build();

        return mapToTeacherResponse(teacherRepository.save(teacher));
    }

    @Transactional
    public TeacherResponse updateTeacher(UUID teacherId, CreateTeacherRequest request) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));

        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found"));

        // Update fields only — never change PRN or password
        teacher.setFirstName(request.getFirstName());
        teacher.setLastName(request.getLastName());
        teacher.setPhone(request.getPhone());
        teacher.setFaculty(faculty);
        teacher.setDesignation(request.getDesignation());

        return mapToTeacherResponse(teacherRepository.save(teacher));
    }

    @Transactional
    public void deactivateTeacher(UUID teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        User user = teacher.getUser();
        user.setIsActive(false);
        userRepository.save(user);
    }

    // ===== EXCEL IMPORT =====

    @Transactional
    public ExcelImportResponse importStudentsFromExcel(InputStream inputStream) {
        List<String> errors = new ArrayList<>();
        int totalRows = 0;
        int successCount = 0;

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return ExcelImportResponse.builder()
                        .totalRows(0).successCount(0).failedCount(0)
                        .errors(List.of("Excel file has no header row"))
                        .build();
            }

            Map<String, Integer> headerMap = buildHeaderMap(headerRow);

            // Validate required headers
            String[] requiredHeaders = {"prn", "firstname", "lastname", "coursecode", "semesternumber", "batchyear"};
            String[] displayHeaders = {"PRN", "First Name", "Last Name", "Course Code", "Semester Number", "Batch Year"};
            for (int i = 0; i < requiredHeaders.length; i++) {
                if (!headerMap.containsKey(requiredHeaders[i])) {
                    return ExcelImportResponse.builder()
                            .totalRows(0).successCount(0).failedCount(0)
                            .errors(List.of("Required column '" + displayHeaders[i] + "' not found in Excel file"))
                            .build();
                }
            }

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                // Skip empty trailing rows (common in Google Sheets exports)
                String prnCheck = getCellStringValue(row, headerMap.get("prn"));
                String nameCheck = getCellStringValue(row, headerMap.get("firstname"));
                if ((prnCheck == null || prnCheck.isBlank()) && (nameCheck == null || nameCheck.isBlank())) {
                    continue;
                }

                totalRows++;

                try {
                    String prn = prnCheck;
                    String firstName = nameCheck;
                    String lastName = getCellStringValue(row, headerMap.get("lastname"));
                    String courseCode = getCellStringValue(row, headerMap.get("coursecode"));
                    int semesterNumber = (int) getCellNumericValue(row, headerMap.get("semesternumber"));
                    int batchYear = (int) getCellNumericValue(row, headerMap.get("batchyear"));
                    String phone = headerMap.containsKey("phone") ? getCellStringValue(row, headerMap.get("phone")) : null;

                    if (prn == null || prn.isBlank()) {
                        errors.add("Row " + (rowIdx + 1) + ": PRN is empty");
                        continue;
                    }

                    Course course = courseRepository.findByCode(courseCode)
                            .orElseThrow(() -> new ValidationException("Course code " + courseCode + " not found"));

                    Semester semester = semesterRepository.findByCourseCourseIdAndSemesterNumber(
                            course.getCourseId(), semesterNumber)
                            .orElseThrow(() -> new ValidationException("Semester " + semesterNumber + " not found for course " + courseCode));

                    CreateStudentRequest request = new CreateStudentRequest(
                            prn, firstName, lastName, phone,
                            course.getCourseId(), batchYear
                    );
                    createStudent(request);
                    successCount++;
                } catch (Exception e) {
                    errors.add("Row " + (rowIdx + 1) + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            errors.add("Failed to read Excel file: " + e.getMessage());
        }

        return ExcelImportResponse.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .failedCount(totalRows - successCount)
                .errors(errors)
                .build();
    }

    @Transactional
    public ExcelImportResponse importTeachersFromExcel(InputStream inputStream) {
        List<String> errors = new ArrayList<>();
        int totalRows = 0;
        int successCount = 0;

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return ExcelImportResponse.builder()
                        .totalRows(0).successCount(0).failedCount(0)
                        .errors(List.of("Excel file has no header row"))
                        .build();
            }

            Map<String, Integer> headerMap = buildHeaderMap(headerRow);

            // Validate required headers
            String[] requiredHeaders = {"prn", "firstname", "lastname", "facultycode", "designation"};
            String[] displayHeaders = {"PRN", "First Name", "Last Name", "Faculty Code", "Designation"};
            for (int i = 0; i < requiredHeaders.length; i++) {
                if (!headerMap.containsKey(requiredHeaders[i])) {
                    return ExcelImportResponse.builder()
                            .totalRows(0).successCount(0).failedCount(0)
                            .errors(List.of("Required column '" + displayHeaders[i] + "' not found in Excel file"))
                            .build();
                }
            }

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                // Skip empty trailing rows (common in Google Sheets exports)
                String prnCheck = getCellStringValue(row, headerMap.get("prn"));
                String nameCheck = getCellStringValue(row, headerMap.get("firstname"));
                if ((prnCheck == null || prnCheck.isBlank()) && (nameCheck == null || nameCheck.isBlank())) {
                    continue;
                }

                totalRows++;

                try {
                    String prn = prnCheck;
                    String firstName = nameCheck;
                    String lastName = getCellStringValue(row, headerMap.get("lastname"));
                    String facultyCode = getCellStringValue(row, headerMap.get("facultycode"));
                    String designation = getCellStringValue(row, headerMap.get("designation"));
                    String phone = headerMap.containsKey("phone") ? getCellStringValue(row, headerMap.get("phone")) : null;

                    if (prn == null || prn.isBlank()) {
                        errors.add("Row " + (rowIdx + 1) + ": PRN is empty");
                        continue;
                    }

                    Faculty faculty = facultyRepository.findByCode(facultyCode)
                            .orElseThrow(() -> new ValidationException("Faculty code " + facultyCode + " not found"));

                    CreateTeacherRequest request = new CreateTeacherRequest(
                            prn, firstName, lastName, phone,
                            faculty.getFacultyId(), designation
                    );
                    createTeacher(request);
                    successCount++;
                } catch (Exception e) {
                    errors.add("Row " + (rowIdx + 1) + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            errors.add("Failed to read Excel file: " + e.getMessage());
        }

        return ExcelImportResponse.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .failedCount(totalRows - successCount)
                .errors(errors)
                .build();
    }

    // ===== EXCEL PREVIEW METHODS =====

    /**
     * Parses the Excel file and validates each row without inserting anything.
     * Returns a list of preview rows with validation status so admin can review before confirming.
     */
    @Transactional(readOnly = true)
    public List<StudentImportRow> previewStudentImport(InputStream inputStream) {
        List<StudentImportRow> rows = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                rows.add(StudentImportRow.builder().rowNumber(0).isValid(false)
                        .errorMessage("Excel file has no header row").build());
                return rows;
            }

            Map<String, Integer> headerMap = buildHeaderMap(headerRow);
            String[] requiredHeaders = {"prn", "firstname", "lastname", "coursecode", "semesternumber", "batchyear"};
            for (String h : requiredHeaders) {
                if (!headerMap.containsKey(h)) {
                    rows.add(StudentImportRow.builder().rowNumber(0).isValid(false)
                            .errorMessage("Required column '" + h + "' not found").build());
                    return rows;
                }
            }

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                String prn = getCellStringValue(row, headerMap.get("prn"));
                String firstName = getCellStringValue(row, headerMap.get("firstname"));
                if ((prn == null || prn.isBlank()) && (firstName == null || firstName.isBlank())) {
                    continue; // skip empty trailing rows
                }

                String lastName = getCellStringValue(row, headerMap.get("lastname"));
                String phone = headerMap.containsKey("phone") ? getCellStringValue(row, headerMap.get("phone")) : null;
                String courseCode = getCellStringValue(row, headerMap.get("coursecode"));
                int semesterNumber = (int) getCellNumericValue(row, headerMap.get("semesternumber"));
                int batchYear = (int) getCellNumericValue(row, headerMap.get("batchyear"));

                StudentImportRow.StudentImportRowBuilder builder = StudentImportRow.builder()
                        .rowNumber(rowIdx + 1)
                        .prn(prn).firstName(firstName).lastName(lastName).phone(phone)
                        .courseCode(courseCode).semesterNumber(semesterNumber).batchYear(batchYear);

                // Validate
                String error = null;
                if (prn == null || prn.isBlank()) {
                    error = "PRN is empty";
                } else if (studentRepository.existsByPrn(prn) || userRepository.findByPrn(prn).isPresent()) {
                    error = "PRN already exists";
                } else if (courseCode == null || courseCode.isBlank()) {
                    error = "Course code is empty";
                } else if (courseRepository.findByCode(courseCode).isEmpty()) {
                    error = "Course code " + courseCode + " not found";
                } else {
                    Optional<Course> courseOpt = courseRepository.findByCode(courseCode);
                    if (courseOpt.isPresent()) {
                        Optional<Semester> semOpt = semesterRepository.findByCourseCourseIdAndSemesterNumber(
                                courseOpt.get().getCourseId(), semesterNumber);
                        if (semOpt.isEmpty()) {
                            error = "Semester " + semesterNumber + " not found for course " + courseCode;
                        }
                    }
                }

                if (error != null) {
                    builder.isValid(false).errorMessage("Error: " + error);
                } else {
                    builder.isValid(true).errorMessage("Ready");
                }
                rows.add(builder.build());
            }
        } catch (Exception e) {
            rows.add(StudentImportRow.builder().rowNumber(0).isValid(false)
                    .errorMessage("Failed to read Excel file: " + e.getMessage()).build());
        }

        return rows;
    }

    /**
     * Imports only the valid rows from the previously previewed data.
     * Accepts the full preview list — skips invalid rows.
     */
    @Transactional
    public ExcelImportResponse confirmStudentImport(List<StudentImportRow> previewRows) {
        int successCount = 0;
        List<String> errors = new ArrayList<>();

        for (StudentImportRow row : previewRows) {
            if (!row.isValid()) continue;
            try {
                Course course = courseRepository.findByCode(row.getCourseCode())
                        .orElseThrow(() -> new ValidationException("Course not found"));

                CreateStudentRequest request = new CreateStudentRequest(
                        row.getPrn(), row.getFirstName(), row.getLastName(),
                        row.getPhone(), course.getCourseId(), row.getBatchYear()
                );
                createStudent(request);
                successCount++;
            } catch (Exception e) {
                errors.add("Row " + row.getRowNumber() + ": " + e.getMessage());
            }
        }

        long totalValid = previewRows.stream().filter(StudentImportRow::isValid).count();
        return ExcelImportResponse.builder()
                .totalRows((int) totalValid)
                .successCount(successCount)
                .failedCount((int) totalValid - successCount)
                .errors(errors)
                .build();
    }

    /**
     * Parses the teacher Excel file and validates each row without inserting anything.
     */
    @Transactional(readOnly = true)
    public List<TeacherImportRow> previewTeacherImport(InputStream inputStream) {
        List<TeacherImportRow> rows = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                rows.add(TeacherImportRow.builder().rowNumber(0).isValid(false)
                        .errorMessage("Excel file has no header row").build());
                return rows;
            }

            Map<String, Integer> headerMap = buildHeaderMap(headerRow);
            String[] requiredHeaders = {"prn", "firstname", "lastname", "facultycode", "designation"};
            for (String h : requiredHeaders) {
                if (!headerMap.containsKey(h)) {
                    rows.add(TeacherImportRow.builder().rowNumber(0).isValid(false)
                            .errorMessage("Required column '" + h + "' not found").build());
                    return rows;
                }
            }

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                String prn = getCellStringValue(row, headerMap.get("prn"));
                String firstName = getCellStringValue(row, headerMap.get("firstname"));
                if ((prn == null || prn.isBlank()) && (firstName == null || firstName.isBlank())) {
                    continue; // skip empty trailing rows
                }

                String lastName = getCellStringValue(row, headerMap.get("lastname"));
                String phone = headerMap.containsKey("phone") ? getCellStringValue(row, headerMap.get("phone")) : null;
                String facultyCode = getCellStringValue(row, headerMap.get("facultycode"));
                String designation = getCellStringValue(row, headerMap.get("designation"));

                TeacherImportRow.TeacherImportRowBuilder builder = TeacherImportRow.builder()
                        .rowNumber(rowIdx + 1)
                        .prn(prn).firstName(firstName).lastName(lastName).phone(phone)
                        .facultyCode(facultyCode).designation(designation);

                // Validate
                String error = null;
                if (prn == null || prn.isBlank()) {
                    error = "PRN is empty";
                } else if (teacherRepository.existsByPrn(prn) || userRepository.findByPrn(prn).isPresent()) {
                    error = "PRN already exists";
                } else if (facultyCode == null || facultyCode.isBlank()) {
                    error = "Faculty code is empty";
                } else if (facultyRepository.findByCode(facultyCode).isEmpty()) {
                    error = "Faculty code " + facultyCode + " not found";
                }

                if (error != null) {
                    builder.isValid(false).errorMessage("Error: " + error);
                } else {
                    builder.isValid(true).errorMessage("Ready");
                }
                rows.add(builder.build());
            }
        } catch (Exception e) {
            rows.add(TeacherImportRow.builder().rowNumber(0).isValid(false)
                    .errorMessage("Failed to read Excel file: " + e.getMessage()).build());
        }

        return rows;
    }

    /**
     * Imports only the valid rows from the previously previewed teacher data.
     */
    @Transactional
    public ExcelImportResponse confirmTeacherImport(List<TeacherImportRow> previewRows) {
        int successCount = 0;
        List<String> errors = new ArrayList<>();

        for (TeacherImportRow row : previewRows) {
            if (!row.isValid()) continue;
            try {
                Faculty faculty = facultyRepository.findByCode(row.getFacultyCode())
                        .orElseThrow(() -> new ValidationException("Faculty not found"));

                CreateTeacherRequest request = new CreateTeacherRequest(
                        row.getPrn(), row.getFirstName(), row.getLastName(),
                        row.getPhone(), faculty.getFacultyId(), row.getDesignation()
                );
                createTeacher(request);
                successCount++;
            } catch (Exception e) {
                errors.add("Row " + row.getRowNumber() + ": " + e.getMessage());
            }
        }

        long totalValid = previewRows.stream().filter(TeacherImportRow::isValid).count();
        return ExcelImportResponse.builder()
                .totalRows((int) totalValid)
                .successCount(successCount)
                .failedCount((int) totalValid - successCount)
                .errors(errors)
                .build();
    }

    // ===== EXCEL HELPER METHODS =====

    /**
     * Builds a normalized header map: strips spaces, hyphens, underscores,
     * converts to lowercase. This way "First Name", "FIRST_NAME", "first-name" all map to "firstname".
     */
    private Map<String, Integer> buildHeaderMap(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String raw = cell.getStringCellValue();
                if (raw != null) {
                    String normalized = raw.replaceAll("[\\s_-]", "").toLowerCase();
                    map.put(normalized, i);
                }
            }
        }
        return map;
    }

    private String getCellStringValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            long val = (long) cell.getNumericCellValue();
            return String.valueOf(val);
        }
        return cell.getStringCellValue() != null ? cell.getStringCellValue().trim() : null;
    }

    private double getCellNumericValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.STRING) {
            return Double.parseDouble(cell.getStringCellValue().trim());
        }
        return cell.getNumericCellValue();
    }

    // ===== MAPPERS =====

    private CourseResponse mapToCourseResponse(Course course) {
        int totalSemesters = course.getDurationYears() != null ? course.getDurationYears() * 2 : 0;
        return CourseResponse.builder()
                .courseId(course.getCourseId())
                .name(course.getName())
                .code(course.getCode())
                .durationYears(course.getDurationYears())
                .facultyName(course.getFaculty().getName())
                .totalSemesters(totalSemesters)
                .build();
    }

    private SemesterResponse mapToSemesterResponse(Semester semester) {
        return SemesterResponse.builder()
                .semesterId(semester.getSemesterId())
                .semesterNumber(semester.getSemesterNumber())
                .label(semester.getLabel())
                .courseId(semester.getCourse().getCourseId())
                .build();
    }

    private SubjectResponse mapToSubjectResponse(Subject subject) {
        return SubjectResponse.builder()
                .subjectId(subject.getSubjectId())
                .name(subject.getName())
                .code(subject.getCode())
                .type(subject.getType())
                .credits(subject.getCredits())
                .semesterId(subject.getSemester().getSemesterId())
                .build();
    }

    private StudentResponse mapToStudentResponse(Student student) {
        return StudentResponse.builder()
                .studentId(student.getStudentId())
                .prn(student.getPrn())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .phone(student.getPhone())
                .courseName(student.getCourse().getName())
                .currentSemesterLabel(student.getCurrentSemester().getLabel())
                .batchYear(student.getBatchYear())
                .isActive(student.getUser().getIsActive())
                .build();
    }

    private TeacherResponse mapToTeacherResponse(Teacher teacher) {
        return TeacherResponse.builder()
                .teacherId(teacher.getTeacherId())
                .prn(teacher.getPrn())
                .firstName(teacher.getFirstName())
                .lastName(teacher.getLastName())
                .phone(teacher.getPhone())
                .facultyName(teacher.getFaculty().getName())
                .designation(teacher.getDesignation())
                .isActive(teacher.getUser().getIsActive())
                .build();
    }
}
