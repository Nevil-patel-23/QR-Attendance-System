package com.university.attendance.ui.views.shared;

import com.university.attendance.dto.response.AttendanceSuccessResponse;
import com.university.attendance.models.Student;
import com.university.attendance.repository.StudentRepository;
import com.university.attendance.security.JwtUtil;
import com.university.attendance.service.AttendanceService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Handles /attend?token={uuid} — the URL students open by scanning a QR code.
 *
 * Flow:
 * 1. Student scans QR on phone → phone opens this URL in mobile browser.
 * 2. If NOT logged in → save token in pending_token cookie → redirect to /login.
 *    LoginView will redirect back here after successful login.
 * 3. If logged in → extract PRN from JWT → find Student → call
 *    attendanceService.recordAttendance(token, studentId).
 * 4. Show GREEN card on success, RED card on failure.
 */
@Route("attend")
@PageTitle("Attendance | QR Attendance")
@AnonymousAllowed
public class AttendanceResultView extends VerticalLayout implements BeforeEnterObserver {

    private final AttendanceService attendanceService;
    private final StudentRepository studentRepository;
    private final JwtUtil jwtUtil;

    public AttendanceResultView(AttendanceService attendanceService,
                                StudentRepository studentRepository,
                                JwtUtil jwtUtil) {
        this.attendanceService = attendanceService;
        this.studentRepository = studentRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // ── Extract token from URL ──
        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
        String token = null;
        if (params.containsKey("token")) {
            token = params.get("token").get(0);
        }

        if (token == null || token.isEmpty()) {
            showError("No attendance token provided. Please scan a valid QR code.");
            return;
        }

        // ── Check authentication via JWT cookie ──
        HttpServletRequest request = (HttpServletRequest) VaadinService.getCurrentRequest();
        String jwtToken = null;

        if (request != null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwt".equals(cookie.getName()) && !cookie.getValue().isEmpty()) {
                        jwtToken = cookie.getValue();
                        break;
                    }
                }
            }
        }

        if (jwtToken == null || !jwtUtil.validateToken(jwtToken)) {
            // ── Not logged in → save token in cookie, redirect to login ──
            HttpServletResponse response = (HttpServletResponse) VaadinService.getCurrentResponse();
            if (response != null) {
                Cookie pendingToken = new Cookie("pending_token", token);
                pendingToken.setMaxAge(600);  // 10 minutes
                pendingToken.setPath("/");
                pendingToken.setHttpOnly(true);
                pendingToken.setSecure(false);  // true in production
                response.addCookie(pendingToken);
            }
            UI.getCurrent().navigate("");
            return;
        }

        // ── Logged in → extract PRN, find student, record attendance ──
        try {
            String prn = jwtUtil.extractPrn(jwtToken);
            Student student = studentRepository.findByPrn(prn)
                    .orElse(null);

            if (student == null) {
                showError("Student profile not found for PRN: " + prn);
                return;
            }

            AttendanceSuccessResponse result = attendanceService.recordAttendance(token, student.getStudentId());
            showSuccess(result);

        } catch (Exception ex) {
            showError(ex.getMessage() != null ? ex.getMessage() : "An error occurred while recording attendance.");
        }
    }

    /**
     * GREEN success card showing attendance confirmation details.
     */
    private void showSuccess(AttendanceSuccessResponse result) {
        removeAll();
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        H1 title = new H1("✅ Attendance Marked");
        title.getStyle().set("color", "var(--lumo-success-color)");

        String date = result.getSessionDate()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

        Span studentSpan = createDetailRow("Student", result.getStudentName());
        Span subjectSpan = createDetailRow("Subject", result.getSubjectName());
        Span teacherSpan = createDetailRow("Teacher", result.getTeacherName());
        Span dateSpan = createDetailRow("Date", date);
        Span timeSpan = createDetailRow("Scanned At", result.getScannedAt()
                .format(DateTimeFormatter.ofPattern("hh:mm:ss a")));

        VerticalLayout card = new VerticalLayout(title, studentSpan, subjectSpan, teacherSpan, dateSpan, timeSpan);
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
        card.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        card.getStyle().set("background-color", "var(--lumo-base-color)");
        card.getStyle().set("border", "2px solid var(--lumo-success-color)");
        card.setPadding(true);
        card.setMaxWidth("500px");

        add(card);
    }

    /**
     * RED error card showing the failure reason.
     */
    private void showError(String message) {
        removeAll();
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        H1 title = new H1("❌ Attendance Failed");
        title.getStyle().set("color", "var(--lumo-error-color)");

        H3 errorMsg = new H3(message);
        errorMsg.getStyle().set("color", "var(--lumo-error-text-color)");
        errorMsg.getStyle().set("text-align", "center");
        errorMsg.getStyle().set("max-width", "400px");

        Paragraph hint = new Paragraph("Please try scanning the QR code again or contact your teacher.");
        hint.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout card = new VerticalLayout(title, errorMsg, hint);
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
        card.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        card.getStyle().set("background-color", "var(--lumo-base-color)");
        card.getStyle().set("border", "2px solid var(--lumo-error-color)");
        card.setPadding(true);
        card.setMaxWidth("500px");

        add(card);
    }

    private Span createDetailRow(String label, String value) {
        Span span = new Span(label + ": " + value);
        span.getStyle().set("font-size", "var(--lumo-font-size-l)");
        return span;
    }
}
