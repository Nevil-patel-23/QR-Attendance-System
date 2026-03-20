package com.university.attendance.repository;

import com.university.attendance.models.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, UUID> {

    List<Holiday> findAllByOrderByDateAsc();

    boolean existsByDate(LocalDate date);
}
