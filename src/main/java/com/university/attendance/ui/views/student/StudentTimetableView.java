package com.university.attendance.ui.views.student;

import com.university.attendance.dto.response.TimetableSlotResponse;
import com.university.attendance.models.DayOfWeek;
import com.university.attendance.service.StudentService;
import com.university.attendance.security.JwtUtil;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Weekly timetable grid for the student. Days as columns (Mon–Sat),
 * time slots as rows. Elective subjects get a small badge.
 */
@Route(value = "student/timetable", layout = StudentLayout.class)
@PageTitle("My Timetable")
public class StudentTimetableView extends VerticalLayout {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DayOfWeek[] DAYS = {
            DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED,
            DayOfWeek.THU, DayOfWeek.FRI, DayOfWeek.SAT
    };

    public StudentTimetableView(StudentService studentService, JwtUtil jwtUtil) {
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("My Weekly Timetable");
        add(title);

        try {
            String prn = resolveCurrentPrn(jwtUtil);
            List<TimetableSlotResponse> slots = studentService.getTimetable(prn);

            if (slots.isEmpty()) {
                Paragraph empty = new Paragraph("No timetable slots found for your current semester.");
                empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
                add(empty);
                return;
            }

            // Group by unique time ranges then build table
            buildTimetableGrid(slots);
        } catch (Exception e) {
            Paragraph error = new Paragraph("Error loading timetable: " + e.getMessage());
            error.getStyle().set("color", "var(--lumo-error-text-color)");
            add(error);
        }
    }

    private void buildTimetableGrid(List<TimetableSlotResponse> slots) {
        // Collect unique time ranges, sorted by start time
        Set<String> timeRanges = new LinkedHashSet<>();
        Map<String, Map<DayOfWeek, TimetableSlotResponse>> grid = new LinkedHashMap<>();

        slots.sort(Comparator.comparing(TimetableSlotResponse::getStartTime));
        for (TimetableSlotResponse slot : slots) {
            String timeKey = slot.getStartTime().format(TIME_FMT) + " – " + slot.getEndTime().format(TIME_FMT);
            timeRanges.add(timeKey);
            grid.computeIfAbsent(timeKey, k -> new EnumMap<>(DayOfWeek.class)).put(slot.getDayOfWeek(), slot);
        }

        // Build HTML table
        // Header row: Time | Mon | Tue | ...
        StringBuilder html = new StringBuilder();
        html.append("<table style='width:100%; border-collapse:collapse; text-align:center;'>");
        html.append("<thead><tr style='background-color:var(--lumo-contrast-5pct);'>");
        html.append("<th style='padding:12px; border:1px solid var(--lumo-contrast-20pct); font-weight:600;'>Time</th>");
        for (DayOfWeek day : DAYS) {
            html.append("<th style='padding:12px; border:1px solid var(--lumo-contrast-20pct); font-weight:600;'>")
                    .append(capitalize(day.name())).append("</th>");
        }
        html.append("</tr></thead><tbody>");

        for (String timeKey : timeRanges) {
            Map<DayOfWeek, TimetableSlotResponse> row = grid.getOrDefault(timeKey, Collections.emptyMap());
            html.append("<tr>");
            html.append("<td style='padding:10px; border:1px solid var(--lumo-contrast-20pct); font-weight:500; white-space:nowrap;'>")
                    .append(timeKey).append("</td>");

            for (DayOfWeek day : DAYS) {
                html.append("<td style='padding:10px; border:1px solid var(--lumo-contrast-20pct); vertical-align:top;'>");
                TimetableSlotResponse slot = row.get(day);
                if (slot != null) {
                    html.append("<div style='font-weight:600;'>").append(escapeHtml(slot.getSubjectName())).append("</div>");
                    html.append("<div style='font-size:0.85em; color:var(--lumo-secondary-text-color);'>")
                            .append(escapeHtml(slot.getSubjectCode())).append("</div>");
                    html.append("<div style='font-size:0.85em; color:var(--lumo-secondary-text-color);'>Room: ")
                            .append(escapeHtml(slot.getRoom())).append("</div>");
                    html.append("<div style='font-size:0.8em; color:var(--lumo-tertiary-text-color);'>")
                            .append(escapeHtml(slot.getTeacherName())).append("</div>");
                }
                html.append("</td>");
            }
            html.append("</tr>");
        }

        html.append("</tbody></table>");

        Div tableDiv = new Div();
        tableDiv.getElement().setProperty("innerHTML", html.toString());
        tableDiv.setWidthFull();
        tableDiv.getStyle().set("overflow-x", "auto");
        add(tableDiv);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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
