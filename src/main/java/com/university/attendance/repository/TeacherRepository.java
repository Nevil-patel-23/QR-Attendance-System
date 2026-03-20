package com.university.attendance.repository;

import com.university.attendance.models.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, UUID> {
    List<Teacher> findByFacultyFacultyId(UUID facultyId);
    boolean existsByPrn(String prn);
    Optional<Teacher> findByPrn(String prn);
}
