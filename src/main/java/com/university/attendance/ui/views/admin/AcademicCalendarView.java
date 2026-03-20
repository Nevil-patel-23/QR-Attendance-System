package com.university.attendance.ui.views.admin;

import com.university.attendance.dto.request.CreateAcademicCalendarRequest;
import com.university.attendance.dto.response.AcademicCalendarResponse;
import com.university.attendance.dto.response.CourseResponse;
import com.university.attendance.exception.ValidationException;
import com.university.attendance.service.AdminService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import com.university.attendance.dto.response.AcademicCalendarImportRow;
import com.university.attendance.dto.response.ExcelImportResponse;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Route(value = "admin/calendar", layout = AdminLayout.class)
@PageTitle("Academic Calendar")
@RolesAllowed("ADMIN")
public class AcademicCalendarView extends VerticalLayout {

    private final AdminService adminService;
    private final Grid<AcademicCalendarResponse> grid = new Grid<>(AcademicCalendarResponse.class, false);

    private final ComboBox<CourseResponse> courseCombo = new ComboBox<>("Course");
    private final TextField academicYearField = new TextField("Academic Year");
    private final IntegerField semesterNumberField = new IntegerField("Semester Number");
    private final DatePicker startDatePicker = new DatePicker("Start Date");
    private final DatePicker endDatePicker = new DatePicker("End Date");
    private final Button saveButton = new Button("Add Calendar Entry");

    private UUID editingId = null;

    public AcademicCalendarView(AdminService adminService) {
        this.adminService = adminService;

        setSizeFull();

        H2 title = new H2("Academic Calendar Management");

        configureGrid();
        configureForm();

        HorizontalLayout formRow1 = new HorizontalLayout(courseCombo, academicYearField, semesterNumberField);
        formRow1.setAlignItems(Alignment.BASELINE);
        formRow1.setWidthFull();

        HorizontalLayout formRow2 = new HorizontalLayout(startDatePicker, endDatePicker, saveButton);
        formRow2.setAlignItems(Alignment.BASELINE);
        formRow2.setWidthFull();

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".xlsx", ".xls");
        upload.setUploadButton(new Button("Import from Excel"));
        upload.addSucceededListener(e -> handleExcelUpload(buffer.getInputStream()));

