package com.university.attendance.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a holiday when no attendance is taken.
 * Maps to the 'holidays' table.
 *
 * The date column is UNIQUE — one holiday per date.
 * The system skips attendance on these dates.
 */
@Entity
@Table(name = "holidays")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "holiday_id", updatable = false, nullable = false)
    private UUID holidayId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "date", nullable = false, unique = true)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private HolidayType type;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
