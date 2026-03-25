package com.university.attendance.repository;

import com.university.attendance.models.StudentSubjectEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Queries the student_subject_enrollments table.
 * Only elective opt-ins are stored here. Compulsory subjects
 * are inferred from student.currentSemesterId.
 */
@Repository
public interface StudentSubjectEnrollmentRepository extends JpaRepository<StudentSubjectEnrollment, UUID> {

    /**
     * Check if a student is enrolled in a specific elective for a given year.
     * Used during scan validation (Check 3 — subject eligibility for electives).
     */
    boolean existsByStudentStudentIdAndSubjectSubjectIdAndAcademicYear(
            UUID studentId, UUID subjectId, String academicYear);

    /**
     * Get all enrollments for a specific elective subject in a given year.
     * Used by ScheduledJobService to find all students who should get
     * ABSENT records for an elective subject.
     */
    List<StudentSubjectEnrollment> findBySubjectSubjectIdAndAcademicYear(
            UUID subjectId, String academicYear);

    /**
     * Get all enrollments for a specific student in a given year.
     * Used by the Elective Enrollment management screen.
     */
    List<StudentSubjectEnrollment> findByStudentStudentIdAndAcademicYear(
            UUID studentId, String academicYear);

    /**
     * Count how many students are enrolled in a subject for a given year.
     * Used to display "enrolled count" next to each elective in the UI.
     */
    long countBySubjectSubjectIdAndAcademicYear(
            UUID subjectId, String academicYear);

    boolean existsByStudentStudentIdAndSubjectSemesterSemesterIdAndAcademicYear(
            UUID studentId, UUID semesterId, String academicYear);
}
