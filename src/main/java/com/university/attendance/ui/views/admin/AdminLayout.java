package com.university.attendance.ui.views.admin;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Shared layout for all admin pages. Implements BeforeEnterObserver to
 * enforce ADMIN-only access via SecurityContextHolder at the layout level.
 */
public class AdminLayout extends AppLayout implements BeforeEnterObserver {

    private Span viewTitle;

    public AdminLayout() {
        createHeader();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().isEmpty()) {
            event.getUI().getPage().setLocation("/");
            return;
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_ADMIN".equals(role));
        if (!isAdmin) {
            boolean isStudent = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> "ROLE_STUDENT".equals(role));
            boolean isTeacher = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> "ROLE_TEACHER".equals(role));

            if (isStudent) {
                event.getUI().getPage().setLocation("/student");
            } else if (isTeacher) {
                event.getUI().getPage().setLocation("/teacher");
            } else {
                event.getUI().getPage().setLocation("/");
            }
        }
    }

    private void createHeader() {
        // ── Left side: App name + breadcrumb ──
        RouterLink adminLink = new RouterLink("QR Attendance", AdminDashboardView.class);
        adminLink.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);
        adminLink.getStyle().set("text-decoration", "none").set("color", "var(--lumo-primary-color)");

        Span separator = new Span(" > ");
        separator.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.Horizontal.SMALL);

        viewTitle = new Span();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE);

        HorizontalLayout left = new HorizontalLayout(adminLink, separator, viewTitle);
        left.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        // ── Right side: Profile Menu ──
        String adminName = "Admin User";
        String prn = "";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            prn = auth.getName();
        }
        
        com.vaadin.flow.component.avatar.Avatar avatar = new com.vaadin.flow.component.avatar.Avatar(adminName);
        avatar.setAbbreviation("AD");
        avatar.getStyle().set("background-color", "var(--lumo-primary-color)"); // Blue
        avatar.getStyle().set("color", "white");
        avatar.getStyle().set("cursor", "pointer");
        
        com.vaadin.flow.component.contextmenu.ContextMenu contextMenu = new com.vaadin.flow.component.contextmenu.ContextMenu(avatar);
        contextMenu.setOpenOnClick(true);
        
        com.vaadin.flow.component.avatar.Avatar largeAvatar = new com.vaadin.flow.component.avatar.Avatar(adminName);
        largeAvatar.setAbbreviation("AD");
        largeAvatar.getStyle()
            .set("background-color", "var(--lumo-primary-color)")
            .set("color", "white")
            .set("width", "var(--lumo-size-xl)")
            .set("height", "var(--lumo-size-xl)");
        
        Span nameLabel = new Span(adminName);
        nameLabel.getStyle().set("font-weight", "bold");
        Span prnLabel = new Span(prn.isEmpty() ? "N/A" : prn);
        prnLabel.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");
        
        com.vaadin.flow.component.orderedlayout.VerticalLayout headerLayout = new com.vaadin.flow.component.orderedlayout.VerticalLayout(largeAvatar, nameLabel, prnLabel);
        headerLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
        headerLayout.setSpacing(false);
        headerLayout.getStyle().set("padding", "var(--lumo-space-m)");
        
        contextMenu.add(headerLayout);
        contextMenu.add(new com.vaadin.flow.component.html.Hr());
        
        contextMenu.addItem("My Profile", e -> UI.getCurrent().navigate("admin/profile"));
        contextMenu.addItem("Change Password", e -> UI.getCurrent().navigate("admin/profile/password"));
        contextMenu.add(new com.vaadin.flow.component.html.Hr());
        
        Span logoutSpan = new Span("Logout");
        logoutSpan.getStyle().set("color", "var(--lumo-error-text-color)").set("font-weight", "bold");
        contextMenu.addItem(logoutSpan, e -> {
            UI.getCurrent().getPage().executeJs(
                    "fetch('/api/v1/auth/logout', {method:'POST'}).then(() => { window.location.href='/'; })");
        });

        HorizontalLayout right = new HorizontalLayout(avatar);
        right.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        right.setSpacing(true);

        // ── Full header ──
        HorizontalLayout fullHeader = new HorizontalLayout(left, right);
        fullHeader.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        fullHeader.setWidthFull();
        fullHeader.expand(left);
        fullHeader.addClassNames(LumoUtility.Padding.Vertical.MEDIUM, LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(fullHeader);
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        if (getContent() != null) {
            PageTitle titleAnnotation = getContent().getClass().getAnnotation(PageTitle.class);
            String title = titleAnnotation != null ? titleAnnotation.value() : "";
            viewTitle.setText(title);
        }
    }
}
