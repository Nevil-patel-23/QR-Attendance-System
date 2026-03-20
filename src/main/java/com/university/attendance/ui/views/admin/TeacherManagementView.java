package com.university.attendance.ui.views.admin;

import com.university.attendance.dto.request.CreateTeacherRequest;
import com.university.attendance.dto.response.FacultyResponse;
import com.university.attendance.dto.response.TeacherImportRow;
import com.university.attendance.dto.response.TeacherResponse;
import com.university.attendance.exception.ValidationException;
import com.university.attendance.service.AdminService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.UUID;

@Route(value = "admin/teachers", layout = AdminLayout.class)
@PageTitle("Teacher Management")
@RolesAllowed("ADMIN")
public class TeacherManagementView extends VerticalLayout {

    private final AdminService adminService;
    private final Grid<TeacherResponse> grid = new Grid<>(TeacherResponse.class, false);

    private final TextField prnField = new TextField("PRN");
    private final TextField firstNameField = new TextField("First Name");
    private final TextField lastNameField = new TextField("Last Name");
    private final TextField phoneField = new TextField("Phone");
    private final ComboBox<FacultyResponse> facultyCombo = new ComboBox<>("Faculty");
    private final TextField designationField = new TextField("Designation");
    private final Button saveButton = new Button("Add Teacher");

    private UUID editingId = null;

    public TeacherManagementView(AdminService adminService) {
        this.adminService = adminService;
        setSizeFull();

        H2 title = new H2("Teacher Management");

        configureGrid();
        configureForm();

        HorizontalLayout formRow1 = new HorizontalLayout(prnField, firstNameField, lastNameField, phoneField);
        formRow1.setAlignItems(Alignment.BASELINE);
        formRow1.setWidthFull();

        HorizontalLayout formRow2 = new HorizontalLayout(facultyCombo, designationField, saveButton);
        formRow2.setAlignItems(Alignment.BASELINE);
        formRow2.setWidthFull();

        // Excel upload with preview dialog
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".xlsx", ".xls");
        upload.setUploadButton(new Button("Import from Excel"));
        upload.addSucceededListener(event -> {
            try {
                List<TeacherImportRow> previewRows = adminService.previewTeacherImport(buffer.getInputStream());
                showImportPreviewDialog(previewRows);
            } catch (Exception ex) {
                showError("Excel import failed: " + ex.getMessage());
            }
        });

        add(title, formRow1, formRow2, upload, grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(TeacherResponse::getPrn).setHeader("PRN").setSortable(true);
        grid.addColumn(TeacherResponse::getFirstName).setHeader("First Name").setSortable(true);
        grid.addColumn(TeacherResponse::getLastName).setHeader("Last Name").setSortable(true);
        grid.addColumn(TeacherResponse::getPhone).setHeader("Phone");
        grid.addColumn(TeacherResponse::getFacultyName).setHeader("Faculty").setSortable(true);
        grid.addColumn(TeacherResponse::getDesignation).setHeader("Designation").setSortable(true);
        grid.addColumn(t -> t.isActive() ? "Active" : "Inactive").setHeader("Status").setSortable(true);

        grid.addComponentColumn(teacher -> {
            Button editBtn = new Button("Edit", e -> editTeacher(teacher));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            Button deactivateBtn = new Button("Deactivate", e -> confirmDeactivate(teacher));
            deactivateBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deactivateBtn.setEnabled(teacher.isActive());

            return new HorizontalLayout(editBtn, deactivateBtn);
        }).setHeader("Actions");
    }