        add(title, formRow1, formRow2, upload, grid);
        refreshGrid();
    }

    private void handleExcelUpload(InputStream inputStream) {
        try {
            List<AcademicCalendarImportRow> previewRows = adminService.previewCalendarImport(inputStream);
            showImportPreviewDialog(previewRows);
        } catch (Exception ex) {
            showError("Failed to read Excel file: " + ex.getMessage());
        }
    }

    private void showImportPreviewDialog(List<AcademicCalendarImportRow> previewRows) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Import Preview");
        dialog.setWidth("80vw");
        dialog.setHeight("80vh");

        Grid<AcademicCalendarImportRow> previewGrid = new Grid<>(AcademicCalendarImportRow.class, false);
        previewGrid.addColumn(AcademicCalendarImportRow::getRowNumber).setHeader("Row").setAutoWidth(true).setFlexGrow(0);
        previewGrid.addColumn(AcademicCalendarImportRow::getCourseCode).setHeader("Course Code");
        previewGrid.addColumn(AcademicCalendarImportRow::getAcademicYear).setHeader("Academic Year");
        previewGrid.addColumn(AcademicCalendarImportRow::getSemesterNumber).setHeader("Semester");
        previewGrid.addColumn(AcademicCalendarImportRow::getStartDate).setHeader("Start Date");
        previewGrid.addColumn(AcademicCalendarImportRow::getEndDate).setHeader("End Date");

        previewGrid.addComponentColumn(row -> {
            com.vaadin.flow.component.html.Span status = new com.vaadin.flow.component.html.Span(row.getErrorMessage());
            if (row.isValid()) {
                status.getStyle().set("color", "green");
            } else {
                status.getStyle().set("color", "red");
            }
            return status;
        }).setHeader("Status").setAutoWidth(true).setFlexGrow(1);

        previewGrid.setItems(previewRows);
        previewGrid.setSizeFull();

        long validCount = previewRows.stream().filter(AcademicCalendarImportRow::isValid).count();
        long invalidCount = previewRows.size() - validCount;

        com.vaadin.flow.component.html.Span summary = new com.vaadin.flow.component.html.Span(
                String.format("Found %d valid rows and %d invalid rows.", validCount, invalidCount)
        );

        Button confirmBtn = new Button("Confirm Import", e -> {
            dialog.close();
            executeImport(previewRows);
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirmBtn.setEnabled(validCount > 0);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        HorizontalLayout actions = new HorizontalLayout(summary, cancelBtn, confirmBtn);
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.BETWEEN);
        actions.setAlignItems(Alignment.CENTER);

        VerticalLayout layout = new VerticalLayout(previewGrid, actions);
        layout.setSizeFull();
        layout.setPadding(false);

        dialog.add(layout);
        dialog.open();
    }

    private void executeImport(List<AcademicCalendarImportRow> previewRows) {
        try {
            ExcelImportResponse response = adminService.confirmCalendarImport(previewRows);
            refreshGrid();
            if (response.getErrors().isEmpty()) {
                showSuccess(String.format("%d calendars imported, 0 skipped", response.getSuccessCount()));
            } else {
                showError(String.format("%d calendars imported, %d skipped",
                        response.getSuccessCount(), response.getFailedCount()));
                response.getErrors().forEach(this::showError);
            }
        } catch (Exception ex) {
            showError("Import failed: " + ex.getMessage());
        }
    }

    private void configureGrid() {
        grid.addColumn(AcademicCalendarResponse::getCourseName).setHeader("Course").setSortable(true);
        grid.addColumn(AcademicCalendarResponse::getCourseCode).setHeader("Code").setSortable(true);
        grid.addColumn(AcademicCalendarResponse::getAcademicYear).setHeader("Academic Year").setSortable(true);
        grid.addColumn(c -> c.getSemesterNumber() == 1 ? "Odd Semester" : "Even Semester").setHeader("Semester").setSortable(true);
        grid.addColumn(AcademicCalendarResponse::getStartDate).setHeader("Start Date").setSortable(true);
        grid.addColumn(AcademicCalendarResponse::getEndDate).setHeader("End Date").setSortable(true);

        grid.addComponentColumn(cal -> {
            Button editBtn = new Button("Edit", e -> editCalendar(cal));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            Button deleteBtn = new Button("Delete", e -> confirmDelete(cal));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("Actions");

        grid.setSizeFull();
    }

    private void configureForm() {
        courseCombo.setItemLabelGenerator(c -> c.getCode() + " — " + c.getName());
        courseCombo.setItems(adminService.getAllCourses());
        courseCombo.setWidth("250px");

        academicYearField.setPlaceholder("e.g. 202627");
        academicYearField.setWidth("150px");

        semesterNumberField.setMin(1);
        semesterNumberField.setMax(2);
        semesterNumberField.setStepButtonsVisible(true);
        semesterNumberField.setWidth("150px");

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveCalendar());
    }

    private void refreshGrid() {
        grid.setItems(adminService.getAllCalendars());
    }

    private void editCalendar(AcademicCalendarResponse cal) {
        editingId = cal.getCalendarId();

        // Match course by code
        courseCombo.getListDataView().getItems()
                .filter(c -> c.getCode().equals(cal.getCourseCode()))
                .findFirst()
                .ifPresent(courseCombo::setValue);

        academicYearField.setValue(cal.getAcademicYear());
        semesterNumberField.setValue(cal.getSemesterNumber());
        startDatePicker.setValue(cal.getStartDate());
        endDatePicker.setValue(cal.getEndDate());
        saveButton.setText("Update");
    }

    private void saveCalendar() {
        try {
            CourseResponse selectedCourse = courseCombo.getValue();
            if (selectedCourse == null) {
                showError("Please select a course");
                return;
            }

            CreateAcademicCalendarRequest request = new CreateAcademicCalendarRequest();
            request.setCourseId(selectedCourse.getCourseId());
            request.setAcademicYear(academicYearField.getValue());
            request.setSemesterNumber(semesterNumberField.getValue());
            request.setStartDate(startDatePicker.getValue());
            request.setEndDate(endDatePicker.getValue());

            if (editingId == null) {
                adminService.createCalendar(request);
                showSuccess("Calendar entry created successfully");
            } else {
                adminService.updateCalendar(editingId, request);
                showSuccess("Calendar entry updated successfully");
                editingId = null;
                saveButton.setText("Add Calendar Entry");
            }

            clearForm();
            refreshGrid();
        } catch (ValidationException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Failed to save calendar entry: " + ex.getMessage());
        }
    }

    private void confirmDelete(AcademicCalendarResponse cal) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Calendar Entry");
        String semLabel = cal.getSemesterNumber() == 1 ? "Odd Semester" : "Even Semester";
        dialog.setText("Are you sure you want to delete the calendar entry for "
                + cal.getCourseName() + " — " + semLabel + " (" + cal.getAcademicYear() + ")?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            try {
                adminService.deleteCalendar(cal.getCalendarId());
                showSuccess("Calendar entry deleted");
                refreshGrid();
            } catch (ValidationException ex) {
                showError(ex.getMessage());
            } catch (Exception ex) {
                showError("Error deleting calendar entry");
            }
        });
        dialog.open();
    }

    private void clearForm() {
        courseCombo.clear();
        academicYearField.clear();
        semesterNumberField.clear();
        startDatePicker.clear();
        endDatePicker.clear();
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
