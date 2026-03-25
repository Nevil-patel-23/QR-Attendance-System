package com.university.attendance.ui.views.admin;

import com.university.attendance.dto.request.CreateEnrollmentRequest;
import com.university.attendance.dto.response.CourseResponse;
import com.university.attendance.dto.response.EnrollmentResponse;
import com.university.attendance.dto.response.SemesterResponse;
import com.university.attendance.dto.response.StudentResponse;
import com.university.attendance.dto.response.SubjectResponse;
import com.university.attendance.service.AdminService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Route(value = "admin/elective-enrollment", layout = AdminLayout.class)
@PageTitle("Elective Enrollment")
@RolesAllowed("ADMIN")
public class ElectiveEnrollmentView extends VerticalLayout {

    private final AdminService adminService;

    private ComboBox<CourseResponse> courseCombo;
    private ComboBox<SemesterResponse> semesterCombo;
    private IntegerField batchYearFilter;
    private Grid<SubjectResponse> subjectGrid;
    private Grid<EnrollmentResponse> enrollmentGrid;
    private Span enrollmentCountLabel;

    private SubjectResponse selectedSubject;
    private final String currentAcademicYear;

    public ElectiveEnrollmentView(AdminService adminService) {
        this.adminService = adminService;
        this.currentAcademicYear = computeCurrentAcademicYear();

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Elective Enrollment Management");

        // Cascading dropdowns: Course → Semester + Batch Year filter
        courseCombo = new ComboBox<>("Select Course");
        courseCombo.setItemLabelGenerator(c -> c.getName() + " (" + c.getCode() + ")");
        courseCombo.setWidth("40%");

        semesterCombo = new ComboBox<>("Select Semester");
        semesterCombo.setItemLabelGenerator(SemesterResponse::getLabel);
        semesterCombo.setWidth("30%");
        semesterCombo.setEnabled(false);

        batchYearFilter = new IntegerField("Batch Year");
        batchYearFilter.setValue(computeCurrentBatchYear());
        batchYearFilter.setStepButtonsVisible(true);
        batchYearFilter.setStep(101);
        batchYearFilter.setMin(200001);
        batchYearFilter.setMax(209999);
        batchYearFilter.setWidth("15%");

        // When course changes → load that course's semesters
        courseCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                List<SemesterResponse> semesters = adminService.getSemestersByCourse(e.getValue().getCourseId());
                semesterCombo.setItems(semesters);
                semesterCombo.setEnabled(true);
                semesterCombo.clear();
            } else {
                semesterCombo.setItems();
                semesterCombo.setEnabled(false);
                semesterCombo.clear();
            }
            subjectGrid.setItems();
            enrollmentGrid.setItems();
            selectedSubject = null;
            enrollmentCountLabel.setText("Select a subject to view enrollments");
        });

        // When semester changes → load elective subjects
        semesterCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                loadElectiveSubjects(e.getValue().getSemesterId());
            } else {
                subjectGrid.setItems();
                enrollmentGrid.setItems();
                selectedSubject = null;
                enrollmentCountLabel.setText("Select a subject to view enrollments");
            }
        });

        // Load all courses
        courseCombo.setItems(adminService.getAllCourses());

        HorizontalLayout filterBar = new HorizontalLayout(courseCombo, semesterCombo, batchYearFilter);
        filterBar.setWidthFull();
        filterBar.setAlignItems(FlexComponent.Alignment.BASELINE);

        // Two-panel layout
        HorizontalLayout panels = new HorizontalLayout();
        panels.setSizeFull();
        panels.setSpacing(true);

        VerticalLayout leftPanel = createSubjectPanel();
        leftPanel.setWidth("40%");

        VerticalLayout rightPanel = createEnrollmentPanel();
        rightPanel.setWidth("60%");

        panels.add(leftPanel, rightPanel);

        add(title, filterBar, panels);
    }

    private VerticalLayout createSubjectPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.getStyle().set("box-shadow", "var(--lumo-box-shadow-s)");
        panel.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        panel.getStyle().set("background-color", "var(--lumo-base-color)");
        panel.setPadding(true);
        panel.setSpacing(true);

        H3 heading = new H3("Elective Subjects");

        subjectGrid = new Grid<>(SubjectResponse.class, false);
        subjectGrid.addColumn(SubjectResponse::getCode).setHeader("Code").setWidth("100px");
        subjectGrid.addColumn(SubjectResponse::getName).setHeader("Subject");
        subjectGrid.addColumn(sub -> {
            long count = adminService.getEnrollmentCount(sub.getSubjectId(), currentAcademicYear);
            return String.valueOf(count);
        }).setHeader("Enrolled").setWidth("80px");

        subjectGrid.asSingleSelect().addValueChangeListener(e -> {
            selectedSubject = e.getValue();
            if (selectedSubject != null) {
                loadEnrollments();
            } else {
                enrollmentGrid.setItems();
                enrollmentCountLabel.setText("Select a subject to view enrollments");
            }
        });

        panel.add(heading, subjectGrid);
        return panel;
    }

    private VerticalLayout createEnrollmentPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.getStyle().set("box-shadow", "var(--lumo-box-shadow-s)");
        panel.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        panel.getStyle().set("background-color", "var(--lumo-base-color)");
        panel.setPadding(true);
        panel.setSpacing(true);

        HorizontalLayout topBar = new HorizontalLayout();
        topBar.setWidthFull();
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);

        H3 heading = new H3("Enrolled Students");
        enrollmentCountLabel = new Span("Select a subject to view enrollments");
        enrollmentCountLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Button enrollBtn = new Button("+ Enroll Student", e -> openEnrollDialog());
        enrollBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        topBar.add(heading, enrollmentCountLabel);
        topBar.expand(enrollmentCountLabel);
        topBar.add(enrollBtn);

        enrollmentGrid = new Grid<>(EnrollmentResponse.class, false);
        enrollmentGrid.addColumn(EnrollmentResponse::getStudentPrn).setHeader("PRN").setWidth("120px");
        enrollmentGrid.addColumn(EnrollmentResponse::getStudentName).setHeader("Student Name");
        enrollmentGrid.addColumn(EnrollmentResponse::getAcademicYear).setHeader("Academic Year").setWidth("120px");
        enrollmentGrid.addComponentColumn(enrollment -> {
            Button removeBtn = new Button("Remove", ev -> {
                try {
                    adminService.deleteEnrollment(enrollment.getEnrollmentId());
                    Notification.show("Enrollment removed", 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    loadEnrollments();
                    refreshSubjectGrid();
                } catch (Exception ex) {
                    Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            return removeBtn;
        }).setHeader("Action").setWidth("100px");

        panel.add(topBar, enrollmentGrid);
        return panel;
    }

    private void loadElectiveSubjects(UUID semesterId) {
        List<SubjectResponse> electives = adminService.getElectiveSubjectsBySemester(semesterId);
        subjectGrid.setItems(electives);
        selectedSubject = null;
        enrollmentGrid.setItems();
        enrollmentCountLabel.setText(electives.isEmpty()
                ? "No elective subjects found for this semester"
                : "Select a subject to view enrollments");
    }

    private void loadEnrollments() {
        if (selectedSubject == null) return;
        List<EnrollmentResponse> enrollments = adminService.getEnrollmentsBySubject(
                selectedSubject.getSubjectId(), currentAcademicYear);
        enrollmentGrid.setItems(enrollments);
        enrollmentCountLabel.setText(enrollments.size() + " student(s) enrolled in " + selectedSubject.getName());
    }

    private void refreshSubjectGrid() {
        if (semesterCombo.getValue() != null) {
            loadElectiveSubjects(semesterCombo.getValue().getSemesterId());
        }
    }

    private void openEnrollDialog() {
        if (selectedSubject == null) {
            Notification.show("Please select an elective subject first", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            return;
        }

        if (batchYearFilter.getValue() == null) {
            Notification.show("Please specify a Batch Year", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Enroll Student in " + selectedSubject.getName());
        dialog.setWidth("500px");

        // Fetch students filtered by semester AND batch year
        UUID semesterId = selectedSubject.getSemesterId();
        Integer batchYear = batchYearFilter.getValue();
        List<StudentResponse> studentsInSemester = adminService.getStudentsBySemesterAndBatchYear(semesterId, batchYear);

        ComboBox<StudentResponse> studentCombo = new ComboBox<>("Select Student");
        studentCombo.setItems(studentsInSemester);
        studentCombo.setItemLabelGenerator(s -> s.getPrn() + " — " + s.getFirstName() + " " + s.getLastName());
        studentCombo.setWidthFull();

        if (studentsInSemester.isEmpty()) {
            studentCombo.setPlaceholder("No students found for batch " + batchYear);
        }

        Button confirmBtn = new Button("Enroll", e -> {
            StudentResponse selected = studentCombo.getValue();
            if (selected == null) {
                Notification.show("Please select a student", 3000, Notification.Position.TOP_CENTER);
                return;
            }

            try {
                CreateEnrollmentRequest request = new CreateEnrollmentRequest();
                request.setStudentId(selected.getStudentId());
                request.setSubjectId(selectedSubject.getSubjectId());
                request.setAcademicYear(currentAcademicYear);

                adminService.createEnrollment(request);
                Notification.show("Student enrolled successfully", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                loadEnrollments();
                refreshSubjectGrid();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        VerticalLayout dialogContent = new VerticalLayout(studentCombo);
        dialogContent.setPadding(false);
        dialogContent.setSpacing(true);
        dialog.add(dialogContent);
        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    private String computeCurrentAcademicYear() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        if (now.getMonthValue() >= 7) {
            return String.valueOf(year) + String.valueOf(year + 1).substring(2);
        } else {
            return String.valueOf(year - 1) + String.valueOf(year).substring(2);
        }
    }

    /**
     * Compute current batch year in 6-digit INT format (e.g. 202526).
     * June-May cycle: month >= 6 → year*100 + (year+1)%100,
     * month < 6 → (year-1)*100 + year%100.
     */
    private int computeCurrentBatchYear() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        if (now.getMonthValue() >= 6) {
            return year * 100 + (year + 1) % 100;
        } else {
            return (year - 1) * 100 + year % 100;
        }
    }
}
