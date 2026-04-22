package com.university.attendance.ui.views.teacher;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.university.attendance.repository.TeacherRepository;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class TeacherLayout extends AppLayout implements BeforeEnterObserver {

    private Span viewTitle;

    public TeacherLayout(TeacherRepository teacherRepository) {
        createHeader(teacherRepository);
    }

    private void createHeader(TeacherRepository teacherRepository) {
        // ── Left side: App name ──
        RouterLink homeLink = new RouterLink("QR Attendance", TeacherDashboardView.class);
        homeLink.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);
        homeLink.getStyle().set("text-decoration", "none").set("color", "var(--lumo-primary-color)");

        Span separator = new Span(" > ");
        separator.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.Horizontal.SMALL);
        viewTitle = new Span();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE);

        HorizontalLayout left = new HorizontalLayout(homeLink, separator, viewTitle);
        left.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        // ── Right side: nav links + name + logout ──
        RouterLink navDashboard = new RouterLink("Dashboard", TeacherDashboardView.class);
        navDashboard.getStyle().set("text-decoration", "none").set("color", "var(--lumo-body-text-color)");
        
        RouterLink navTimetable = new RouterLink("Timetable", TeacherTimetableView.class);
        navTimetable.getStyle().set("text-decoration", "none").set("color", "var(--lumo-body-text-color)");
        
        RouterLink navSubjectReport = new RouterLink("Subjects Report", AttendanceBySubjectView.class);
        navSubjectReport.getStyle().set("text-decoration", "none").set("color", "var(--lumo-body-text-color)");
        
        RouterLink navSessionReport = new RouterLink("Sessions Report", AttendanceBySessionView.class);
        navSessionReport.getStyle().set("text-decoration", "none").set("color", "var(--lumo-body-text-color)");

        String teacherName = "Teacher";
        String prn = "";
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                prn = auth.getName();
                teacherName = teacherRepository.findByPrn(prn)
                        .map(u -> u.getFirstName() + " " + u.getLastName())
                        .orElse("Teacher");
            }
        } catch (Exception ignored) {
        }

        String initials = "";
        String[] parts = teacherName.split(" ");
        if (parts.length > 0 && !parts[0].isEmpty()) initials += parts[0].substring(0, 1).toUpperCase();
        if (parts.length > 1 && !parts[1].isEmpty()) initials += parts[1].substring(0, 1).toUpperCase();
        if (initials.isEmpty()) initials = "T";

        com.vaadin.flow.component.avatar.Avatar avatar = new com.vaadin.flow.component.avatar.Avatar(teacherName);
        avatar.setAbbreviation(initials);
        avatar.getStyle().set("background-color", "var(--lumo-success-color)"); // Green
        avatar.getStyle().set("color", "white");
        avatar.getStyle().set("cursor", "pointer");

        com.vaadin.flow.component.contextmenu.ContextMenu contextMenu = new com.vaadin.flow.component.contextmenu.ContextMenu(avatar);
        contextMenu.setOpenOnClick(true);

        com.vaadin.flow.component.avatar.Avatar largeAvatar = new com.vaadin.flow.component.avatar.Avatar(teacherName);
        largeAvatar.setAbbreviation(initials);
        largeAvatar.getStyle()
            .set("background-color", "var(--lumo-success-color)")
            .set("color", "white")
            .set("width", "var(--lumo-size-xl)")
            .set("height", "var(--lumo-size-xl)");

        Span nameLabel = new Span(teacherName);
        nameLabel.getStyle().set("font-weight", "bold");
        Span prnLabel = new Span(prn.isEmpty() ? "N/A" : prn);
        prnLabel.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");

        com.vaadin.flow.component.orderedlayout.VerticalLayout headerLayout = new com.vaadin.flow.component.orderedlayout.VerticalLayout(largeAvatar, nameLabel, prnLabel);
        headerLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
        headerLayout.setSpacing(false);
        headerLayout.getStyle().set("padding", "var(--lumo-space-m)");

        contextMenu.add(headerLayout);
        contextMenu.add(new com.vaadin.flow.component.html.Hr());

        contextMenu.addItem("My Profile", e -> UI.getCurrent().navigate("teacher/profile"));
        contextMenu.addItem("Change Password", e -> UI.getCurrent().navigate("teacher/profile/password"));
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
                navDashboard, navTimetable, navSubjectReport, navSessionReport, divider, avatar);
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

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().isEmpty()) {
            event.getUI().getPage().setLocation("/");
            return;
        }
        
        boolean isTeacher = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));

        if (!isTeacher) {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isStudent = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
                    
            if (isAdmin) {
                event.getUI().getPage().setLocation("/admin");
            } else if (isStudent) {
                event.getUI().getPage().setLocation("/student");
            } else {
                event.getUI().getPage().setLocation("/");
            }
            return;
        }
    }
}
