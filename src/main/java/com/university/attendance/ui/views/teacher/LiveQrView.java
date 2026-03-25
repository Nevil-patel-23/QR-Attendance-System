package com.university.attendance.ui.views.teacher;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.university.attendance.dto.response.AttendanceSessionResponse;
import com.university.attendance.models.Teacher;
import com.university.attendance.security.JwtUtil;
import com.university.attendance.service.QrService;
import com.university.attendance.service.TeacherService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Full-screen QR code display for teachers.
 *
 * Two entry modes:
 * 1. ?slotId={uuid}  — generates a NEW session, then displays the QR
 * 2. /teacher/live-qr/{sessionId} — displays an EXISTING session's QR
 *
 * Features:
 * - Large QR code image generated server-side with ZXing
 * - Countdown timer showing seconds remaining until expiry
 * - Live scan counter ("X / Y scanned") refreshed every 5 seconds
 * - "Back to Dashboard" button
 */
@Route("teacher/live-qr")
@PageTitle("Live QR | QR Attendance")
public class LiveQrView extends VerticalLayout implements HasUrlParameter<String>, BeforeEnterObserver {

    private final QrService qrService;
    private final TeacherService teacherService;
    private final JwtUtil jwtUtil;

    // UI components that get updated
    private final Span countdownLabel = new Span();
    private final Span scanCounter = new Span();
    private final Image qrImage = new Image();
    private final H3 subjectLabel = new H3();
    private final Span statusLabel = new Span();

    // State
    private AttendanceSessionResponse session;
    private UUID slotIdParam;
    private UUID sessionIdParam;

    // Background polling
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> pollingTask;

