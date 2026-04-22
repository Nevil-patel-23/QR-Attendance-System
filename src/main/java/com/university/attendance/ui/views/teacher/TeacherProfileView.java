package com.university.attendance.ui.views.teacher;

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

@Route(value = "teacher/profile", layout = TeacherLayout.class)
@PageTitle("Teacher Profile")
@RolesAllowed("TEACHER")
public class TeacherProfileView extends VerticalLayout implements BeforeEnterObserver {

    public TeacherProfileView(ProfileService profileService) {
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
        
        TextField phoneField = new TextField("Phone");
        phoneField.setValue(profile.getPhone() != null ? profile.getPhone() : "");
        phoneField.setReadOnly(true);

        TextField roleField = new TextField("Role");
        roleField.setValue(profile.getRole() != null ? profile.getRole().name() : "");
        roleField.setReadOnly(true);
        
        TextField facultyField = new TextField("Faculty Name");
        facultyField.setValue(profile.getFacultyName() != null ? profile.getFacultyName() : "");
        facultyField.setReadOnly(true);

        form.add(prnField, nameField, phoneField, roleField, facultyField);
        
        add(title, form);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) {
            event.rerouteTo("");
        }
    }
}
