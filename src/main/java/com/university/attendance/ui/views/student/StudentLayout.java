package com.university.attendance.ui.views.student;

import com.university.attendance.repository.StudentRepository;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Shared layout for all student pages. Implements BeforeEnterObserver to
 * enforce STUDENT-only access via SecurityContextHolder at the layout level.
 */
public class StudentLayout extends AppLayout implements BeforeEnterObserver {

    private Span viewTitle;

    public StudentLayout(StudentRepository studentRepository) {
        createHeader(studentRepository);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().isEmpty()) {
            event.getUI().getPage().setLocation("/");
            return;
        }
        boolean isStudent = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_STUDENT".equals(role));
        if (!isStudent) {
            boolean isAdmin = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> "ROLE_ADMIN".equals(role));
            boolean isTeacher = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> "ROLE_TEACHER".equals(role));

            if (isAdmin) {
                event.getUI().getPage().setLocation("/admin");
            } else if (isTeacher) {
                event.getUI().getPage().setLocation("/teacher");
            } else {
                event.getUI().getPage().setLocation("/");
            }
        }
    }

    private void createHeader(StudentRepository studentRepository) {
        // ── Left side: App name ──
        RouterLink homeLink = new RouterLink("QR Attendance", StudentDashboardView.class);
        homeLink.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);
        homeLink.getStyle().set("text-decoration", "none").set("color", "var(--lumo-primary-color)");

        Span separator = new Span(" > ");
        separator.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.Horizontal.SMALL);
        viewTitle = new Span();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE);

        HorizontalLayout left = new HorizontalLayout(homeLink, separator, viewTitle);
        left.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        // ── Right side: nav links + name + logout ──
        RouterLink dashLink = new RouterLink("Dashboard", StudentDashboardView.class);
        dashLink.getStyle().set("text-decoration", "none");
        RouterLink attendLink = new RouterLink("Attendance", AttendanceDetailView.class);
        attendLink.getStyle().set("text-decoration", "none");
        RouterLink ttLink = new RouterLink("Timetable", StudentTimetableView.class);
        ttLink.getStyle().set("text-decoration", "none");
        RouterLink subLink = new RouterLink("Subjects", MySubjectsView.class);
        subLink.getStyle().set("text-decoration", "none");

        // Student full name from SecurityContextHolder PRN → DB lookup
        String studentName = resolveStudentFullName(studentRepository);
        String prn = "";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            prn = auth.getName();
        }

        String initials = "";
        String[] parts = studentName.split(" ");
        if (parts.length > 0 && !parts[0].isEmpty()) initials += parts[0].substring(0, 1).toUpperCase();
        if (parts.length > 1 && !parts[1].isEmpty()) initials += parts[1].substring(0, 1).toUpperCase();
        if (initials.isEmpty()) initials = "S";

        com.vaadin.flow.component.avatar.Avatar avatar = new com.vaadin.flow.component.avatar.Avatar(studentName);
        avatar.setAbbreviation(initials);
        avatar.getStyle().set("background-color", "#9c27b0"); // Purple
        avatar.getStyle().set("color", "white");
        avatar.getStyle().set("cursor", "pointer");

        com.vaadin.flow.component.contextmenu.ContextMenu contextMenu = new com.vaadin.flow.component.contextmenu.ContextMenu(avatar);
        contextMenu.setOpenOnClick(true);

        com.vaadin.flow.component.avatar.Avatar largeAvatar = new com.vaadin.flow.component.avatar.Avatar(studentName);
        largeAvatar.setAbbreviation(initials);
        largeAvatar.getStyle()
            .set("background-color", "#3164d1ff") // Purple
            .set("color", "white")
            .set("width", "var(--lumo-size-xl)")
            .set("height", "var(--lumo-size-xl)");

        Span nameLabel = new Span(studentName);
        nameLabel.getStyle().set("font-weight", "bold");
        Span prnLabel = new Span(prn.isEmpty() ? "N/A" : prn);
        prnLabel.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");

        com.vaadin.flow.component.orderedlayout.VerticalLayout headerLayout = new com.vaadin.flow.component.orderedlayout.VerticalLayout(largeAvatar, nameLabel, prnLabel);
        headerLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
        headerLayout.setSpacing(false);
        headerLayout.getStyle().set("padding", "var(--lumo-space-m)");

        contextMenu.add(headerLayout);
        contextMenu.add(new com.vaadin.flow.component.html.Hr());

        contextMenu.addItem("My Profile", e -> UI.getCurrent().navigate("student/profile"));
        contextMenu.addItem("Change Password", e -> UI.getCurrent().navigate("student/profile/password"));
        contextMenu.add(new com.vaadin.flow.component.html.Hr());

        Span logoutSpan = new Span("Logout");
        logoutSpan.getStyle().set("color", "var(--lumo-error-text-color)").set("font-weight", "bold");
        contextMenu.addItem(logoutSpan, e -> {
            UI.getCurrent().getPage().executeJs(
                    "fetch('/api/v1/auth/logout', {method:'POST'}).then(() => { window.location.href='/'; })");
        });

        Span divider = new Span("|");
        divider.getStyle().set("color", "var(--lumo-contrast-30pct)");

        HorizontalLayout right = new HorizontalLayout(
                dashLink, attendLink, ttLink, subLink, divider, avatar);
        right.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        right.setSpacing(true);

        // ── Full header ──
        HorizontalLayout fullHeader = new HorizontalLayout(left, right);
        fullHeader.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        fullHeader.setWidthFull();
        fullHeader.expand(left);
        fullHeader.addClassNames(LumoUtility.Padding.Vertical.SMALL, LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(fullHeader);
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        if (getContent() != null) {
            PageTitle titleAnnotation = getContent().getClass().getAnnotation(PageTitle.class);
            String title = titleAnnotation != null ? titleAnnotation.value() : "";
            viewTitle.setText(title);
        }
    }

    /**
     * Resolve student full name via PRN from SecurityContextHolder → DB.
     */
    private String resolveStudentFullName(StudentRepository studentRepository) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                String prn = auth.getName();
                return studentRepository.findByPrn(prn)
                        .map(s -> s.getFirstName() + " " + s.getLastName())
                        .orElse("Student");
            }
        } catch (Exception ignored) {
        }
        return "Student";
    }
}
