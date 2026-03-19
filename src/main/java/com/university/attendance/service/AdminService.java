package com.university.attendance.service;

import com.university.attendance.dto.request.CreateCourseRequest;
import com.university.attendance.dto.request.CreateFacultyRequest;
import com.university.attendance.dto.request.CreateSubjectRequest;
import com.university.attendance.dto.response.CourseResponse;
import com.university.attendance.dto.response.FacultyResponse;
import com.university.attendance.dto.response.SemesterResponse;
import com.university.attendance.dto.response.SubjectResponse;
import com.university.attendance.exception.ResourceNotFoundException;
import com.university.attendance.exception.ValidationException;
import com.university.attendance.models.Course;
import com.university.attendance.models.Faculty;
import com.university.attendance.models.Semester;
import com.university.attendance.models.Subject;
import com.university.attendance.repository.CourseRepository;
import com.university.attendance.repository.FacultyRepository;
import com.university.attendance.repository.SemesterRepository;
import com.university.attendance.repository.SubjectRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
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

    // ===== MAPPERS FOR NEW DTOs =====

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
}
