package com.university.attendance.ui.views.admin;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("admin/dashboard")
@PageTitle("Admin Dashboard")
public class AdminDashboardView extends VerticalLayout {

    public AdminDashboardView() {
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        add(new H1("Admin Dashboard — coming soon"));
    }
}
