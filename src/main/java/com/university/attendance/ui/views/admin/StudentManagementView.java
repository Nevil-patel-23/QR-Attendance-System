package com.university.attendance.ui.views.admin;

import com.university.attendance.dto.request.CreateStudentRequest;
import com.university.attendance.dto.response.CourseResponse;
import com.university.attendance.dto.response.StudentImportRow;
import com.university.attendance.dto.response.StudentResponse;
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
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Route(value = "admin/students", layout = AdminLayout.class)
@PageTitle("Student Management")
@RolesAllowed("ADMIN")
public class StudentManagementView extends VerticalLayout {

    private final AdminService adminService;
    private final Grid<StudentResponse> grid = new Grid<>(StudentResponse.class, false);

    private final TextField prnField = new TextField("PRN");
    private final TextField firstNameField = new TextField("First Name");
    private final TextField lastNameField = new TextField("Last Name");
    private final TextField phoneField = new TextField("Phone");
    private final ComboBox<CourseResponse> courseCombo = new ComboBox<>("Course");
    private final IntegerField batchYearField = new IntegerField("Batch Year");
    private final Button saveButton = new Button("Add Student");

    private UUID editingId = null;

    public StudentManagementView(AdminService adminService) {
        this.adminService = adminService;
        setSizeFull();

        H2 title = new H2("Student Management");

        configureGrid();
        configureForm();

        HorizontalLayout formRow1 = new HorizontalLayout(prnField, firstNameField, lastNameField, phoneField);
        formRow1.setAlignItems(Alignment.BASELINE);
        formRow1.setWidthFull();

        HorizontalLayout formRow2 = new HorizontalLayout(courseCombo, batchYearField, saveButton);
        formRow2.setAlignItems(Alignment.BASELINE);
        formRow2.setWidthFull();

        // Excel upload with preview dialog
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".xlsx", ".xls");
        upload.setUploadButton(new Button("Import from Excel"));
        upload.addSucceededListener(event -> {
            try {
                List<StudentImportRow> previewRows = adminService.previewStudentImport(buffer.getInputStream());
                showImportPreviewDialog(previewRows);
            } catch (Exception ex) {
                showError("Excel import failed: " + ex.getMessage());
            }
        });

        add(title, formRow1, formRow2, upload, grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(StudentResponse::getPrn).setHeader("PRN").setSortable(true);
        grid.addColumn(StudentResponse::getFirstName).setHeader("First Name").setSortable(true);
        grid.addColumn(StudentResponse::getLastName).setHeader("Last Name").setSortable(true);
        grid.addColumn(StudentResponse::getPhone).setHeader("Phone");
        grid.addColumn(StudentResponse::getCourseName).setHeader("Course").setSortable(true);
        grid.addColumn(StudentResponse::getCurrentSemesterLabel).setHeader("Semester").setSortable(true);
        grid.addColumn(StudentResponse::getBatchYear).setHeader("Batch Year").setSortable(true);
        grid.addColumn(s -> s.isActive() ? "Active" : "Inactive").setHeader("Status").setSortable(true);

        grid.addComponentColumn(student -> {
            Button editBtn = new Button("Edit", e -> editStudent(student));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            Button deactivateBtn = new Button("Deactivate", e -> confirmDeactivate(student));
            deactivateBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deactivateBtn.setEnabled(student.isActive());

            return new HorizontalLayout(editBtn, deactivateBtn);
        }).setHeader("Actions");
    }

