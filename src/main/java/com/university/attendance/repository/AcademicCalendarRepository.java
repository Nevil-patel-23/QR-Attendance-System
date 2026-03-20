package com.university.attendance.repository;

import com.university.attendance.models.AcademicCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AcademicCalendarRepository extends JpaRepository<AcademicCalendar, UUID> {

    List<AcademicCalendar> findByCourseCourseId(UUID courseId);

    List<AcademicCalendar> findByCourseCourseIdAndAcademicYear(UUID courseId, String academicYear);

    boolean existsByCourseCourseIdAndAcademicYearAndSemesterNumber(
            UUID courseId, String academicYear, Integer semesterNumber);
}
