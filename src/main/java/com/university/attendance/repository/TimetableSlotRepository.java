package com.university.attendance.repository;

import com.university.attendance.models.TimetableSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.university.attendance.models.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, UUID> {

    List<TimetableSlot> findByAllocationAllocationId(UUID allocationId);

    List<TimetableSlot> findByAllocationSemesterSemesterIdAndEffectiveToIsNull(UUID semesterId);

    List<TimetableSlot> findByAllocationTeacherTeacherIdAndEffectiveToIsNull(UUID teacherId);

    boolean existsByAllocationTeacherTeacherIdAndDayOfWeekAndStartTimeLessThanAndEndTimeGreaterThanAndEffectiveToIsNull(
            UUID teacherId, DayOfWeek dayOfWeek, LocalTime endTime, LocalTime startTime);
}