    private void configureForm() {
        List<CourseResponse> courses = adminService.getAllCourses();
        courseCombo.setItems(courses);
        courseCombo.setItemLabelGenerator(CourseResponse::getName);

        // Auto-calculate current academic year batch
        batchYearField.setValue(calculateAcademicBatchYear());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveStudent());
    }

    /**
     * Calculates the academic batch year in format YYYYZZ.
     * If current month >= 6 (June onwards): batchYear = currentYear*100 + (currentYear+1)%100
     * If current month < 6: batchYear = (currentYear-1)*100 + currentYear%100
     */
    private int calculateAcademicBatchYear() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        if (month >= 6) {
            return year * 100 + (year + 1) % 100;
        } else {
            return (year - 1) * 100 + year % 100;
        }
    }

    private void refreshGrid() {
        grid.setItems(adminService.getAllStudents());
    }

    private void editStudent(StudentResponse student) {
        editingId = student.getStudentId();
        prnField.setValue(student.getPrn());
        prnField.setReadOnly(true); // PRN cannot be changed
        firstNameField.setValue(student.getFirstName());
        lastNameField.setValue(student.getLastName());
        phoneField.setValue(student.getPhone() != null ? student.getPhone() : "");
        // Pre-select course by matching name
        courseCombo.getListDataView().getItems()
                .filter(c -> c.getName().equals(student.getCourseName()))
                .findFirst()
                .ifPresent(courseCombo::setValue);
        batchYearField.setValue(student.getBatchYear());
        saveButton.setText("Update Student");
    }

    private void saveStudent() {
        try {
            CourseResponse course = courseCombo.getValue();

            if (course == null) {
                showError("Please select a course");
                return;
            }

            CreateStudentRequest request = new CreateStudentRequest(
                    prnField.getValue(),
                    firstNameField.getValue(),
                    lastNameField.getValue(),
                    phoneField.getValue(),
                    course.getCourseId(),
                    batchYearField.getValue()
            );

            if (editingId == null) {
                adminService.createStudent(request);
                showSuccess("Student created successfully");
            } else {
                adminService.updateStudent(editingId, request);
                showSuccess("Student updated successfully");
                editingId = null;
                prnField.setReadOnly(false);
                saveButton.setText("Add Student");
            }

            clearForm();
            refreshGrid();
        } catch (ValidationException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Failed to save student: " + ex.getMessage());
        }
    }

    /**
     * Shows a preview dialog with the parsed Excel rows.
     * Valid rows show "Ready" in green, invalid rows show error in red.
     * Admin clicks "Confirm Import" to insert only valid rows, or "Cancel" to abort.
     */
    private void showImportPreviewDialog(List<StudentImportRow> previewRows) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Import Preview");
        dialog.setWidth("900px");
        dialog.setHeight("600px");

        Grid<StudentImportRow> previewGrid = new Grid<>(StudentImportRow.class, false);
        previewGrid.addColumn(StudentImportRow::getRowNumber).setHeader("Row").setWidth("60px");
        previewGrid.addColumn(StudentImportRow::getPrn).setHeader("PRN");
        previewGrid.addColumn(StudentImportRow::getFirstName).setHeader("First Name");
        previewGrid.addColumn(StudentImportRow::getLastName).setHeader("Last Name");
        previewGrid.addColumn(StudentImportRow::getPhone).setHeader("Phone");
        previewGrid.addColumn(StudentImportRow::getCourseCode).setHeader("Course Code");
        previewGrid.addColumn(StudentImportRow::getSemesterNumber).setHeader("Semester");
        previewGrid.addColumn(StudentImportRow::getBatchYear).setHeader("Batch Year");
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

        long validCount = previewRows.stream().filter(StudentImportRow::isValid).count();
        long invalidCount = previewRows.size() - validCount;
        Span summary = new Span(validCount + " valid, " + invalidCount + " invalid out of " + previewRows.size() + " rows");

        Button confirmBtn = new Button("Confirm Import", e -> {
            var result = adminService.confirmStudentImport(previewRows);
            dialog.close();
            showSuccess(result.getSuccessCount() + " students imported successfully. "
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

    private void confirmDeactivate(StudentResponse student) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Deactivate Student");
        dialog.setText("Are you sure you want to deactivate " + student.getFirstName() + " " + student.getLastName() + "?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Deactivate");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            try {
                adminService.deactivateStudent(student.getStudentId());
                showSuccess("Student deactivated");
                refreshGrid();
            } catch (Exception ex) {
                showError("Error deactivating student");
            }
        });
        dialog.open();
    }

    private void clearForm() {
        prnField.clear();
        firstNameField.clear();
        lastNameField.clear();
        phoneField.clear();
        courseCombo.clear();
        batchYearField.setValue(calculateAcademicBatchYear());
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
