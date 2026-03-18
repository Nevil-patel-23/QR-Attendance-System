package com.university.attendance.ui.views.student;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("student/dashboard")
@PageTitle("Student Dashboard")
public class StudentDashboardView extends VerticalLayout {

    public StudentDashboardView() {
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        add(new H1("Student Dashboard — coming soon"));
    }
}
