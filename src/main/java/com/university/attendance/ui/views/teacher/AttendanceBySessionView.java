package com.university.attendance.ui.views.teacher;

import com.university.attendance.dto.response.SessionAttendanceRowResponse;
import com.university.attendance.dto.response.SessionSummaryResponse;
import com.university.attendance.dto.response.TeacherSubjectResponse;
import com.university.attendance.models.AttendanceStatus;
import com.university.attendance.service.TeacherService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "teacher/reports/session", layout = TeacherLayout.class)
@PageTitle("Session Report")
@RolesAllowed("TEACHER")
public class AttendanceBySessionView extends VerticalLayout {

    private final TeacherService teacherService;
    private final ComboBox<TeacherSubjectResponse> subjectComboBox = new ComboBox<>("Select Subject");
    private final ComboBox<SessionSummaryResponse> sessionComboBox = new ComboBox<>("Select Session");
    private final DatePicker fromDate = new DatePicker("From Date");
    private final DatePicker toDate = new DatePicker("To Date");
    private final Grid<SessionAttendanceRowResponse> grid = new Grid<>(SessionAttendanceRowResponse.class, false);
    
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");

    public AttendanceBySessionView(TeacherService teacherService) {
        this.teacherService = teacherService;
        
        setSizeFull();
        setPadding(true);

        H2 header = new H2("Attendance by Session");
        
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
        subjectComboBox.setWidth("300px");

        fromDate.setValue(LocalDate.now().minusDays(30));
        toDate.setValue(LocalDate.now());

        sessionComboBox.setItemLabelGenerator(session -> {
            String startTimeStr = session.getStartTime() != null ? session.getStartTime().format(TIME_FORMAT) : "--";
            String endTimeStr = session.getEndTime() != null ? session.getEndTime().format(TIME_FORMAT) : "--";
            return String.format("%s | %s - %s", session.getSessionDate(), startTimeStr, endTimeStr);
        });
        sessionComboBox.setWidth("350px");

        subjectComboBox.addValueChangeListener(e -> loadSessions());
        fromDate.addValueChangeListener(e -> loadSessions());
        toDate.addValueChangeListener(e -> loadSessions());
        sessionComboBox.addValueChangeListener(e -> loadReportData());
    }

    private HorizontalLayout createFilterLayout() {
        HorizontalLayout layout = new HorizontalLayout(subjectComboBox, fromDate, toDate, sessionComboBox);
        layout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        return layout;
    }

    private void setupGrid() {
        grid.addColumn(SessionAttendanceRowResponse::getStudentPrn)
                .setHeader("PRN").setSortable(true).setAutoWidth(true);
                
        grid.addColumn(SessionAttendanceRowResponse::getStudentName)
                .setHeader("Name").setSortable(true).setAutoWidth(true);

        grid.addComponentColumn(r -> {
            Span badge = new Span();
            if (r.getStatus() == AttendanceStatus.PRESENT) {
                badge.setText("Present");
                badge.getElement().getThemeList().add("badge success");
            } else {
                badge.setText("Absent");
                badge.getElement().getThemeList().add("badge error");
            }
            return badge;
        }).setHeader("Status").setAutoWidth(true).setSortable(true).setComparator((r1, r2) -> r1.getStatus().compareTo(r2.getStatus()));

        grid.addColumn(r -> r.getScannedAt() != null ? r.getScannedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a")) : "--")
                .setHeader("Scanned At").setAutoWidth(true);

        grid.setSizeFull();
    }

    private void loadSubjects() {
        String prn = SecurityContextHolder.getContext().getAuthentication().getName();
        List<TeacherSubjectResponse> subjects = teacherService.getMySubjects(prn);
        subjectComboBox.setItems(subjects);
        if (!subjects.isEmpty()) {
            subjectComboBox.setValue(subjects.get(0));
        }
    }
    
    private void loadSessions() {
        if (subjectComboBox.getValue() == null || fromDate.getValue() == null || toDate.getValue() == null) {
            sessionComboBox.setItems();
            return;
        }

        String prn = SecurityContextHolder.getContext().getAuthentication().getName();
        List<SessionSummaryResponse> sessions = teacherService.getSessionsForSubject(
                prn, 
                subjectComboBox.getValue().getSubjectId(), 
                fromDate.getValue(), 
                toDate.getValue()
        );
        sessionComboBox.setItems(sessions);
    }

    private void loadReportData() {
        if (sessionComboBox.getValue() == null) {
            grid.setItems();
            return;
        }

        String prn = SecurityContextHolder.getContext().getAuthentication().getName();
        List<SessionAttendanceRowResponse> data = teacherService.getAttendanceBySession(
                prn, 
                sessionComboBox.getValue().getSessionId()
        );
        grid.setItems(data);
    }
}
