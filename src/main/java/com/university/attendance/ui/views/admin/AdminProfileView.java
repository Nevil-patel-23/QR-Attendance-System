package com.university.attendance.ui.views.admin;

import com.university.attendance.dto.response.UserProfileResponse;
import com.university.attendance.service.ProfileService;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Route(value = "admin/profile", layout = AdminLayout.class)
@PageTitle("Admin Profile")
@RolesAllowed("ADMIN")
public class AdminProfileView extends VerticalLayout implements BeforeEnterObserver {

    public AdminProfileView(ProfileService profileService) {
        setSpacing(true);
        setPadding(true);

        UserProfileResponse profile = profileService.getProfile();

        H2 title = new H2("My Profile");
        
        FormLayout form = new FormLayout();
        
        TextField prnField = new TextField("PRN");
        prnField.setValue(profile.getPrn() != null ? profile.getPrn() : "");
        prnField.setReadOnly(true);

        TextField nameField = new TextField("Full Name");
        nameField.setValue(profile.getFullName() != null ? profile.getFullName() : "");
        nameField.setReadOnly(true);

        TextField roleField = new TextField("Role");
        roleField.setValue(profile.getRole() != null ? profile.getRole().name() : "");
        roleField.setReadOnly(true);

        form.add(prnField, nameField, roleField);
        
        add(title, form);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            event.rerouteTo("");
        }
    }
}
