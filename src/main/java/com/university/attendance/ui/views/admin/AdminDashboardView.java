package com.university.attendance.ui.views.admin;

import com.university.attendance.service.AdminService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin", layout = AdminLayout.class)
@PageTitle("Admin Dashboard")
@RolesAllowed("ADMIN")
public class AdminDashboardView extends VerticalLayout {

    public AdminDashboardView(AdminService adminService) {
        setSizeFull();
        setSpacing(true);
        setPadding(true);

        H2 title = new H2("Admin Dashboard");

        HorizontalLayout statCards = new HorizontalLayout();
        statCards.setWidthFull();
        statCards.add(createStatCard("Total Faculties", String.valueOf(adminService.getAllFaculties().size())));
        statCards.add(createStatCard("Total Courses", String.valueOf(adminService.getAllCourses().size())));
        statCards.add(createStatCard("Total Students", String.valueOf(adminService.getAllStudents().size())));
        statCards.add(createStatCard("Total Teachers", String.valueOf(adminService.getAllTeachers().size())));

        HorizontalLayout navigation = new HorizontalLayout();
        
        Button facultiesBtn = new Button("Manage Faculties", e -> UI.getCurrent().navigate(FacultyManagementView.class));
        facultiesBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button coursesBtn = new Button("Manage Courses", e -> UI.getCurrent().navigate(CourseManagementView.class));
        coursesBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button subjectsBtn = new Button("Manage Subjects", e -> UI.getCurrent().navigate(SubjectManagementView.class));
        subjectsBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button studentsBtn = new Button("Manage Students", e -> UI.getCurrent().navigate(StudentManagementView.class));
        studentsBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button teachersBtn = new Button("Manage Teachers", e -> UI.getCurrent().navigate(TeacherManagementView.class));
        teachersBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        navigation.add(facultiesBtn, coursesBtn, subjectsBtn, studentsBtn, teachersBtn);

        add(title, statCards, navigation);
    }

    private VerticalLayout createStatCard(String title, String value) {
        VerticalLayout card = new VerticalLayout();
        card.getStyle().set("box-shadow", "var(--lumo-box-shadow-s)");
        card.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        card.getStyle().set("background-color", "var(--lumo-base-color)");
        card.setPadding(true);
        card.setAlignItems(Alignment.CENTER);

        Span titleSpan = new Span(title);
        titleSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        Span valueSpan = new Span(value);
        valueSpan.getStyle().set("font-size", "var(--lumo-font-size-xxxl)");
        valueSpan.getStyle().set("font-weight", "bold");

        card.add(titleSpan, valueSpan);
        return card;
    }
}
