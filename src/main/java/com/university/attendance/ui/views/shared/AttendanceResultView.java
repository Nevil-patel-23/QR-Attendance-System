package com.university.attendance.ui.views.shared;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
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

import java.util.List;
import java.util.Map;

/**
 * Handles the /attend?token={uuid} URL that students reach by scanning a QR code.
 *
 * Flow:
 * 1. Student scans QR on phone → phone opens this URL in browser
 * 2. If student is NOT logged in:
 *    - Save the token in a "pending_token" cookie (maxAge=600 = 10 minutes)
 *    - Redirect to /login
 *    - After login, LoginView checks for pending_token cookie and redirects back here
 * 3. If student IS logged in:
 *    - Show a placeholder "Scanner coming soon" message (actual validation is Slice 9)
 *
 * This is @AnonymousAllowed because the whole point is that students reach
 * this URL by scanning a QR code — they might not be logged in yet.
 */
@Route("attend")
@PageTitle("Attendance | QR Attendance")
@AnonymousAllowed
public class AttendanceResultView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
        String token = null;

        if (params.containsKey("token")) {
            token = params.get("token").get(0);
        }

        if (token == null || token.isEmpty()) {
            showError("No attendance token provided. Please scan a valid QR code.");
            return;
        }

        // Check if the user is logged in by looking for the JWT cookie
        HttpServletRequest request = (HttpServletRequest) VaadinService.getCurrentRequest();
        boolean isLoggedIn = false;

        if (request != null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwt".equals(cookie.getName()) && !cookie.getValue().isEmpty()) {
                        isLoggedIn = true;
                        break;
                    }
                }
            }
        }

        if (!isLoggedIn) {
            // Save token in a cookie so LoginView can redirect back after login
            HttpServletResponse response = (HttpServletResponse) VaadinService.getCurrentResponse();
            if (response != null) {
                Cookie pendingToken = new Cookie("pending_token", token);
                pendingToken.setMaxAge(600);  // 10 minutes
                pendingToken.setPath("/");
                pendingToken.setHttpOnly(true);
                pendingToken.setSecure(false);  // set true in production
                response.addCookie(pendingToken);
            }

            // Redirect to login
            UI.getCurrent().navigate("");
            return;
        }

        // Student is logged in — show placeholder for Slice 9
        showPlaceholder(token);
    }

    /**
     * Show the placeholder message for logged-in students.
     * Actual scan validation will be implemented in Slice 9.
     */
    private void showPlaceholder(String token) {
        removeAll();

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        H1 title = new H1("📱 QR Scanned!");
        title.getStyle().set("font-size", "var(--lumo-font-size-xxl)");

        Paragraph message = new Paragraph(
                "Your scan has been received. Attendance validation will be processed shortly.");
        message.getStyle().set("font-size", "var(--lumo-font-size-l)");
        message.getStyle().set("color", "var(--lumo-secondary-text-color)");
        message.getStyle().set("text-align", "center");
        message.setMaxWidth("400px");

        Span tokenInfo = new Span("Token: " + token.substring(0, 8) + "...");
        tokenInfo.getStyle().set("color", "var(--lumo-tertiary-text-color)");
        tokenInfo.getStyle().set("font-size", "var(--lumo-font-size-s)");

        Span comingSoon = new Span("🔧 Full validation coming in Slice 9");
        comingSoon.getStyle().set("color", "var(--lumo-tertiary-text-color)");
        comingSoon.getStyle().set("font-style", "italic");

        VerticalLayout card = new VerticalLayout(title, message, tokenInfo, comingSoon);
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
        card.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        card.getStyle().set("background-color", "var(--lumo-base-color)");
        card.setPadding(true);
        card.setMaxWidth("500px");

        add(card);
    }

    /**
     * Show an error message.
     */
    private void showError(String message) {
        removeAll();

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        H1 title = new H1("❌ Error");
        Paragraph errorMsg = new Paragraph(message);
        errorMsg.getStyle().set("color", "var(--lumo-error-text-color)");

        add(title, errorMsg);
    }
}
