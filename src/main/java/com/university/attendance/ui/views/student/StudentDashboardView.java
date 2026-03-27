package com.university.attendance.ui.views.student;

import com.university.attendance.dto.response.AttendanceSummaryResponse;
import com.university.attendance.dto.response.StudentDashboardResponse;
import com.university.attendance.dto.response.TimetableSlotResponse;
import com.university.attendance.service.StudentService;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.university.attendance.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.time.format.DateTimeFormatter;

/**
 * Student dashboard — shows profile strip, at-risk warning,
 * attendance summary cards, and today's class schedule.
 */
@Route(value = "student", layout = StudentLayout.class)
@PageTitle("My Dashboard")
public class StudentDashboardView extends VerticalLayout {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    public StudentDashboardView(StudentService studentService, JwtUtil jwtUtil) {
        setPadding(true);
        setSpacing(true);

        try {
            String prn = resolveCurrentPrn(jwtUtil);
            StudentDashboardResponse dash = studentService.getDashboard(prn);
            buildDashboard(dash);
        } catch (Exception e) {
            Paragraph error = new Paragraph("Error loading dashboard: " + e.getMessage());
            error.getStyle().set("color", "var(--lumo-error-text-color)");
            add(error);
        }
    }

    private void buildDashboard(StudentDashboardResponse dash) {
        // ── Profile strip ──
        H2 greeting = new H2("Welcome, " + dash.getStudentName());
        greeting.getStyle().set("margin-bottom", "0");

        HorizontalLayout profileRow = new HorizontalLayout();
        profileRow.setSpacing(true);
        profileRow.add(createBadge("PRN: " + dash.getPrn()));
        profileRow.add(createBadge(dash.getCourseName()));
        profileRow.add(createBadge(dash.getCurrentSemesterLabel()));
        profileRow.add(createBadge("AY: " + dash.getAcademicYear()));
        add(greeting, profileRow);

        // ── At-risk warning ──
        if (dash.isHasAtRiskSubjects()) {
            Div warning = new Div();
            warning.setText("⚠️ You are below 75% attendance in one or more subjects. Immediate action required.");
            warning.getStyle()
                    .set("background-color", "var(--lumo-error-color-10pct)")
                    .set("color", "var(--lumo-error-text-color)")
                    .set("padding", "var(--lumo-space-m)")
                    .set("border-radius", "var(--lumo-border-radius-m)")
                    .set("font-weight", "600")
                    .set("border-left", "4px solid var(--lumo-error-color)");
            add(warning);
        }

        // ── Attendance cards ──
        if (dash.getAttendanceSummaries() != null && !dash.getAttendanceSummaries().isEmpty()) {
            H3 sectionTitle = new H3("Attendance Summary");
            add(sectionTitle);

            FlexLayout cardsGrid = new FlexLayout();
            cardsGrid.setFlexWrap(FlexLayout.FlexWrap.WRAP);
            cardsGrid.getStyle().set("gap", "var(--lumo-space-m)");

            for (AttendanceSummaryResponse summary : dash.getAttendanceSummaries()) {
                cardsGrid.add(createAttendanceCard(summary));
            }
            add(cardsGrid);
        } else {
            add(new Paragraph("No attendance data available yet. Check back after your first class."));
        }

        // ── Today's classes ──
        H3 todayTitle = new H3("Today's Classes");
        add(todayTitle);

        if (dash.getTodaySlots() == null || dash.getTodaySlots().isEmpty()) {
            Paragraph noClasses = new Paragraph("No classes scheduled for today 🎉");
            noClasses.getStyle().set("color", "var(--lumo-secondary-text-color)");
            add(noClasses);
        } else {
            for (TimetableSlotResponse slot : dash.getTodaySlots()) {
                add(createTodaySlotRow(slot));
            }
        }
    }

    private VerticalLayout createAttendanceCard(AttendanceSummaryResponse s) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidth("280px");
        card.getStyle()
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background-color", "var(--lumo-base-color)")
                .set("border-left", s.isAtRisk()
                        ? "4px solid var(--lumo-error-color)"
                        : "4px solid var(--lumo-success-color)");

        // Subject name + code
        Span subName = new Span(s.getSubjectName());
        subName.getStyle().set("font-weight", "600").set("font-size", "var(--lumo-font-size-l)");
        Span subCode = new Span(s.getSubjectCode() + " • " + s.getSubjectType());
        subCode.getStyle().set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        // Percentage (big, colored)
        Span pctSpan = new Span(s.getAttendancePercentage() + "%");
        pctSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "700")
                .set("color", s.isAtRisk() ? "var(--lumo-error-color)" : "var(--lumo-success-color)");

        // Present / Total
        Span countSpan = new Span(s.getPresentCount() + " / " + s.getTotalSessions() + " classes");
        countSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

        // Progress bar
        ProgressBar bar = new ProgressBar();
        bar.setMin(0);
        bar.setMax(100);
        bar.setValue(s.getAttendancePercentage());
        bar.setWidth("100%");
        if (s.isAtRisk()) {
            bar.getStyle().set("--vaadin-progress-bar-fill-color", "var(--lumo-error-color)");
        }

        // Extra info
        Span extraInfo;
        if (s.isAtRisk()) {
            extraInfo = new Span("Need " + s.getClassesNeeded() + " more class(es) to reach 75%");
            extraInfo.getStyle().set("color", "var(--lumo-error-text-color)")
                    .set("font-size", "var(--lumo-font-size-s)");
        } else {
            extraInfo = new Span("Can miss " + s.getClassesCanMiss() + " more class(es)");
            extraInfo.getStyle().set("color", "var(--lumo-success-text-color)")
                    .set("font-size", "var(--lumo-font-size-s)");
        }

        card.add(subName, subCode, pctSpan, countSpan, bar, extraInfo);
        return card;
    }

    private HorizontalLayout createTodaySlotRow(TimetableSlotResponse slot) {
        HorizontalLayout row = new HorizontalLayout();
        row.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        row.setWidthFull();
        row.setPadding(true);
        row.getStyle()
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background-color", "var(--lumo-base-color)");

        Span time = new Span(slot.getStartTime().format(TIME_FMT) + " – " + slot.getEndTime().format(TIME_FMT));
        time.getStyle().set("font-weight", "600").set("min-width", "160px");

        Span subject = new Span(slot.getSubjectName() + " (" + slot.getSubjectCode() + ")");

        Span room = new Span("Room: " + slot.getRoom());
        room.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span teacher = new Span(slot.getTeacherName());
        teacher.getStyle().set("color", "var(--lumo-secondary-text-color)");

        row.add(time, subject, room, teacher);
        return row;
    }

    private Span createBadge(String text) {
        Span badge = new Span(text);
        badge.getStyle()
                .set("background-color", "var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-s)")
                .set("font-size", "var(--lumo-font-size-s)");
        return badge;
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
