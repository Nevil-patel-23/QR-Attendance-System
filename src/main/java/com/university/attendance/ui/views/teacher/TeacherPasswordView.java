package com.university.attendance.ui.views.teacher;

import com.university.attendance.dto.request.PasswordChangeRequest;
import com.university.attendance.service.ProfileService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Route(value = "teacher/profile/password", layout = TeacherLayout.class)
@PageTitle("Change Password")
@RolesAllowed("TEACHER")
public class TeacherPasswordView extends VerticalLayout implements BeforeEnterObserver {

    public TeacherPasswordView(ProfileService profileService) {
        setSpacing(true);
        setPadding(true);

        H2 title = new H2("Change Password");

        FormLayout form = new FormLayout();
        
        PasswordField oldPassword = new PasswordField("Old Password");
        PasswordField newPassword = new PasswordField("New Password");
        PasswordField confirmPassword = new PasswordField("Confirm New Password");

        Button updateButton = new Button("Update Password", e -> {
            try {
                PasswordChangeRequest req = new PasswordChangeRequest();
                req.setOldPassword(oldPassword.getValue());
                req.setNewPassword(newPassword.getValue());
                req.setConfirmPassword(confirmPassword.getValue());
                profileService.updatePassword(req);
                Notification.show("Password updated successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                oldPassword.clear();
                newPassword.clear();
                confirmPassword.clear();
            } catch (Exception ex) {
                Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        form.add(oldPassword, newPassword, confirmPassword);
        
        add(title, form, updateButton);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) {
            event.rerouteTo("");
        }
    }
}
