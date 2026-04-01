package com.university.attendance.ui.views.teacher;

import com.university.attendance.dto.response.TodaySlotResponse;
import com.university.attendance.models.Teacher;
import com.university.attendance.security.JwtUtil;
import com.university.attendance.dto.response.AttendanceSessionResponse;
import com.university.attendance.service.QrService;
import com.university.attendance.service.TeacherService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Teacher Dashboard — shows today's timetable slots as cards.
 *
 * Each card displays:
 *   - Subject name and code
 *   - Semester and course
 *   - Time range and room
 *   - "Generate QR" button (enabled only within the 15min-before to 30min-after window)
 *   - If active session exists: "View Live QR" button instead
 *
 * Replaces the placeholder "Teacher Dashboard — coming soon" view.
 */
@Route(value = "teacher", layout = TeacherLayout.class)
@PageTitle("My Dashboard")
@RolesAllowed("TEACHER")
public class TeacherDashboardView extends VerticalLayout {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");

    private final TeacherService teacherService;
    private final JwtUtil jwtUtil;

    private final QrService qrService;

    public TeacherDashboardView(TeacherService teacherService, JwtUtil jwtUtil, QrService qrService) {
        this.teacherService = teacherService;
        this.jwtUtil = jwtUtil;
        this.qrService = qrService;

        // Programmatic role guard — Vaadin @RolesAllowed is not enforced with stateless JWT
        String role = resolveRole();
        if (!"TEACHER".equalsIgnoreCase(role)) {
            UI.getCurrent().getPage().setLocation("/");
            return;
        }

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        try {
            Teacher teacher = resolveTeacher();
            UUID teacherId = teacher.getTeacherId();

            // ── Profile strip ──
            H2 greeting = new H2("Welcome, " + teacher.getFirstName() + " " + teacher.getLastName());
            greeting.getStyle().set("margin-bottom", "0");
            greeting.getStyle().set("margin-top", "0");

            HorizontalLayout profileRow = new HorizontalLayout();
            profileRow.setSpacing(true);
            profileRow.add(createBadge("PRN: " + teacher.getPrn()));
            
            if (teacher.getFaculty() != null) {
                profileRow.add(createBadge(teacher.getFaculty().getName()));
            }
            if (teacher.getDesignation() != null) {
                profileRow.add(createBadge(teacher.getDesignation()));
            }

            add(greeting, profileRow);

            // Header
            H3 title = new H3("Today's Lectures");
            title.getStyle().set("margin-top", "var(--lumo-space-l)");
            add(title);

            List<TodaySlotResponse> slots = teacherService.getTodaySlots(teacherId);

            if (slots.isEmpty()) {
                Paragraph noSlots = new Paragraph("No lectures scheduled for today. Enjoy your day! 🎉");
                noSlots.getStyle().set("font-size", "var(--lumo-font-size-l)");
                noSlots.getStyle().set("color", "var(--lumo-secondary-text-color)");
                add(noSlots);
            } else {
                for (TodaySlotResponse slot : slots) {
                    add(createSlotCard(slot));
                }
            }
        } catch (Exception e) {
            Paragraph error = new Paragraph("Error loading dashboard: " + e.getMessage());
            error.getStyle().set("color", "var(--lumo-error-text-color)");
            add(error);
        }
    }

    /**
     * Create a card layout for one timetable slot.
     */
    private VerticalLayout createSlotCard(TodaySlotResponse slot) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle().set("box-shadow", "var(--lumo-box-shadow-s)");
        card.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        card.getStyle().set("background-color", "var(--lumo-base-color)");
        card.setMaxWidth("600px");
        card.setWidthFull();

        // Subject header
        H3 subjectTitle = new H3(slot.getSubjectName() + " (" + slot.getSubjectCode() + ")");
        subjectTitle.getStyle().set("margin", "0");

        // Details row
        Span semesterInfo = new Span(slot.getCourseName() + " — " + slot.getSemesterLabel());
        semesterInfo.getStyle().set("color", "var(--lumo-secondary-text-color)");

        // Time and room
        String timeRange = slot.getStartTime().format(TIME_FORMAT) + " - " + slot.getEndTime().format(TIME_FORMAT);
        HorizontalLayout timeRoomLayout = new HorizontalLayout();
        timeRoomLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        timeRoomLayout.add(
                new Icon(VaadinIcon.CLOCK),
                new Span(timeRange),
                new Span(" | "),
                new Icon(VaadinIcon.MAP_MARKER),
                new Span("Room " + slot.getRoom())
        );

        // Action button
        if (slot.isHasActiveSession()) {
            // Active session exists — show "View Live QR" button
            Button viewQrButton = new Button("View Live QR", new Icon(VaadinIcon.QRCODE), e -> {
                UI.getCurrent().navigate("teacher/live-qr?sessionId=" + slot.getActiveSessionId());
            });
            viewQrButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            card.add(subjectTitle, semesterInfo, timeRoomLayout, viewQrButton);
        } else if (slot.isWithinWindow()) {
            // Explain fix in 2 plain English lines before coding
            // We generate the session directly in the dashboard and get its sessionId.
            // Then we navigate to LiveQrView using sessionId instead of slotId.
            Button generateButton = new Button("Generate QR", new Icon(VaadinIcon.QRCODE), e -> {
                try {
                    Teacher teacher = resolveTeacher();
                    AttendanceSessionResponse generatedSession = qrService.generateSession(slot.getSlotId(), teacher.getTeacherId());
                    QueryParameters qp = new QueryParameters(Map.of("sessionId", List.of(generatedSession.getSessionId().toString())));
                    UI.getCurrent().navigate("teacher/live-qr", qp);
                } catch (Exception ex) {
                    // error
                }
            });
            generateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
            card.add(subjectTitle, semesterInfo, timeRoomLayout, generateButton);
        } else {
            // Outside window — show disabled info
            Span windowInfo = new Span("QR generation available 15 min before lecture starts");
            windowInfo.getStyle().set("color", "var(--lumo-tertiary-text-color)");
            windowInfo.getStyle().set("font-style", "italic");
            card.add(subjectTitle, semesterInfo, timeRoomLayout, windowInfo);
        }

        return card;
    }

    /**
     * Resolve teacher from the JWT cookie.
     */
    private Teacher resolveTeacher() {
        HttpServletRequest httpRequest = (HttpServletRequest) VaadinRequest.getCurrent();
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    UUID userId = jwtUtil.extractUserId(cookie.getValue());
                    return teacherService.getTeacherByUserId(userId);
                }
            }
        }
        throw new RuntimeException("Not authenticated");
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

    /**
     * Resolve the role from the JWT cookie for programmatic access control.
     */
    private String resolveRole() {
        HttpServletRequest httpRequest = (HttpServletRequest) VaadinRequest.getCurrent();
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    return jwtUtil.extractRole(cookie.getValue());
                }
            }
        }
        return "";
    }
}
