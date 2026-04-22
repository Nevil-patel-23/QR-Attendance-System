package com.university.attendance.ui.views.student;

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

@Route(value = "student/profile", layout = StudentLayout.class)
@PageTitle("Student Profile")
@RolesAllowed("STUDENT")
public class StudentProfileView extends VerticalLayout implements BeforeEnterObserver {

    public StudentProfileView(ProfileService profileService) {
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
        
        TextField courseField = new TextField("Course");
        courseField.setValue(profile.getCourseName() != null ? profile.getCourseName() : "");
        courseField.setReadOnly(true);
        
        TextField semesterField = new TextField("Semester");
        semesterField.setValue(profile.getSemesterLabel() != null ? profile.getSemesterLabel() : "");
        semesterField.setReadOnly(true);
        
        TextField batchField = new TextField("Batch Year");
        batchField.setValue(profile.getBatchYear() != null ? profile.getBatchYear().toString() : "");
        batchField.setReadOnly(true);

        form.add(prnField, nameField, phoneField, roleField, courseField, semesterField, batchField);
        
        add(title, form);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))) {
            event.rerouteTo("");
        }
    }
}
