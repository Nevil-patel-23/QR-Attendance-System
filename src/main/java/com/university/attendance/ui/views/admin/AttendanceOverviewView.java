package com.university.attendance.ui.views.admin;

import com.university.attendance.dto.response.*;
import com.university.attendance.service.AdminService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.RolesAllowed;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Route(value = "admin/attendance/overview", layout = AdminLayout.class)
@PageTitle("Attendance Overview")
@RolesAllowed("ADMIN")
public class AttendanceOverviewView extends VerticalLayout {

    private final AdminService adminService;

    private final ComboBox<FacultyResponse> facultyCombo = new ComboBox<>("Faculty");
    private final ComboBox<CourseResponse> courseCombo = new ComboBox<>("Course");
    private final ComboBox<SemesterResponse> semesterCombo = new ComboBox<>("Semester");
    private final DatePicker fromDate = new DatePicker("From Date");
    private final DatePicker toDate = new DatePicker("To Date");
    private final Grid<StudentAttendanceMatrixRow> grid = new Grid<>(StudentAttendanceMatrixRow.class, false);
    private final Span summaryLabel = new Span();
    private final Anchor exportAnchor = new Anchor();

    private AttendanceMatrixResponse lastResult;

    public AttendanceOverviewView(AdminService adminService) {
        this.adminService = adminService;

        setSizeFull();
        setPadding(true);

        H2 header = new H2("Attendance Overview");

        setupFilters();
        setupGrid();
        setupExportButton();

        summaryLabel.getStyle().set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "bold")
                .set("margin-top", "var(--lumo-space-m)");
        summaryLabel.setVisible(false);

