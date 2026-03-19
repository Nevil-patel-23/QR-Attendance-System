package com.university.attendance.ui.views.admin;

import com.university.attendance.dto.request.CreateSubjectRequest;
import com.university.attendance.dto.response.CourseResponse;
import com.university.attendance.dto.response.SemesterResponse;
import com.university.attendance.dto.response.SubjectResponse;
import com.university.attendance.exception.ValidationException;
import com.university.attendance.models.SubjectType;
import com.university.attendance.service.AdminService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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

import java.util.List;
import java.util.UUID;

@Route(value = "admin/subjects", layout = AdminLayout.class)
@PageTitle("Subject Management")
@RolesAllowed("ADMIN")
public class SubjectManagementView extends VerticalLayout {

    private final AdminService adminService;

    // Filters & Context
    private final ComboBox<CourseResponse> courseCombo = new ComboBox<>("Select Course");
    private final ComboBox<SemesterResponse> semesterCombo = new ComboBox<>("Select Semester");

    // Form
    private final TextField nameField = new TextField("Subject Name");
    private final TextField codePrefixField = new TextField("Prefix");
    private final TextField codeNumberField = new TextField("Code Number");
    private final ComboBox<SubjectType> typeCombo = new ComboBox<>("Subject Type");
    private final IntegerField creditsField = new IntegerField("Credits");
    private final Button saveButton = new Button("Save");

    private final Grid<SubjectResponse> grid = new Grid<>(SubjectResponse.class, false);

    private UUID editingId = null;

    public SubjectManagementView(AdminService adminService) {
        this.adminService = adminService;

        setSizeFull();

        H2 title = new H2("Subject Management");

        configureSelectors();
        configureForm();
        configureGrid();

        HorizontalLayout selectorsLayout = new HorizontalLayout(courseCombo, semesterCombo);
        selectorsLayout.setAlignItems(Alignment.BASELINE);

        HorizontalLayout formLayout = new HorizontalLayout(nameField, codePrefixField, codeNumberField, typeCombo, creditsField, saveButton);
        formLayout.setAlignItems(Alignment.BASELINE);
        formLayout.setWidthFull();

        add(title, selectorsLayout, formLayout, grid);
    }

    private void configureSelectors() {
        courseCombo.setItems(adminService.getAllCourses());
        courseCombo.setItemLabelGenerator(CourseResponse::getName);
        courseCombo.addValueChangeListener(e -> {
            semesterCombo.clear();
            if (e.getValue() != null) {
                semesterCombo.setItems(adminService.getSemestersByCourse(e.getValue().getCourseId()));
                semesterCombo.setEnabled(true);
            } else {
                semesterCombo.setItems();
                semesterCombo.setEnabled(false);
            }
            refreshGrid();
        });

        semesterCombo.setItemLabelGenerator(SemesterResponse::getLabel);
        semesterCombo.setEnabled(false);
        semesterCombo.addValueChangeListener(e -> {
            refreshGrid();
            if (e.getValue() != null && courseCombo.getValue() != null) {
                generateSubjectCodeSuggestion(courseCombo.getValue(), e.getValue());
            } else {
                codePrefixField.clear();
                codeNumberField.clear();
            }
        });
    }

    private void generateSubjectCodeSuggestion(CourseResponse course, SemesterResponse semester) {
        String courseCode = course.getCode();
        int semNo = semester.getSemesterNumber();
        int durationYears = course.getDurationYears();
        String prefix = courseCode + durationYears + semNo;
        
        List<SubjectResponse> existingSubjects = adminService.getSubjectsBySemester(semester.getSemesterId());
        int count = existingSubjects.size();
        String nextNumber = String.format("%02d", count + 1);
        
        codePrefixField.setValue(prefix);
        codePrefixField.setReadOnly(true);
        codeNumberField.setValue(nextNumber);
    }

    private void configureForm() {
        typeCombo.setItems(SubjectType.values());
        
        creditsField.setMin(1);
        creditsField.setMax(6);
        creditsField.setStepButtonsVisible(true);

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveSubject());
    }

    private void configureGrid() {
        grid.addColumn(SubjectResponse::getName).setHeader("Name").setSortable(true);
        grid.addColumn(SubjectResponse::getCode).setHeader("Code").setSortable(true);
        grid.addColumn(SubjectResponse::getType).setHeader("Type").setSortable(true);
        grid.addColumn(SubjectResponse::getCredits).setHeader("Credits").setSortable(true);

        grid.addComponentColumn(subject -> {
            Button editBtn = new Button("Edit", e -> editSubject(subject));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            Button deleteBtn = new Button("Delete", e -> confirmDelete(subject));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("Actions");
    }

    private void refreshGrid() {
        SemesterResponse selectedSemester = semesterCombo.getValue();
        if (selectedSemester != null) {
            grid.setItems(adminService.getSubjectsBySemester(selectedSemester.getSemesterId()));
        } else {
            grid.setItems(); // Empty grid when no semester is selected
        }
    }

    private void editSubject(SubjectResponse subject) {
        editingId = subject.getSubjectId();
        nameField.setValue(subject.getName());
        
        String prefix = codePrefixField.getValue();
        if (prefix != null && !prefix.isEmpty() && subject.getCode().startsWith(prefix)) {
            codeNumberField.setValue(subject.getCode().substring(prefix.length()));
        } else {
            codePrefixField.setReadOnly(false);
            codePrefixField.clear();
            codeNumberField.setValue(subject.getCode());
        }
        
        typeCombo.setValue(subject.getType());
        creditsField.setValue(subject.getCredits());
        saveButton.setText("Update");
    }

    private void saveSubject() {
        try {
            SemesterResponse selectedSemester = semesterCombo.getValue();
            if (selectedSemester == null) {
                showError("Please select a semester first");
                return;
            }
            if (typeCombo.getValue() == null) {
                showError("Please select a subject type");
                return;
            }

            String fullCode = codePrefixField.getValue() + codeNumberField.getValue();
            CreateSubjectRequest request = new CreateSubjectRequest(
                    nameField.getValue(),
                    fullCode,
                    typeCombo.getValue(),
                    creditsField.getValue(),
                    selectedSemester.getSemesterId()
            );

            if (editingId == null) {
                adminService.createSubject(request);
                showSuccess("Subject created successfully");
            } else {
                adminService.updateSubject(editingId, request);
                showSuccess("Subject updated successfully");
                editingId = null;
                saveButton.setText("Save");
            }

            nameField.clear();
            codeNumberField.clear();
            typeCombo.clear();
            creditsField.clear();
            
            // Re-generate code for next item
            generateSubjectCodeSuggestion(courseCombo.getValue(), selectedSemester);
            
            refreshGrid();
        } catch (ValidationException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Failed to save subject");
        }
    }

    private void confirmDelete(SubjectResponse subject) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Subject");
        dialog.setText("Are you sure you want to delete " + subject.getName() + "?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            try {
                adminService.deleteSubject(subject.getSubjectId());
                showSuccess("Subject deleted successfully");
                refreshGrid();
            } catch (Exception ex) {
                showError("Error deleting subject");
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
