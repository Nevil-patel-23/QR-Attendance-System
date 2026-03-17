package com.university.attendance.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Represents a recurring weekly lecture slot in the timetable.
 * Maps to the 'timetable_slots' table.
 *
 * Each slot links to a TeacherSubjectAllocation (who teaches what)
 * and specifies the day, time, and room.
 *
 * effective_from / effective_to define when this slot is valid.
 * effective_to = null means the slot is still active.
 *
 * The service layer enforces no teacher double-booking:
 * same teacher cannot have two slots with overlapping day+time.
 */
@Entity
@Table(name = "timetable_slots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "slot_id", updatable = false, nullable = false)
    private UUID slotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id", nullable = false)
    private TeacherSubjectAllocation allocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "room", length = 20, nullable = false)
    private String room;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;
}
