package com.university.attendance.ui.views.teacher;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("teacher/dashboard")
@PageTitle("Teacher Dashboard")
public class TeacherDashboardView extends VerticalLayout {

    public TeacherDashboardView() {
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        add(new H1("Teacher Dashboard — coming soon"));
    }
}
