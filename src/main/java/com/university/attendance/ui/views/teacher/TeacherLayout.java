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
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                String prn = auth.getName();
                teacherName = teacherRepository.findByPrn(prn)
                        .map(u -> u.getFirstName() + " " + u.getLastName())
                        .orElse("Teacher");
            }
        } catch (Exception ignored) {
        }
        Span nameSpan = new Span(teacherName);
        nameSpan.getStyle().set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        Button logoutBtn = new Button("Logout", e -> {
            UI.getCurrent().getPage().executeJs(
                "fetch('/api/v1/auth/logout', {method:'POST'}).then(() => { window.location.href='/'; })"
            );
        });
        logoutBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        Span divider = new Span("|");
        divider.getStyle().set("color", "var(--lumo-contrast-30pct)");

        HorizontalLayout right = new HorizontalLayout(
                navDashboard, navTimetable, navSubjectReport, navSessionReport, divider, nameSpan, logoutBtn);
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
