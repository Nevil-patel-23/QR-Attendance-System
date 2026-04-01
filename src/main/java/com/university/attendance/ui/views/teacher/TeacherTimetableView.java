package com.university.attendance.ui.views.teacher;

import com.university.attendance.dto.response.TeacherTimetableSlotResponse;
import com.university.attendance.service.TeacherService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

@Route(value = "teacher/timetable", layout = TeacherLayout.class)
@PageTitle("My Timetable")
@RolesAllowed("TEACHER")
public class TeacherTimetableView extends VerticalLayout {

    private final TeacherService teacherService;
    
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final com.university.attendance.models.DayOfWeek[] DAYS = {
            com.university.attendance.models.DayOfWeek.MON, com.university.attendance.models.DayOfWeek.TUE, com.university.attendance.models.DayOfWeek.WED,
            com.university.attendance.models.DayOfWeek.THU, com.university.attendance.models.DayOfWeek.FRI, com.university.attendance.models.DayOfWeek.SAT
    };

    public TeacherTimetableView(TeacherService teacherService) {
        this.teacherService = teacherService;
        
        setSizeFull();
        setPadding(true);

        H2 header = new H2("My Weekly Timetable");
        add(header);
        
        loadTimetable();
    }

    private void loadTimetable() {
        String prn = SecurityContextHolder.getContext().getAuthentication().getName();
        List<TeacherTimetableSlotResponse> slots = teacherService.getMyTimetable(prn);
        buildTimetableGrid(slots);
    }

    private void buildTimetableGrid(List<TeacherTimetableSlotResponse> slots) {
        if (slots == null || slots.isEmpty()) {
            Paragraph empty = new Paragraph("No timetable assigned yet");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
            add(empty);
            return;
        }

        Set<String> timeRanges = new LinkedHashSet<>();
        // Map<TimeKey, Map<DayOfWeek, List<TeacherTimetableSlotResponse>>>
        Map<String, Map<com.university.attendance.models.DayOfWeek, List<TeacherTimetableSlotResponse>>> gridMap = new LinkedHashMap<>();

        slots.sort(Comparator.comparing(TeacherTimetableSlotResponse::getStartTime));
        for (TeacherTimetableSlotResponse slot : slots) {
            String timeKey = slot.getStartTime().format(TIME_FMT) + " – " + slot.getEndTime().format(TIME_FMT);
            timeRanges.add(timeKey);
            gridMap.computeIfAbsent(timeKey, k -> new EnumMap<>(com.university.attendance.models.DayOfWeek.class))
                    .computeIfAbsent(slot.getDayOfWeek(), k -> new ArrayList<>())
                    .add(slot);
        }

        StringBuilder html = new StringBuilder();
        html.append("<table style='width:100%; border-collapse:collapse; text-align:center;'>");
        html.append("<thead><tr style='background-color:var(--lumo-contrast-5pct);'>");
        html.append("<th style='padding:12px; border:1px solid var(--lumo-contrast-20pct); font-weight:600;'>Time</th>");
        for (com.university.attendance.models.DayOfWeek day : DAYS) {
            html.append("<th style='padding:12px; border:1px solid var(--lumo-contrast-20pct); font-weight:600;'>")
                    .append(capitalize(day.name())).append("</th>");
        }
        html.append("</tr></thead><tbody>");

        for (String timeKey : timeRanges) {
            Map<com.university.attendance.models.DayOfWeek, List<TeacherTimetableSlotResponse>> row = gridMap.getOrDefault(timeKey, Collections.emptyMap());
            html.append("<tr>");
            html.append("<td style='padding:10px; border:1px solid var(--lumo-contrast-20pct); font-weight:500; white-space:nowrap;'>")
                    .append(timeKey).append("</td>");

            for (com.university.attendance.models.DayOfWeek day : DAYS) {
                html.append("<td style='padding:10px; border:1px solid var(--lumo-contrast-20pct); vertical-align:top;'>");
                List<TeacherTimetableSlotResponse> daySlots = row.get(day);
                if (daySlots != null && !daySlots.isEmpty()) {
                    for (int i = 0; i < daySlots.size(); i++) {
                        TeacherTimetableSlotResponse slot = daySlots.get(i);
                        html.append("<div style='font-weight:600;'>").append(escapeHtml(slot.getSubjectName())).append("</div>");
                        html.append("<div style='font-size:0.85em; color:var(--lumo-secondary-text-color);'>")
                                .append(escapeHtml(slot.getSubjectCode())).append("</div>");
                        html.append("<div style='font-size:0.85em; color:var(--lumo-secondary-text-color);'>")
                                .append(escapeHtml(slot.getSemesterLabel()))
                                .append(" | Room: ").append(escapeHtml(slot.getRoom())).append("</div>");
                        
                        if (i < daySlots.size() - 1) {
                            html.append("<hr style='border: 0; border-top: 1px dashed var(--lumo-contrast-20pct); margin: 8px 0;'>");
                        }
                    }
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
}
