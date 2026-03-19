package com.university.attendance.repository;

import com.university.attendance.models.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, UUID> {
    List<Semester> findByCourseCourseId(UUID courseId);
    List<Semester> findByCourseCourseIdOrderBySemesterNumberAsc(UUID courseId);
}
