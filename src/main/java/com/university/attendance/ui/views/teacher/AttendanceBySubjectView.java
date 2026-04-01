package com.university.attendance.ui.views.teacher;

import com.university.attendance.dto.response.StudentAttendanceRowResponse;
import com.university.attendance.dto.response.TeacherSubjectResponse;
import com.university.attendance.service.TeacherService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;

@Route(value = "teacher/reports/subject", layout = TeacherLayout.class)
@PageTitle("Subject Report")
@RolesAllowed("TEACHER")
public class AttendanceBySubjectView extends VerticalLayout {

    private final TeacherService teacherService;
    private final ComboBox<TeacherSubjectResponse> subjectComboBox = new ComboBox<>("Select Subject");
    private final DatePicker fromDate = new DatePicker("From Date");
    private final DatePicker toDate = new DatePicker("To Date");
    private final Grid<StudentAttendanceRowResponse> grid = new Grid<>(StudentAttendanceRowResponse.class, false);

    public AttendanceBySubjectView(TeacherService teacherService) {
        this.teacherService = teacherService;
        
        setSizeFull();
        setPadding(true);

        H2 header = new H2("Attendance by Subject");
        
        setupFilters();
        setupGrid();

        add(header, createFilterLayout(), grid);
        loadSubjects();
    }

    private void setupFilters() {
        subjectComboBox.setItemLabelGenerator(subject -> 
                String.format("%s (%s) — %s — %s", 
                        subject.getSubjectName(), 
                        subject.getSubjectCode(), 
                        subject.getSemesterLabel(), 
                        subject.getAcademicYear())
        );
        subjectComboBox.setWidth("400px");

        fromDate.setValue(LocalDate.now().minusDays(30));
        toDate.setValue(LocalDate.now());
    }

    private HorizontalLayout createFilterLayout() {
        Button searchBtn = new Button("Search", VaadinIcon.SEARCH.create());
        searchBtn.addClickListener(e -> loadReportData());

        HorizontalLayout layout = new HorizontalLayout(subjectComboBox, fromDate, toDate, searchBtn);
        layout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        return layout;
    }

    private void setupGrid() {
        // Initial static setup for an empty state
        rebuildGridColumns(java.util.Collections.emptyList());
        grid.setSizeFull();
    }

    private void rebuildGridColumns(List<com.university.attendance.dto.response.SessionSummaryResponse> sessions) {
        grid.removeAllColumns();

        grid.addColumn(StudentAttendanceRowResponse::getStudentPrn)
                .setHeader("PRN").setSortable(true).setAutoWidth(true);
                
        grid.addColumn(StudentAttendanceRowResponse::getStudentName)
                .setHeader("Name").setSortable(true).setAutoWidth(true);

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM");

        for (com.university.attendance.dto.response.SessionSummaryResponse session : sessions) {
            String colHeader = session.getSessionDate().format(formatter);
            java.util.UUID sessionId = session.getSessionId();

            grid.addComponentColumn(r -> {
                com.vaadin.flow.component.html.Span badge = new com.vaadin.flow.component.html.Span();
                com.university.attendance.models.AttendanceStatus status = r.getSessionStatuses().get(sessionId);

                if (status == com.university.attendance.models.AttendanceStatus.PRESENT) {
                    badge.setText("P");
                    badge.getElement().getThemeList().add("badge success");
                } else {
                    badge.setText("A");
                    badge.getElement().getThemeList().add("badge error");
                }
                return badge;
            }).setHeader(colHeader).setAutoWidth(true);
        }
                
        grid.addColumn(r -> r.getPresentCount() + " / " + r.getTotalSessions())
                .setHeader("Attended / Total").setAutoWidth(true);
                
        grid.addColumn(r -> String.format("%.2f%%", r.getAttendancePercentage()))
                .setHeader("Percentage").setSortable(true).setAutoWidth(true);

        grid.addComponentColumn(r -> {
            com.vaadin.flow.component.html.Span badge = new com.vaadin.flow.component.html.Span();
            if (r.isAtRisk()) {
                badge.setText("At Risk");
                badge.getElement().getThemeList().add("badge error");
            } else {
                badge.setText("Safe");
                badge.getElement().getThemeList().add("badge success");
            }
            return badge;
        }).setHeader("Status").setAutoWidth(true);
    }

    private void loadSubjects() {
        String prn = SecurityContextHolder.getContext().getAuthentication().getName();
        List<TeacherSubjectResponse> subjects = teacherService.getMySubjects(prn);
        subjectComboBox.setItems(subjects);
        if (!subjects.isEmpty()) {
            subjectComboBox.setValue(subjects.get(0));
        }
    }

    private void loadReportData() {
        if (subjectComboBox.getValue() == null) {
            com.vaadin.flow.component.notification.Notification.show("Please select a subject");
            return;
        }

        String prn = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Fetch sessions for dynamic columns
        List<com.university.attendance.dto.response.SessionSummaryResponse> sessions = teacherService.getSessionsForSubject(
                prn, 
                subjectComboBox.getValue().getSubjectId(), 
                fromDate.getValue(), 
                toDate.getValue()
        );
        
        // Rebuild grid with dynamic columns
        rebuildGridColumns(sessions);

        // Fetch attendance grouped data
        List<StudentAttendanceRowResponse> data = teacherService.getAttendanceBySubject(
                prn, 
                subjectComboBox.getValue().getSubjectId(), 
                fromDate.getValue(), 
                toDate.getValue()
        );
        grid.setItems(data);
    }
}
