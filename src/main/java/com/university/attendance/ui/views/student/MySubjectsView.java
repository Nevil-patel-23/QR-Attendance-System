package com.university.attendance.ui.views.student;

import com.university.attendance.dto.response.StudentSubjectResponse;
import com.university.attendance.models.SubjectType;
import com.university.attendance.service.StudentService;
import com.university.attendance.security.JwtUtil;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * "My Subjects" view — two sections showing compulsory and elective
 * subjects with name, code, credits, and teacher columns.
 */
@Route(value = "student/subjects", layout = StudentLayout.class)
@PageTitle("My Subjects")
public class MySubjectsView extends VerticalLayout {

    public MySubjectsView(StudentService studentService, JwtUtil jwtUtil) {
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("My Subjects");
        add(title);

        try {
            String prn = resolveCurrentPrn(jwtUtil);
            List<StudentSubjectResponse> allSubjects = studentService.getMySubjects(prn);

            List<StudentSubjectResponse> compulsory = allSubjects.stream()
                    .filter(s -> s.getSubjectType() == SubjectType.COMPULSORY)
                    .collect(Collectors.toList());

            List<StudentSubjectResponse> electives = allSubjects.stream()
                    .filter(s -> s.getSubjectType() == SubjectType.ELECTIVE)
                    .collect(Collectors.toList());

            // Compulsory section
            H3 compTitle = new H3("Compulsory Subjects");
            add(compTitle);
            if (compulsory.isEmpty()) {
                Paragraph none = new Paragraph("None enrolled");
                none.getStyle().set("color", "var(--lumo-secondary-text-color)");
                add(none);
            } else {
                add(createSubjectGrid(compulsory));
            }

            // Elective section
            H3 elTitle = new H3("My Electives");
            add(elTitle);
            if (electives.isEmpty()) {
                Paragraph none = new Paragraph("None enrolled");
                none.getStyle().set("color", "var(--lumo-secondary-text-color)");
                add(none);
            } else {
                add(createSubjectGrid(electives));
            }

        } catch (Exception e) {
            Paragraph error = new Paragraph("Error loading subjects: " + e.getMessage());
            error.getStyle().set("color", "var(--lumo-error-text-color)");
            add(error);
        }
    }

    private Grid<StudentSubjectResponse> createSubjectGrid(List<StudentSubjectResponse> subjects) {
        Grid<StudentSubjectResponse> grid = new Grid<>();
        grid.setAllRowsVisible(true);
        grid.setWidthFull();

        grid.addColumn(StudentSubjectResponse::getSubjectName)
                .setHeader("Subject Name").setAutoWidth(true);
        grid.addColumn(StudentSubjectResponse::getSubjectCode)
                .setHeader("Code").setAutoWidth(true);
        grid.addColumn(s -> String.valueOf(s.getCredits()))
                .setHeader("Credits").setAutoWidth(true);
        grid.addColumn(StudentSubjectResponse::getTeacherName)
                .setHeader("Teacher").setAutoWidth(true);

        grid.setItems(subjects);
        return grid;
    }

    private String resolveCurrentPrn(JwtUtil jwtUtil) {
        HttpServletRequest request = (HttpServletRequest) VaadinRequest.getCurrent();
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    return jwtUtil.extractPrn(cookie.getValue());
                }
            }
        }
        throw new RuntimeException("Not authenticated");
    }
}
