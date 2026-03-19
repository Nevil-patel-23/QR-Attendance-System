package com.university.attendance.ui.views.admin;

import com.university.attendance.dto.request.CreateFacultyRequest;
import com.university.attendance.dto.response.FacultyResponse;
import com.university.attendance.exception.ValidationException;
import com.university.attendance.service.AdminService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.UUID;

@Route(value = "admin/faculties", layout = AdminLayout.class)
@PageTitle("Faculty Management")
@RolesAllowed("ADMIN")
public class FacultyManagementView extends VerticalLayout {

    private final AdminService adminService;
    private final Grid<FacultyResponse> grid = new Grid<>(FacultyResponse.class, false);
    private final TextField nameField = new TextField("Faculty Name");
    private final TextField codeField = new TextField("Faculty Code");
    private final Button saveButton = new Button("Save");

    private UUID editingId = null;

    public FacultyManagementView(AdminService adminService) {
        this.adminService = adminService;
        
        setSizeFull();
        
        H2 title = new H2("Faculty Management");
        
        configureGrid();
        configureForm();

        HorizontalLayout formLayout = new HorizontalLayout(nameField, codeField, saveButton);
        formLayout.setAlignItems(Alignment.BASELINE);
        formLayout.setWidthFull();

        add(title, formLayout, grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(FacultyResponse::getName).setHeader("Name").setSortable(true);
        grid.addColumn(FacultyResponse::getCode).setHeader("Code").setSortable(true);
        grid.addColumn(FacultyResponse::getCreatedAt).setHeader("Created Date").setSortable(true);
        
        grid.addComponentColumn(faculty -> {
            Button editBtn = new Button("Edit", e -> editFaculty(faculty));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            
            Button deleteBtn = new Button("Delete", e -> confirmDelete(faculty));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            
            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("Actions");
    }

    private void configureForm() {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveFaculty());
    }

    private void refreshGrid() {
        grid.setItems(adminService.getAllFaculties());
    }

    private void editFaculty(FacultyResponse faculty) {
        editingId = faculty.getFacultyId();
        nameField.setValue(faculty.getName());
        codeField.setValue(faculty.getCode());
        saveButton.setText("Update");
    }

    private void saveFaculty() {
        try {
            CreateFacultyRequest request = new CreateFacultyRequest(nameField.getValue(), codeField.getValue());
            if (editingId == null) {
                adminService.createFaculty(request);
                showSuccess("Faculty created successfully");
            } else {
                adminService.updateFaculty(editingId, request);
                showSuccess("Faculty updated successfully");
                editingId = null;
                saveButton.setText("Save");
            }
            nameField.clear();
            codeField.clear();
            refreshGrid();
        } catch (ValidationException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Failed to save faculty");
        }
    }

    private void confirmDelete(FacultyResponse faculty) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Faculty");
        dialog.setText("Are you sure you want to delete " + faculty.getName() + "?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            try {
                adminService.deleteFaculty(faculty.getFacultyId());
                showSuccess("Faculty deleted successfully");
                refreshGrid();
            } catch (ValidationException ex) {
                showError(ex.getMessage());
            } catch (Exception ex) {
                showError("Error deleting faculty");
            }
        });
        dialog.open();
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.setPosition(Notification.Position.TOP_END);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.setPosition(Notification.Position.TOP_END);
    }
}