    private void configureForm() {
        List<FacultyResponse> faculties = adminService.getAllFaculties();
        facultyCombo.setItems(faculties);
        facultyCombo.setItemLabelGenerator(FacultyResponse::getName);

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveTeacher());
    }

    private void refreshGrid() {
        grid.setItems(adminService.getAllTeachers());
    }

    private void editTeacher(TeacherResponse teacher) {
        editingId = teacher.getTeacherId();
        prnField.setValue(teacher.getPrn());
        prnField.setReadOnly(true);
        firstNameField.setValue(teacher.getFirstName());
        lastNameField.setValue(teacher.getLastName());
        phoneField.setValue(teacher.getPhone() != null ? teacher.getPhone() : "");
        designationField.setValue(teacher.getDesignation() != null ? teacher.getDesignation() : "");
        // Pre-select faculty by matching name
        facultyCombo.getListDataView().getItems()
                .filter(f -> f.getName().equals(teacher.getFacultyName()))
                .findFirst()
                .ifPresent(facultyCombo::setValue);
        saveButton.setText("Update Teacher");
    }

    private void saveTeacher() {
        try {
            FacultyResponse faculty = facultyCombo.getValue();
            if (faculty == null) {
                showError("Please select a faculty");
                return;
            }

            CreateTeacherRequest request = new CreateTeacherRequest(
                    prnField.getValue(),
                    firstNameField.getValue(),
                    lastNameField.getValue(),
                    phoneField.getValue(),
                    faculty.getFacultyId(),
                    designationField.getValue()
            );

            if (editingId == null) {
                adminService.createTeacher(request);
                showSuccess("Teacher created successfully");
            } else {
                adminService.updateTeacher(editingId, request);
                showSuccess("Teacher updated successfully");
                editingId = null;
                prnField.setReadOnly(false);
                saveButton.setText("Add Teacher");
            }

            clearForm();
            refreshGrid();
        } catch (ValidationException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Failed to save teacher: " + ex.getMessage());
        }
    }

    /**
     * Shows a preview dialog with the parsed teacher Excel rows.
     * Valid rows show "Ready" in green, invalid rows show error in red.
     * Admin clicks "Confirm Import" to insert only valid rows, or "Cancel" to abort.
     */
    private void showImportPreviewDialog(List<TeacherImportRow> previewRows) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Import Preview");
        dialog.setWidth("800px");
        dialog.setHeight("600px");

        Grid<TeacherImportRow> previewGrid = new Grid<>(TeacherImportRow.class, false);
        previewGrid.addColumn(TeacherImportRow::getRowNumber).setHeader("Row").setWidth("60px");
        previewGrid.addColumn(TeacherImportRow::getPrn).setHeader("PRN");
        previewGrid.addColumn(TeacherImportRow::getFirstName).setHeader("First Name");
        previewGrid.addColumn(TeacherImportRow::getLastName).setHeader("Last Name");
        previewGrid.addColumn(TeacherImportRow::getPhone).setHeader("Phone");
        previewGrid.addColumn(TeacherImportRow::getFacultyCode).setHeader("Faculty Code");
        previewGrid.addColumn(TeacherImportRow::getDesignation).setHeader("Designation");
        previewGrid.addComponentColumn(row -> {
            Span statusSpan = new Span(row.getErrorMessage());
            if (row.isValid()) {
                statusSpan.getStyle().set("color", "green");
                statusSpan.getStyle().set("font-weight", "bold");
            } else {
                statusSpan.getStyle().set("color", "red");
                statusSpan.getStyle().set("font-weight", "bold");
            }
            return statusSpan;
        }).setHeader("Status").setWidth("200px");

        previewGrid.setItems(previewRows);
        previewGrid.setSizeFull();

        long validCount = previewRows.stream().filter(TeacherImportRow::isValid).count();
        long invalidCount = previewRows.size() - validCount;
        Span summary = new Span(validCount + " valid, " + invalidCount + " invalid out of " + previewRows.size() + " rows");

        Button confirmBtn = new Button("Confirm Import", e -> {
            var result = adminService.confirmTeacherImport(previewRows);
            dialog.close();
            showSuccess(result.getSuccessCount() + " teachers imported successfully. "
                    + result.getFailedCount() + " rows skipped.");
            refreshGrid();
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        if (validCount == 0) {
            confirmBtn.setEnabled(false);
        }

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        HorizontalLayout buttons = new HorizontalLayout(confirmBtn, cancelBtn);

        VerticalLayout dialogContent = new VerticalLayout(summary, previewGrid, buttons);
        dialogContent.setSizeFull();
        dialog.add(dialogContent);
        dialog.open();
    }

    private void confirmDeactivate(TeacherResponse teacher) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Deactivate Teacher");
        dialog.setText("Are you sure you want to deactivate " + teacher.getFirstName() + " " + teacher.getLastName() + "?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Deactivate");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            try {
                adminService.deactivateTeacher(teacher.getTeacherId());
                showSuccess("Teacher deactivated");
                refreshGrid();
            } catch (Exception ex) {
                showError("Error deactivating teacher");
            }
        });
        dialog.open();
    }

    private void clearForm() {
        prnField.clear();
        firstNameField.clear();
        lastNameField.clear();
        phoneField.clear();
        facultyCombo.clear();
        designationField.clear();
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
