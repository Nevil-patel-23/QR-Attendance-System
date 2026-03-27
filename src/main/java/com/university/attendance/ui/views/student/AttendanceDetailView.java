package com.university.attendance.ui.views.student;

import com.university.attendance.dto.response.AttendanceRecordResponse;
import com.university.attendance.dto.response.AttendanceSummaryResponse;
import com.university.attendance.dto.response.StudentSubjectResponse;
import com.university.attendance.models.AttendanceStatus;
import com.university.attendance.service.StudentService;
import com.university.attendance.security.JwtUtil;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Per-subject attendance detail view. Shows a subject dropdown,
 * summary stats, and a grid of dated attendance records with
 * colored PRESENT/ABSENT badges.
 */
@Route(value = "student/attendance", layout = StudentLayout.class)
@PageTitle("My Attendance")
public class AttendanceDetailView extends VerticalLayout {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final StudentService studentService;
    private final String currentPrn;

    private final VerticalLayout summarySection = new VerticalLayout();
    private final Grid<AttendanceRecordResponse> recordGrid = new Grid<>();

    public AttendanceDetailView(StudentService studentService, JwtUtil jwtUtil) {
        this.studentService = studentService;
        this.currentPrn = resolveCurrentPrn(jwtUtil);

        setPadding(true);
        setSpacing(true);

        H2 title = new H2("My Attendance");
        add(title);

        // Subject dropdown
        ComboBox<StudentSubjectResponse> subjectCombo = new ComboBox<>("Select Subject");
        subjectCombo.setWidth("400px");
        subjectCombo.setItemLabelGenerator(s -> s.getSubjectName() + " (" + s.getSubjectCode() + ")");

        try {
            List<StudentSubjectResponse> subjects = studentService.getMySubjects(currentPrn);
            subjectCombo.setItems(subjects);
        } catch (Exception e) {
            add(new Paragraph("Error loading subjects: " + e.getMessage()));
        }

        subjectCombo.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                loadAttendance(event.getValue().getSubjectId());
            }
        });
        add(subjectCombo);

        // Summary section (populated on subject select)
        summarySection.setPadding(false);
        summarySection.setSpacing(true);
        add(summarySection);

        // Records grid
        configureGrid();
        add(recordGrid);
    }

    private void configureGrid() {
        recordGrid.setWidthFull();
        recordGrid.setAllRowsVisible(true);

        recordGrid.addColumn(r -> r.getSessionDate().format(DATE_FMT))
                .setHeader("Date").setSortable(true).setAutoWidth(true);

        recordGrid.addColumn(r -> r.getSessionDate().getDayOfWeek().toString())
                .setHeader("Day").setAutoWidth(true);

        recordGrid.addColumn(r -> r.getSessionStartTime() != null
                        ? r.getSessionStartTime().format(TIME_FMT) : "—")
                .setHeader("Time").setAutoWidth(true);

        recordGrid.addComponentColumn(r -> {
            Span badge = new Span(r.getStatus().name());
            if (r.getStatus() == AttendanceStatus.PRESENT) {
                badge.getStyle()
                        .set("background-color", "var(--lumo-success-color-10pct)")
                        .set("color", "var(--lumo-success-text-color)")
                        .set("padding", "var(--lumo-space-xs) var(--lumo-space-s)")
                        .set("border-radius", "var(--lumo-border-radius-s)")
                        .set("font-weight", "600");
            } else {
                badge.getStyle()
                        .set("background-color", "var(--lumo-error-color-10pct)")
                        .set("color", "var(--lumo-error-text-color)")
                        .set("padding", "var(--lumo-space-xs) var(--lumo-space-s)")
                        .set("border-radius", "var(--lumo-border-radius-s)")
                        .set("font-weight", "600");
            }
            return badge;
        }).setHeader("Status").setAutoWidth(true);
    }

    private void loadAttendance(UUID subjectId) {
        summarySection.removeAll();

        try {
            // Load summary
            AttendanceSummaryResponse summary =
                    studentService.getAttendanceSummaryForSubject(currentPrn, subjectId);

            HorizontalLayout statsRow = new HorizontalLayout();
            statsRow.setSpacing(true);
            statsRow.setWidthFull();
            statsRow.getStyle()
                    .set("background-color", "var(--lumo-base-color)")
                    .set("box-shadow", "var(--lumo-box-shadow-xs)")
                    .set("border-radius", "var(--lumo-border-radius-m)")
                    .set("padding", "var(--lumo-space-m)");

            String pctColor = summary.isAtRisk()
                    ? "var(--lumo-error-color)" : "var(--lumo-success-color)";

            statsRow.add(createStatBlock("Attendance", summary.getAttendancePercentage() + "%", pctColor));
            statsRow.add(createStatBlock("Present", String.valueOf(summary.getPresentCount()), null));
            statsRow.add(createStatBlock("Total Sessions", String.valueOf(summary.getTotalSessions()), null));

            if (summary.isAtRisk()) {
                statsRow.add(createStatBlock("Classes Needed",
                        String.valueOf(summary.getClassesNeeded()), "var(--lumo-error-color)"));
            } else {
                statsRow.add(createStatBlock("Can Miss",
                        String.valueOf(summary.getClassesCanMiss()), "var(--lumo-success-color)"));
            }

            summarySection.add(statsRow);

            // Load records (most recent first)
            List<AttendanceRecordResponse> records =
                    studentService.getAttendanceDetail(currentPrn, subjectId);
            // Reverse to show most recent first
            records.sort((a, b) -> b.getSessionDate().compareTo(a.getSessionDate()));
            recordGrid.setItems(records);

        } catch (Exception e) {
            summarySection.add(new Paragraph("Error: " + e.getMessage()));
        }
    }

    private VerticalLayout createStatBlock(String label, String value, String color) {
        VerticalLayout block = new VerticalLayout();
        block.setPadding(false);
        block.setSpacing(false);
        block.setAlignItems(Alignment.CENTER);

        Span valSpan = new Span(value);
        valSpan.getStyle().set("font-size", "var(--lumo-font-size-xl)").set("font-weight", "700");
        if (color != null) {
            valSpan.getStyle().set("color", color);
        }

        Span lblSpan = new Span(label);
        lblSpan.getStyle().set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        block.add(valSpan, lblSpan);
        return block;
    }

    private String resolveCurrentPrn(JwtUtil jwtUtil) {
        HttpServletRequest request = (HttpServletRequest) VaadinRequest.getCurrent();
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    return jwtUtil.extractPrn(cookie.getValue());
                }
            }
        }
        throw new RuntimeException("Not authenticated");
    }
}
