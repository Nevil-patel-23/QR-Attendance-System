package com.university.attendance.repository;

import com.university.attendance.models.Subject;
import com.university.attendance.models.SubjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    List<Subject> findBySemesterSemesterId(UUID semesterId);
    List<Subject> findBySemesterSemesterIdAndType(UUID semesterId, SubjectType type);
    boolean existsByCode(String code);
    java.util.Optional<Subject> findByCode(String code);
}
