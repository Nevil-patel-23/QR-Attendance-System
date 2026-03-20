package com.university.attendance.controller;

import com.university.attendance.dto.request.CreateCourseRequest;
import com.university.attendance.dto.request.CreateFacultyRequest;
import com.university.attendance.dto.request.CreateStudentRequest;
import com.university.attendance.dto.request.CreateSubjectRequest;
import com.university.attendance.dto.request.CreateTeacherRequest;
import com.university.attendance.dto.response.*;
import com.university.attendance.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ===== FACULTY =====

    @GetMapping("/faculties")
    public ResponseEntity<List<FacultyResponse>> getAllFaculties() {
        return ResponseEntity.ok(adminService.getAllFaculties());
    }

    @PostMapping("/faculties")
    public ResponseEntity<FacultyResponse> createFaculty(@Valid @RequestBody CreateFacultyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createFaculty(request));
    }

    @PutMapping("/faculties/{id}")
    public ResponseEntity<FacultyResponse> updateFaculty(@PathVariable UUID id, @Valid @RequestBody CreateFacultyRequest request) {
        return ResponseEntity.ok(adminService.updateFaculty(id, request));
    }

    @DeleteMapping("/faculties/{id}")
    public ResponseEntity<Void> deleteFaculty(@PathVariable UUID id) {
        adminService.deleteFaculty(id);
        return ResponseEntity.noContent().build();
    }

    // ===== COURSE =====

    @GetMapping("/courses")
    public ResponseEntity<List<CourseResponse>> getAllCourses() {
        return ResponseEntity.ok(adminService.getAllCourses());
    }

    @GetMapping("/courses/faculty/{facultyId}")
    public ResponseEntity<List<CourseResponse>> getCoursesByFaculty(@PathVariable UUID facultyId) {
        return ResponseEntity.ok(adminService.getCoursesByFaculty(facultyId));
    }

    @PostMapping("/courses")
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createCourse(request));
    }

    @PutMapping("/courses/{id}")
    public ResponseEntity<CourseResponse> updateCourse(@PathVariable UUID id, @Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.ok(adminService.updateCourse(id, request));
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable UUID id) {
        adminService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    // ===== SEMESTER =====

    @GetMapping("/semesters/course/{courseId}")
    public ResponseEntity<List<SemesterResponse>> getSemestersByCourse(@PathVariable UUID courseId) {
        return ResponseEntity.ok(adminService.getSemestersByCourse(courseId));
    }

    // ===== SUBJECT =====

    @GetMapping("/subjects/semester/{semesterId}")
    public ResponseEntity<List<SubjectResponse>> getSubjectsBySemester(@PathVariable UUID semesterId) {
        return ResponseEntity.ok(adminService.getSubjectsBySemester(semesterId));
    }

    @PostMapping("/subjects")
    public ResponseEntity<SubjectResponse> createSubject(@Valid @RequestBody CreateSubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createSubject(request));
    }

    @PutMapping("/subjects/{id}")
    public ResponseEntity<SubjectResponse> updateSubject(@PathVariable UUID id, @Valid @RequestBody CreateSubjectRequest request) {
        return ResponseEntity.ok(adminService.updateSubject(id, request));
    }

    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable UUID id) {
        adminService.deleteSubject(id);
        return ResponseEntity.noContent().build();
    }

    // ===== STUDENT =====

    @GetMapping("/students")
    public ResponseEntity<List<StudentResponse>> getAllStudents() {
        return ResponseEntity.ok(adminService.getAllStudents());
    }

    @PostMapping("/students")
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody CreateStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createStudent(request));
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<StudentResponse> updateStudent(@PathVariable UUID id, @Valid @RequestBody CreateStudentRequest request) {
        return ResponseEntity.ok(adminService.updateStudent(id, request));
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<Void> deactivateStudent(@PathVariable UUID id) {
        adminService.deactivateStudent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/students/import")
    public ResponseEntity<ExcelImportResponse> importStudents(@RequestParam("file") MultipartFile file) {
        try {
            ExcelImportResponse response = adminService.importStudentsFromExcel(file.getInputStream());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ExcelImportResponse.builder()
                            .totalRows(0).successCount(0).failedCount(0)
                            .errors(List.of("Failed to process file: " + e.getMessage()))
                            .build()
            );
        }
    }

    // ===== TEACHER =====

    @GetMapping("/teachers")
    public ResponseEntity<List<TeacherResponse>> getAllTeachers() {
        return ResponseEntity.ok(adminService.getAllTeachers());
    }

    @PostMapping("/teachers")
    public ResponseEntity<TeacherResponse> createTeacher(@Valid @RequestBody CreateTeacherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createTeacher(request));
    }

    @PutMapping("/teachers/{id}")
    public ResponseEntity<TeacherResponse> updateTeacher(@PathVariable UUID id, @Valid @RequestBody CreateTeacherRequest request) {
        return ResponseEntity.ok(adminService.updateTeacher(id, request));
    }

    @DeleteMapping("/teachers/{id}")
    public ResponseEntity<Void> deactivateTeacher(@PathVariable UUID id) {
        adminService.deactivateTeacher(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/teachers/import")
    public ResponseEntity<ExcelImportResponse> importTeachers(@RequestParam("file") MultipartFile file) {
        try {
            ExcelImportResponse response = adminService.importTeachersFromExcel(file.getInputStream());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ExcelImportResponse.builder()
                            .totalRows(0).successCount(0).failedCount(0)
                            .errors(List.of("Failed to process file: " + e.getMessage()))
                            .build()
            );
        }
    }
}
