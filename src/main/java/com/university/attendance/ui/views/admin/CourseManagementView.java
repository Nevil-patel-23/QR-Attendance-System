package com.university.attendance.ui.views.admin;

import com.university.attendance.dto.request.CreateCourseRequest;
import com.university.attendance.dto.response.CourseResponse;
import com.university.attendance.dto.response.FacultyResponse;
import com.university.attendance.dto.response.SemesterResponse;
import com.university.attendance.exception.ValidationException;
import com.university.attendance.service.AdminService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.UUID;

@Route(value = "admin/courses", layout = AdminLayout.class)
@PageTitle("Course Management")
@RolesAllowed("ADMIN")
public class CourseManagementView extends VerticalLayout {

    private final AdminService adminService;

    // Filters & Form
    private final ComboBox<FacultyResponse> filterFacultyCombo = new ComboBox<>("Filter by Faculty");
    private final ComboBox<FacultyResponse> formFacultyCombo = new ComboBox<>("Faculty");
    private final TextField nameField = new TextField("Course Name");
    private final TextField codeField = new TextField("Course Code");
    private final IntegerField durationField = new IntegerField("Duration (Years)");
    private final Button saveButton = new Button("Save");

    private final Grid<CourseResponse> grid = new Grid<>(CourseResponse.class, false);

    private UUID editingId = null;

    public CourseManagementView(AdminService adminService) {
        this.adminService = adminService;

        setSizeFull();

        H2 title = new H2("Course Management");

        configureFilters();
        configureForm();
        configureGrid();

        HorizontalLayout formLayout = new HorizontalLayout(formFacultyCombo, nameField, codeField, durationField, saveButton);
        formLayout.setAlignItems(Alignment.BASELINE);
        formLayout.setWidthFull();

        add(title, filterFacultyCombo, formLayout, grid);
        refreshGrid();
    }

    private void configureFilters() {
        List<FacultyResponse> faculties = adminService.getAllFaculties();
        filterFacultyCombo.setItems(faculties);
        filterFacultyCombo.setItemLabelGenerator(FacultyResponse::getName);
        filterFacultyCombo.setClearButtonVisible(true);
        filterFacultyCombo.addValueChangeListener(e -> refreshGrid());

        formFacultyCombo.setItems(faculties);
        formFacultyCombo.setItemLabelGenerator(FacultyResponse::getName);
    }

    private void configureForm() {
        durationField.setMin(1);
        durationField.setMax(6);
        durationField.setStepButtonsVisible(true);

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveCourse());
    }

    private void configureGrid() {
        grid.addColumn(CourseResponse::getName).setHeader("Name").setSortable(true);
        grid.addColumn(CourseResponse::getCode).setHeader("Code").setSortable(true);
        grid.addColumn(CourseResponse::getDurationYears).setHeader("Duration (Years)").setSortable(true);
        grid.addColumn(CourseResponse::getFacultyName).setHeader("Faculty").setSortable(true);

        grid.addComponentColumn(course -> {
            Button editBtn = new Button("Edit", e -> editCourse(course));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            Button deleteBtn = new Button("Delete", e -> confirmDelete(course));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("Actions");

        // Details Panel for Semesters
        grid.setItemDetailsRenderer(new ComponentRenderer<>(course -> {
            VerticalLayout layout = new VerticalLayout();
            layout.setPadding(true);
            layout.getStyle().set("background-color", "var(--lumo-contrast-5pct)");

            H4 semestersTitle = new H4("Semesters for " + course.getName());
            Grid<SemesterResponse> semesterGrid = new Grid<>(SemesterResponse.class, false);
            semesterGrid.addColumn(SemesterResponse::getSemesterNumber).setHeader("Semester #");
            semesterGrid.addColumn(SemesterResponse::getLabel).setHeader("Label");
            
            List<SemesterResponse> semesters = adminService.getSemestersByCourse(course.getCourseId());
            semesterGrid.setItems(semesters);
            semesterGrid.setAllRowsVisible(true); // show all comfortably

            layout.add(semestersTitle, semesterGrid);
            return layout;
        }));
    }

    private void refreshGrid() {
        FacultyResponse selectedFaculty = filterFacultyCombo.getValue();
        if (selectedFaculty != null) {
            grid.setItems(adminService.getCoursesByFaculty(selectedFaculty.getFacultyId()));
        } else {
            grid.setItems(adminService.getAllCourses());
        }
    }

    private void editCourse(CourseResponse course) {
        editingId = course.getCourseId();
        nameField.setValue(course.getName());
        codeField.setValue(course.getCode());
        durationField.setValue(course.getDurationYears());

        adminService.getAllFaculties().stream()
                .filter(f -> f.getName().equals(course.getFacultyName()))
                .findFirst()
                .ifPresent(formFacultyCombo::setValue);

        saveButton.setText("Update");
    }

    private void saveCourse() {
        try {
            FacultyResponse selectedFaculty = formFacultyCombo.getValue();
            if (selectedFaculty == null) {
                showError("Please select a faculty");
                return;
            }

            CreateCourseRequest request = new CreateCourseRequest(
                    nameField.getValue(),
                    codeField.getValue(),
                    durationField.getValue(),
                    selectedFaculty.getFacultyId()
            );

            if (editingId == null) {
                adminService.createCourse(request);
                showSuccess("Course created successfully with " + (request.getDurationYears() * 2) + " semesters");
            } else {
                adminService.updateCourse(editingId, request);
                showSuccess("Course updated successfully");
                editingId = null;
                saveButton.setText("Save");
            }

            nameField.clear();
            codeField.clear();
            durationField.clear();
            formFacultyCombo.clear();
            refreshGrid();
        } catch (ValidationException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Failed to save course");
        }
    }

    private void confirmDelete(CourseResponse course) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Course");
        dialog.setText("Are you sure you want to delete " + course.getName() + "?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            try {
                adminService.deleteCourse(course.getCourseId());
                showSuccess("Course deleted successfully");
                refreshGrid();
            } catch (ValidationException ex) {
                showError(ex.getMessage());
            } catch (Exception ex) {
                showError("Error deleting course");
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
