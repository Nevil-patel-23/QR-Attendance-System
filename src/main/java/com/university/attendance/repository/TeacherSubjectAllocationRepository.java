package com.university.attendance.repository;

import com.university.attendance.models.TeacherSubjectAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherSubjectAllocationRepository extends JpaRepository<TeacherSubjectAllocation, UUID> {

    List<TeacherSubjectAllocation> findByTeacherTeacherIdAndSemesterSemesterIdAndAcademicYear(
            UUID teacherId, UUID semesterId, String academicYear);

    List<TeacherSubjectAllocation> findBySemesterSemesterIdAndAcademicYear(
            UUID semesterId, String academicYear);

    boolean existsByTeacherTeacherIdAndSubjectSubjectIdAndSemesterSemesterIdAndAcademicYear(
            UUID teacherId, UUID subjectId, UUID semesterId, String academicYear);

    Optional<TeacherSubjectAllocation> findByTeacherTeacherIdAndSubjectSubjectIdAndSemesterSemesterIdAndAcademicYear(
            UUID teacherId, UUID subjectId, UUID semesterId, String academicYear);
}