    public LiveQrView(QrService qrService, TeacherService teacherService, JwtUtil jwtUtil) {
        this.qrService = qrService;
        this.teacherService = teacherService;
        this.jwtUtil = jwtUtil;

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        setSpacing(true);
        setPadding(true);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        // Route format: /teacher/live-qr/{sessionId}
        if (parameter != null && !parameter.isEmpty()) {
            try {
                sessionIdParam = UUID.fromString(parameter);
            } catch (IllegalArgumentException ignored) {
                // Not a valid UUID, will check query params
            }
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Check for ?sessionId= query parameter
        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
        if (params.containsKey("sessionId")) {
            try {
                sessionIdParam = UUID.fromString(params.get("sessionId").get(0));
            } catch (Exception ignored) {
                // Invalid sessionId
            }
        }

        // Generate or load session
        try {
            if (slotIdParam != null) {
                // Resolve teacherId from JWT cookie, then generate session
                UUID teacherId = resolveTeacherId();
                session = qrService.generateSession(slotIdParam, teacherId);
            } else if (sessionIdParam != null) {
                session = qrService.getSessionStatus(sessionIdParam);
            } else {
                showError("No slot or session specified");
                return;
            }

            buildUI();
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        }
    }

    /**
     * Resolve teacher ID from the JWT cookie.
     */
    private UUID resolveTeacherId() {
        HttpServletRequest httpRequest = (HttpServletRequest) VaadinRequest.getCurrent();
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    UUID userId = jwtUtil.extractUserId(cookie.getValue());
                    Teacher teacher = teacherService.getTeacherByUserId(userId);
                    return teacher.getTeacherId();
                }
            }
        }
        throw new RuntimeException("Not authenticated");
    }

    /**
     * Build the full QR display UI.
     */
    private void buildUI() {
        removeAll();

        // Back button
        Button backButton = new Button("Back to Dashboard", new Icon(VaadinIcon.ARROW_LEFT), e -> {
            UI.getCurrent().navigate("teacher");
        });
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        // Subject info
        subjectLabel.setText(session.getSubjectName() + " (" + session.getSubjectCode() + ")");
        subjectLabel.getStyle().set("margin", "0");

        Span semesterLabel = new Span(session.getSemesterLabel());
        semesterLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");

        // QR code image
        try {
            String qrUrl = "http://localhost:8080/attend?token=" + session.getQrToken();
            byte[] qrBytes = generateQrCode(qrUrl, 350);

            StreamResource resource = new StreamResource("qr.png",
                    () -> new ByteArrayInputStream(qrBytes));
            qrImage.setSrc(resource);
            qrImage.setAlt("QR Code for attendance");
            qrImage.setWidth("350px");
            qrImage.setHeight("350px");
            qrImage.getStyle().set("border-radius", "var(--lumo-border-radius-l)");
            qrImage.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
        } catch (Exception e) {
            add(new Span("Error generating QR code: " + e.getMessage()));
            return;
        }

        // Countdown timer
        countdownLabel.getStyle().set("font-size", "var(--lumo-font-size-xxl)");
        countdownLabel.getStyle().set("font-weight", "bold");
        updateCountdown();

        // Scan counter
        scanCounter.setText(session.getPresentCount() + " / " + session.getTotalStudents() + " scanned");
        scanCounter.getStyle().set("font-size", "var(--lumo-font-size-l)");
        scanCounter.getStyle().set("color", "var(--lumo-primary-color)");

        // Status
        statusLabel.setText(session.isActive() ? "● QR is ACTIVE" : "● QR has EXPIRED");
        statusLabel.getStyle().set("color",
                session.isActive() ? "var(--lumo-success-color)" : "var(--lumo-error-color)");
        statusLabel.getStyle().set("font-weight", "bold");

        // Regenerate button
        Button regenerateButton = new Button("Regenerate QR", new Icon(VaadinIcon.REFRESH), e -> {
            if (session != null) {
                // Navigate back to slotId-based generation to get a fresh session
                // We need the slotId from the current session — but it's not in the DTO
                // For now, navigate back to dashboard
                UI.getCurrent().navigate("teacher");
            }
        });
        regenerateButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        // Layout
        HorizontalLayout topBar = new HorizontalLayout(backButton);
        topBar.setWidthFull();

        VerticalLayout content = new VerticalLayout(
                subjectLabel,
                semesterLabel,
                qrImage,
                countdownLabel,
                statusLabel,
                scanCounter,
                regenerateButton
        );
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.setSpacing(true);
        content.setPadding(true);

        add(topBar, content);
    }

    /**
     * Update the countdown timer display.
     */
    private void updateCountdown() {
        if (session == null) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = session.getExpiresAt();

        if (now.isAfter(expiresAt)) {
            countdownLabel.setText("EXPIRED");
            countdownLabel.getStyle().set("color", "var(--lumo-error-color)");
        } else {
            long secondsLeft = Duration.between(now, expiresAt).getSeconds();
            long minutes = secondsLeft / 60;
            long seconds = secondsLeft % 60;
            countdownLabel.setText(String.format("%d:%02d remaining", minutes, seconds));
            countdownLabel.getStyle().set("color", "var(--lumo-success-color)");
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        getUI().ifPresent(ui -> ui.setPollInterval(1000));

        // Start background polling for scan counter updates
        UI ui = attachEvent.getUI();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        pollingTask = scheduler.scheduleAtFixedRate(() -> {
            if (session != null && session.isActive()) {
                try {
                    AttendanceSessionResponse updated = qrService.getSessionStatus(session.getSessionId());
                    ui.access(() -> {
                        session = updated;
                        scanCounter.setText(updated.getPresentCount() + " / " + updated.getTotalStudents() + " scanned");
                        updateCountdown();

                        if (!updated.isActive()) {
                            statusLabel.setText("● QR has EXPIRED");
                            statusLabel.getStyle().set("color", "var(--lumo-error-color)");
                        }
                    });
                } catch (Exception ignored) {
                    // Session might have been cleaned up
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);

        // Stop polling when view is detached
        if (pollingTask != null) {
            pollingTask.cancel(true);
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    /**
     * Generate a QR code image as PNG bytes using ZXing.
     */
    private byte[] generateQrCode(String content, int size) throws WriterException, IOException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    /**
     * Show an error message.
     */
    private void showError(String message) {
        removeAll();

        Button backButton = new Button("Back to Dashboard", new Icon(VaadinIcon.ARROW_LEFT), e -> {
            UI.getCurrent().navigate("teacher");
        });
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Span error = new Span(message);
        error.getStyle().set("color", "var(--lumo-error-text-color)");
        error.getStyle().set("font-size", "var(--lumo-font-size-l)");

        add(backButton, error);
    }
}
