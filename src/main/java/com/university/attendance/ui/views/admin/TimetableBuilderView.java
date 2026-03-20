package com.university.attendance.ui.views.admin;

import com.university.attendance.dto.request.CreateTimetableEntryRequest;
import com.university.attendance.dto.response.*;
import com.university.attendance.exception.ValidationException;
import com.university.attendance.service.AdminService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import com.university.attendance.models.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "admin/timetable", layout = AdminLayout.class)
@PageTitle("Timetable Builder")
@RolesAllowed("ADMIN")
public class TimetableBuilderView extends VerticalLayout {

    private final AdminService adminService;
    private final Grid<TimetableEntryResponse> grid = new Grid<>(TimetableEntryResponse.class, false);

    // Filters for viewing timetable
    private final ComboBox<CourseResponse> filterCourseCombo = new ComboBox<>("Course");
    private final ComboBox<SemesterResponse> filterSemesterCombo = new ComboBox<>("Semester");
    private final TextField filterAcademicYearField = new TextField("Academic Year (e.g. 202425)");
    private final Button loadTimetableBtn = new Button("Load Timetable");

    // Form for adding new slot
    private final ComboBox<CourseResponse> formCourseCombo = new ComboBox<>("Course");
    private final ComboBox<SemesterResponse> formSemesterCombo = new ComboBox<>("Semester");
    private final TextField formAcademicYearField = new TextField("Academic Year (e.g. 202425)");
    private final ComboBox<SubjectResponse> formSubjectCombo = new ComboBox<>("Subject");
    private final ComboBox<TeacherResponse> formTeacherCombo = new ComboBox<>("Teacher");
    private final ComboBox<DayOfWeek> dayOfWeekCombo = new ComboBox<>("Day of Week");
    private final TimePicker startTimePicker = new TimePicker("Start Time");
    private final TimePicker endTimePicker = new TimePicker("End Time");
    private final TextField roomField = new TextField("Room");
    private final Button addSlotBtn = new Button("Add Timetable Slot");

    public TimetableBuilderView(AdminService adminService) {
        this.adminService = adminService;
        setSizeFull();

        H2 title = new H2("Timetable Builder");

        VerticalLayout filterSection = createFilterSection();
        VerticalLayout formSection = createFormSection();
        HorizontalLayout uploadSection = createUploadSection();

        configureGrid();

        add(title, filterSection, formSection, uploadSection, grid);
    }

    private VerticalLayout createFilterSection() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        H3 sectionTitle = new H3("View Timetable");
        sectionTitle.getStyle().set("margin-top", "0");

        HorizontalLayout filters = new HorizontalLayout(filterCourseCombo, filterSemesterCombo, filterAcademicYearField, loadTimetableBtn);
        filters.setAlignItems(Alignment.BASELINE);

