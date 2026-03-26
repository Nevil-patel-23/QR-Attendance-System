package com.university.attendance.ui.views.shared;

import com.university.attendance.dto.request.LoginRequest;
import com.university.attendance.dto.response.AuthResponse;
import com.university.attendance.service.AuthService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.router.QueryParameters;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;

@Route("")
@PageTitle("Login | QR Attendance")
@AnonymousAllowed
public class LoginView extends VerticalLayout {

    private final AuthService authService;

    public LoginView(AuthService authService) {
        this.authService = authService;

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        // UI Components
        H1 title = new H1("University Login");
        title.getStyle().set("font-size", "var(--lumo-font-size-xxl)");

        TextField prnField = new TextField("PRN");
        prnField.setPlaceholder("10-digit PRN");
        prnField.setPattern("[0-9]{10}");
        prnField.setErrorMessage("PRN must be exactly 10 digits");
        prnField.setRequiredIndicatorVisible(true);
        prnField.setWidth("100%");
        prnField.setMaxLength(10);
        prnField.setAllowedCharPattern("[0-9]");

        PasswordField passwordField = new PasswordField("Password");
        passwordField.setPlaceholder("Enter password");
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setWidth("100%");

        Span errorMessage = new Span();
        errorMessage.getStyle().set("color", "var(--lumo-error-text-color)");
        errorMessage.setVisible(false);

        Button loginButton = new Button("Login", e -> {
            errorMessage.setVisible(false);
            
            String prn = prnField.getValue().trim();
            String password = passwordField.getValue();

            if (prn.length() != 10) {
                errorMessage.setText("PRN must be exactly 10 digits");
                errorMessage.setVisible(true);
                return;
            }

            if (password.isEmpty()) {
                errorMessage.setText("Password cannot be empty");
                errorMessage.setVisible(true);
                return;
            }

            try {
                LoginRequest loginReq = new LoginRequest(prn, password);
                AuthResponse response = this.authService.login(loginReq);
                
                // Store JWT in HTTP-only cookie (sent automatically on every request)
                Cookie jwtCookie = new Cookie("jwt", response.getToken());
                jwtCookie.setHttpOnly(true);
                jwtCookie.setPath("/");
                jwtCookie.setMaxAge(86400); // 24 hours
                jwtCookie.setSecure(false);  // set true in production
                HttpServletResponse httpResponse = (HttpServletResponse) VaadinService.getCurrentResponse();
                httpResponse.addCookie(jwtCookie);

                // Check for pending_token cookie (set by AttendanceResultView when
                // a student scans a QR but wasn't logged in)
                // IMPORTANT: Only redirect to /attend for STUDENT role.
                // Teachers/admins must always reach their dashboards.
                String role = response.getRole();

                if ("STUDENT".equalsIgnoreCase(role)) {
                    HttpServletRequest httpRequest = (HttpServletRequest) VaadinService.getCurrentRequest();
                    String pendingToken = null;
                    Cookie[] cookies = httpRequest.getCookies();
                    if (cookies != null) {
                        for (Cookie cookie : cookies) {
                            if ("pending_token".equals(cookie.getName())) {
                                pendingToken = cookie.getValue();
                                break;
                            }
                        }
                    }

                    if (pendingToken != null && !pendingToken.isEmpty()) {
                        // Clear the cookie — it's been consumed
                        Cookie clearCookie = new Cookie("pending_token", "");
                        clearCookie.setMaxAge(0);
                        clearCookie.setPath("/");
                        clearCookie.setHttpOnly(true);
                        httpResponse.addCookie(clearCookie);

                        // Redirect back to the scan page with the token
                        // Use a hard browser redirect instead of Vaadin router to ensure context reloads
                        UI.getCurrent().getPage().setLocation("/attend?token=" + pendingToken);
                    } else {
                        UI.getCurrent().getPage().setLocation("student");
                    }
                } else if ("ADMIN".equalsIgnoreCase(role)) {
                    // Always clear any stale pending_token for non-students
                    clearPendingTokenCookie(httpResponse);
                    UI.getCurrent().getPage().setLocation("admin");
                } else if ("TEACHER".equalsIgnoreCase(role)) {
                    clearPendingTokenCookie(httpResponse);
                    UI.getCurrent().getPage().setLocation("teacher");
                } else {
                    errorMessage.setText("Unknown role assigned.");
                    errorMessage.setVisible(true);
                }

            } catch (Exception ex) {
                // Catches ValidationException and ResourceNotFoundException
                errorMessage.setText(ex.getMessage() != null ? ex.getMessage() : "Invalid PRN or Password");
                errorMessage.setVisible(true);
            }
        });
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidth("100%");

        VerticalLayout loginCard = new VerticalLayout(title, prnField, passwordField, errorMessage, loginButton);
        loginCard.setAlignItems(FlexComponent.Alignment.CENTER);
        loginCard.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
        loginCard.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        loginCard.getStyle().set("background-color", "var(--lumo-base-color)");
        loginCard.setPadding(true);
        loginCard.setMaxWidth("400px");
        loginCard.setWidth("100%");

        add(loginCard);
    }

    /**
     * Clear any stale pending_token cookie (e.g. for non-student roles).
     */
    private void clearPendingTokenCookie(HttpServletResponse httpResponse) {
        Cookie clearCookie = new Cookie("pending_token", "");
        clearCookie.setMaxAge(0);
        clearCookie.setPath("/");
        clearCookie.setHttpOnly(true);
        httpResponse.addCookie(clearCookie);
    }
}