        add(header, createFilterLayout(), summaryLabel, grid);
        loadFaculties();
    }

    private void setupFilters() {
        facultyCombo.setItemLabelGenerator(FacultyResponse::getName);
        facultyCombo.setWidth("250px");
        facultyCombo.addValueChangeListener(e -> {
            courseCombo.clear();
            semesterCombo.clear();
            if (e.getValue() != null) {
                loadCourses(e.getValue().getFacultyId());
            }
        });

        courseCombo.setItemLabelGenerator(c -> c.getName() + " (" + c.getCode() + ")");
        courseCombo.setWidth("250px");
        courseCombo.addValueChangeListener(e -> {
            semesterCombo.clear();
            if (e.getValue() != null) {
                loadSemesters(e.getValue().getCourseId());
            }
        });

        semesterCombo.setItemLabelGenerator(SemesterResponse::getLabel);
        semesterCombo.setWidth("250px");
        semesterCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                loadCalendarDates(e.getValue().getSemesterId());
            }
        });

        fromDate.setValue(LocalDate.now().withDayOfMonth(1));
        toDate.setValue(LocalDate.now());
    }

    private void loadCalendarDates(java.util.UUID semesterId) {
        AcademicCalendarResponse cal = adminService.findCalendarForSemester(semesterId);
        if (cal != null) {
            fromDate.setValue(cal.getStartDate());
            toDate.setValue(cal.getEndDate());
        } else {
            fromDate.setValue(LocalDate.now().withDayOfMonth(1));
            toDate.setValue(LocalDate.now());
        }
    }

    private HorizontalLayout createFilterLayout() {
        Button searchBtn = new Button("Search", VaadinIcon.SEARCH.create());
        searchBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchBtn.addClickListener(e -> loadReportData());

        HorizontalLayout filterRow = new HorizontalLayout(
                facultyCombo, courseCombo, semesterCombo, fromDate, toDate, searchBtn, exportAnchor);
        filterRow.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        filterRow.setFlexGrow(0, exportAnchor);
        return filterRow;
    }

    private void setupGrid() {
        grid.setSizeFull();
        rebuildGridColumns(null);
    }

    private void setupExportButton() {
        Button exportBtn = new Button("Export Excel", VaadinIcon.DOWNLOAD.create());
        exportBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        exportAnchor.add(exportBtn);
        exportAnchor.getElement().setAttribute("download", true);
        exportAnchor.setVisible(false);
    }

    private void loadFaculties() {
        List<FacultyResponse> faculties = adminService.getAllFaculties();
        facultyCombo.setItems(faculties);
    }

    private void loadCourses(java.util.UUID facultyId) {
        List<CourseResponse> courses = adminService.getCoursesByFaculty(facultyId);
        courseCombo.setItems(courses);
    }

    private void loadSemesters(java.util.UUID courseId) {
        List<SemesterResponse> semesters = adminService.getSemestersByCourse(courseId);
        semesterCombo.setItems(semesters);
    }

    private void loadReportData() {
        if (semesterCombo.getValue() == null) {
            Notification.show("Please select Faculty, Course, and Semester");
            return;
        }

        java.util.UUID semId = semesterCombo.getValue().getSemesterId();

        // Resolve academic year from calendar
        AcademicCalendarResponse cal = adminService.findCalendarForSemester(semId);
        String academicYear;
        if (cal != null) {
            academicYear = cal.getAcademicYear();
        } else {
            // Fallback: derive from fromDate picker
            LocalDate fd = fromDate.getValue() != null ? fromDate.getValue() : LocalDate.now();
            int year = fd.getYear();
            if (fd.getMonthValue() >= 6) {
                academicYear = String.valueOf(year) + String.valueOf(year + 1).substring(2);
            } else {
                academicYear = String.valueOf(year - 1) + String.valueOf(year).substring(2);
            }
        }

        lastResult = adminService.getAttendanceMatrix(
                semId,
                fromDate.getValue(),
                toDate.getValue(),
                academicYear);

        if (lastResult.getTotalStudents() == 0) {
            Notification.show("No students found for Batch " + academicYear + " in this Semester.");
            grid.removeAllColumns();
            grid.setItems(java.util.Collections.emptyList());
            summaryLabel.setVisible(false);
            exportAnchor.setVisible(false);
            return;
        }

        // Summary label
        summaryLabel.setText(String.format("%d students — %d at risk — %s %s",
                lastResult.getTotalStudents(),
                lastResult.getAtRiskCount(),
                lastResult.getCourseName(),
                lastResult.getSemesterLabel()));
        summaryLabel.setVisible(true);

        // Rebuild grid
        rebuildGridColumns(lastResult);
        grid.setItems(lastResult.getRows());

        // Enable export
        updateExportLink();
    }

    private void rebuildGridColumns(AttendanceMatrixResponse result) {
        grid.removeAllColumns();

        // Fixed columns
        grid.addColumn(StudentAttendanceMatrixRow::getStudentPrn)
                .setHeader("PRN").setSortable(true).setAutoWidth(true).setFrozen(true);
        grid.addColumn(StudentAttendanceMatrixRow::getStudentName)
                .setHeader("Name").setSortable(true).setAutoWidth(true).setFrozen(true);

        if (result == null) return;

        // Compulsory subject columns
        List<String> compHeaders = result.getCompulsorySubjectHeaders();
        for (int i = 0; i < compHeaders.size(); i++) {
            final int idx = i;
            String code = compHeaders.get(idx);
            grid.addComponentColumn(row -> {
                SubjectAttendanceSummary s = row.getCompulsorySubjects().get(idx);
                return createCellBadge(s);
            }).setHeader(code).setAutoWidth(true);
        }

        // Elective subject columns
        List<String> elecHeaders = result.getElectiveSubjectHeaders();
        for (int i = 0; i < elecHeaders.size(); i++) {
            final int idx = i;
            String code = elecHeaders.get(idx);
            grid.addComponentColumn(row -> {
                SubjectAttendanceSummary s = row.getElectiveSubjects().get(idx);
                return createCellBadge(s);
            }).setHeader(code).setAutoWidth(true);
        }
    }

    private Span createCellBadge(SubjectAttendanceSummary s) {
        Span badge = new Span();
        if (!s.isEnrolled()) {
            badge.setText("—");
            badge.getStyle().set("color", "var(--lumo-contrast-50pct)");
        } else if (s.getTotalSessions() == 0) {
            badge.setText("0/0");
            badge.getStyle().set("color", "var(--lumo-contrast-50pct)");
        } else {
            badge.setText(String.format("%.1f%%", s.getAttendancePercentage()));
            if (s.isAtRisk()) {
                badge.getStyle().set("color", "var(--lumo-error-text-color)")
                        .set("font-weight", "bold");
            } else {
                badge.getStyle().set("color", "var(--lumo-success-text-color)")
                        .set("font-weight", "bold");
            }
        }
        return badge;
    }

    private void updateExportLink() {
        if (lastResult == null) {
            exportAnchor.setVisible(false);
            return;
        }

        String filename = String.format("Attendance_%s_%s_%s_%s.xlsx",
                lastResult.getCourseCode(),
                lastResult.getSemesterLabel().replaceAll("\\s+", "_"),
                lastResult.getFromDate(), lastResult.getToDate());

        StreamResource resource = new StreamResource(filename, () -> {
            try {
                return new ByteArrayInputStream(generateExcel(lastResult));
            } catch (Exception e) {
                Notification.show("Export failed: " + e.getMessage());
                return new ByteArrayInputStream(new byte[0]);
            }
        });
        resource.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        exportAnchor.setHref(resource);
        exportAnchor.setVisible(true);
    }

    private byte[] generateExcel(AttendanceMatrixResponse data) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Attendance");

            // Styles
            XSSFCellStyle greenStyle = workbook.createCellStyle();
            greenStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 198, (byte) 239, (byte) 206}, null));
            greenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle redStyle = workbook.createCellStyle();
            redStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 255, (byte) 199, (byte) 206}, null));
            redStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle grayStyle = workbook.createCellStyle();
            grayStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 217, (byte) 217, (byte) 217}, null));
            grayStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 68, (byte) 114, (byte) 196}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font whiteFont = workbook.createFont();
            whiteFont.setBold(true);
            whiteFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(whiteFont);

            List<String> compHeaders = data.getCompulsorySubjectHeaders();
            List<String> elecHeaders = data.getElectiveSubjectHeaders();

            // Group header row (row 0)
            Row groupRow = sheet.createRow(0);
            int colIdx = 0;
            groupRow.createCell(colIdx++); // PRN
            groupRow.createCell(colIdx++); // Name

            if (!compHeaders.isEmpty()) {
                Cell groupCell = groupRow.createCell(colIdx);
                groupCell.setCellValue("Compulsory Subjects");
                groupCell.setCellStyle(headerStyle);
                if (compHeaders.size() > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(0, 0, colIdx, colIdx + compHeaders.size() - 1));
                }
                colIdx += compHeaders.size();
            }

            if (!elecHeaders.isEmpty()) {
                Cell groupCell = groupRow.createCell(colIdx);
                groupCell.setCellValue("Elective Subjects");
                groupCell.setCellStyle(headerStyle);
                if (elecHeaders.size() > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(0, 0, colIdx, colIdx + elecHeaders.size() - 1));
                }
            }

            // Column header row (row 1)
            Row headerRow = sheet.createRow(1);
            colIdx = 0;
            Cell prnHeader = headerRow.createCell(colIdx++);
            prnHeader.setCellValue("PRN");
            prnHeader.setCellStyle(headerStyle);

            Cell nameHeader = headerRow.createCell(colIdx++);
            nameHeader.setCellValue("Student Name");
            nameHeader.setCellStyle(headerStyle);

            for (String code : compHeaders) {
                Cell cell = headerRow.createCell(colIdx++);
                cell.setCellValue(code);
                cell.setCellStyle(headerStyle);
            }
            for (String code : elecHeaders) {
                Cell cell = headerRow.createCell(colIdx++);
                cell.setCellValue(code);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 2;
            for (StudentAttendanceMatrixRow student : data.getRows()) {
                Row row = sheet.createRow(rowIdx++);
                colIdx = 0;
                row.createCell(colIdx++).setCellValue(student.getStudentPrn());
                row.createCell(colIdx++).setCellValue(student.getStudentName());

                for (SubjectAttendanceSummary s : student.getCompulsorySubjects()) {
                    Cell cell = row.createCell(colIdx++);
                    writeSummaryCell(cell, s, greenStyle, redStyle, grayStyle);
                }
                for (SubjectAttendanceSummary s : student.getElectiveSubjects()) {
                    Cell cell = row.createCell(colIdx++);
                    writeSummaryCell(cell, s, greenStyle, redStyle, grayStyle);
                }
            }

            // Auto-size columns
            for (int i = 0; i < colIdx; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void writeSummaryCell(Cell cell, SubjectAttendanceSummary s,
                                  CellStyle greenStyle, CellStyle redStyle, CellStyle grayStyle) {
        if (!s.isEnrolled()) {
            cell.setCellValue("—");
            cell.setCellStyle(grayStyle);
        } else if (s.getTotalSessions() == 0) {
            cell.setCellValue("0/0");
        } else {
            cell.setCellValue(String.format("%.1f%%", s.getAttendancePercentage()));
            cell.setCellStyle(s.isAtRisk() ? redStyle : greenStyle);
        }
    }
}