        List<CourseResponse> courses = adminService.getAllCourses();
        filterCourseCombo.setItems(courses);
        filterCourseCombo.setItemLabelGenerator(c -> c.getName() + " (" + c.getCode() + ")");
        filterCourseCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                filterSemesterCombo.setItems(adminService.getSemestersByCourse(e.getValue().getCourseId()));
            } else {
                filterSemesterCombo.clear();
                filterSemesterCombo.setItems();
            }
        });

        filterSemesterCombo.setItemLabelGenerator(s -> "Semester " + s.getSemesterNumber());

        loadTimetableBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loadTimetableBtn.addClickListener(e -> loadTimetable());

        layout.add(sectionTitle, filters);
        return layout;
    }

    private VerticalLayout createFormSection() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        H3 sectionTitle = new H3("Add New Slot & Allocation");

        HorizontalLayout row1 = new HorizontalLayout(formCourseCombo, formSemesterCombo, formAcademicYearField, formSubjectCombo);
        row1.setWidthFull();
        HorizontalLayout row2 = new HorizontalLayout(formTeacherCombo, dayOfWeekCombo, startTimePicker, endTimePicker, roomField);
        row2.setWidthFull();

        List<CourseResponse> courses = adminService.getAllCourses();
        formCourseCombo.setItems(courses);
        formCourseCombo.setItemLabelGenerator(c -> c.getName() + " (" + c.getCode() + ")");
        formCourseCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                formSemesterCombo.setItems(adminService.getSemestersByCourse(e.getValue().getCourseId()));
            } else {
                formSemesterCombo.clear();
                formSemesterCombo.setItems();
                formSubjectCombo.clear();
                formSubjectCombo.setItems();
            }
        });

        formSemesterCombo.setItemLabelGenerator(s -> "Semester " + s.getSemesterNumber());
        formSemesterCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                formSubjectCombo.setItems(adminService.getSubjectsBySemester(e.getValue().getSemesterId()));
            } else {
                formSubjectCombo.clear();
                formSubjectCombo.setItems();
            }
        });

        formSubjectCombo.setItemLabelGenerator(s -> s.getName() + " (" + s.getCode() + ")");

        List<TeacherResponse> teachers = adminService.getAllTeachers();
        formTeacherCombo.setItems(teachers);
        formTeacherCombo.setItemLabelGenerator(t -> t.getFirstName() + " " + t.getLastName() + " (" + t.getPrn() + ")");

        dayOfWeekCombo.setItems(DayOfWeek.values());

        startTimePicker.setMin(LocalTime.of(8, 0));
        startTimePicker.setMax(LocalTime.of(17, 0));
        startTimePicker.setStep(Duration.ofMinutes(30));

        endTimePicker.setMin(LocalTime.of(9, 0));
        endTimePicker.setMax(LocalTime.of(18, 0));
        endTimePicker.setStep(Duration.ofMinutes(30));

        startTimePicker.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                endTimePicker.setMin(e.getValue());
            }
        });

        addSlotBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addSlotBtn.addClickListener(e -> addTimetableSlot());

        layout.add(sectionTitle, row1, row2, addSlotBtn);
        return layout;
    }

    private HorizontalLayout createUploadSection() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(Alignment.CENTER);

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".xlsx", ".xls");
        upload.setUploadButton(new Button("Import from Excel"));
        upload.addSucceededListener(event -> {
            try {
                List<TimetableImportRow> previewRows = adminService.previewTimetableImport(buffer.getInputStream());
                showImportPreviewDialog(previewRows);
            } catch (Exception ex) {
                showError("Excel import failed: " + ex.getMessage());
            }
        });

        layout.add(upload);
        return layout;
    }

    private void configureGrid() {
        grid.addColumn(TimetableEntryResponse::getTeacherName).setHeader("Teacher");
        grid.addColumn(TimetableEntryResponse::getSubjectName).setHeader("Subject");
        grid.addColumn(TimetableEntryResponse::getDayOfWeek).setHeader("Day");
        grid.addColumn(slot -> slot.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                slot.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"))).setHeader("Time");
        grid.addColumn(TimetableEntryResponse::getRoom).setHeader("Room");
        grid.addColumn(slot -> slot.getEffectiveFrom().toString()).setHeader("Effective From");

        grid.addComponentColumn(slot -> {
            Button deleteBtn = new Button("Delete", e -> confirmDelete(slot));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            return deleteBtn;
        }).setHeader("Actions");
    }

    private void loadTimetable() {
        SemesterResponse semester = filterSemesterCombo.getValue();
        String academicYear = filterAcademicYearField.getValue();

        if (semester == null || academicYear == null || academicYear.isBlank()) {
            showError("Please select Semester and provide Academic Year to load timetable.");
            return;
        }

        try {
            List<TimetableEntryResponse> entries = adminService.getTimetableBySemester(semester.getSemesterId(), academicYear);
            grid.setItems(entries);
            if (entries.isEmpty()) {
                showSuccess("No timetable entries found for selected criteria.");
            }
        } catch (Exception ex) {
            showError("Failed to load timetable: " + ex.getMessage());
        }
    }

    private void addTimetableSlot() {
        try {
            if (formCourseCombo.getValue() == null || formSemesterCombo.getValue() == null ||
                    formAcademicYearField.getValue().isBlank() || formTeacherCombo.getValue() == null ||
                    formSubjectCombo.getValue() == null || dayOfWeekCombo.getValue() == null ||
                    startTimePicker.getValue() == null || endTimePicker.getValue() == null ||
                    roomField.getValue().isBlank()) {
                showError("Please fill out all the fields");
                return;
            }

            CreateTimetableEntryRequest request = new CreateTimetableEntryRequest(
                    formCourseCombo.getValue().getCourseId(),
                    formSemesterCombo.getValue().getSemesterId(),
                    formAcademicYearField.getValue(),
                    formTeacherCombo.getValue().getTeacherId(),
                    formSubjectCombo.getValue().getSubjectId(),
                    dayOfWeekCombo.getValue(),
                    startTimePicker.getValue(),
                    endTimePicker.getValue(),
                    roomField.getValue()
            );

            adminService.createTimetableEntry(request);
            showSuccess("Timetable slot created successfully");
            
            // Optionally, automatically load grid if filters match the newly added slot
            if (filterSemesterCombo.getValue() != null && 
                filterSemesterCombo.getValue().getSemesterId().equals(formSemesterCombo.getValue().getSemesterId()) &&
                filterAcademicYearField.getValue().equals(formAcademicYearField.getValue())) {
                loadTimetable();
            }

            // Keep course/semester/year/teacher selected for ease of bulk entry
            dayOfWeekCombo.clear();
            startTimePicker.clear();
            endTimePicker.clear();
            roomField.clear();

        } catch (ValidationException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Failed to save timetable slot: " + ex.getMessage());
        }
    }

    private void confirmDelete(TimetableEntryResponse slot) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Timetable Slot");
        dialog.setText("Are you sure you want to delete this slot for " + slot.getSubjectName() + "?\n" +
                "If this is the last slot for this teacher-subject allocation, the allocation will also be removed.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            try {
                adminService.deleteTimetableEntry(slot.getSlotId());
                showSuccess("Slot deleted successfully");
                loadTimetable();
            } catch (Exception ex) {
                showError("Error deleting slot: " + ex.getMessage());
            }
        });
        dialog.open();
    }

    private void showImportPreviewDialog(List<TimetableImportRow> previewRows) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Timetable Import Preview");
        dialog.setWidth("900px");
        dialog.setHeight("600px");

        Grid<TimetableImportRow> previewGrid = new Grid<>(TimetableImportRow.class, false);
        previewGrid.addColumn(TimetableImportRow::getRowNumber).setHeader("Row").setWidth("60px");
        previewGrid.addColumn(TimetableImportRow::getTeacherPrn).setHeader("Teacher PRN");
        previewGrid.addColumn(TimetableImportRow::getSubjectCode).setHeader("Subject");
        previewGrid.addColumn(TimetableImportRow::getCourseCode).setHeader("Course Code");
        previewGrid.addColumn(TimetableImportRow::getSemesterNumber).setHeader("Semester");
        previewGrid.addColumn(TimetableImportRow::getDay).setHeader("Day");
        previewGrid.addColumn(TimetableImportRow::getStartTime).setHeader("Start");
        previewGrid.addColumn(TimetableImportRow::getEndTime).setHeader("End");
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
        }).setHeader("Status").setWidth("250px");

        previewGrid.setItems(previewRows);
        previewGrid.setSizeFull();

        long validCount = previewRows.stream().filter(TimetableImportRow::isValid).count();
        long invalidCount = previewRows.size() - validCount;
        Span summary = new Span(validCount + " valid, " + invalidCount + " invalid out of " + previewRows.size() + " rows");

        Button confirmBtn = new Button("Confirm Import", e -> {
            var result = adminService.confirmTimetableImport(previewRows);
            dialog.close();
            showSuccess(result.getSuccessCount() + " slots imported successfully. "
                    + result.getFailedCount() + " rows skipped.");
            if (filterSemesterCombo.getValue() != null && !filterAcademicYearField.getValue().isBlank()) {
                loadTimetable();
            }
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
