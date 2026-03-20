package com.university.attendance.repository;

import com.university.attendance.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {
    List<Student> findByCourseCourseId(UUID courseId);
    List<Student> findByCurrentSemesterSemesterId(UUID semesterId);
    boolean existsByPrn(String prn);
    Optional<Student> findByPrn(String prn);
}
