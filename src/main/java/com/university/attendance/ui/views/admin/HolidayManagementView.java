package com.university.attendance.ui.views.admin;

import com.university.attendance.dto.request.CreateHolidayRequest;
import com.university.attendance.dto.response.HolidayImportRow;
import com.university.attendance.dto.response.HolidayResponse;
import com.university.attendance.exception.ValidationException;
import com.university.attendance.models.HolidayType;
import com.university.attendance.service.AdminService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.UUID;

@Route(value = "admin/holidays", layout = AdminLayout.class)
@PageTitle("Holiday Management")
@RolesAllowed("ADMIN")
public class HolidayManagementView extends VerticalLayout {

    private final AdminService adminService;
    private final Grid<HolidayResponse> grid = new Grid<>(HolidayResponse.class, false);

    private final TextField nameField = new TextField("Holiday Name");
    private final DatePicker datePicker = new DatePicker("Date");
    private final ComboBox<HolidayType> typeCombo = new ComboBox<>("Type");
    private final Button saveButton = new Button("Add Holiday");

    private UUID editingId = null;

    public HolidayManagementView(AdminService adminService) {
        this.adminService = adminService;

        setSizeFull();

        H2 title = new H2("Holiday Management");

        configureGrid();
        configureForm();

        HorizontalLayout formLayout = new HorizontalLayout(nameField, datePicker, typeCombo, saveButton);
        formLayout.setAlignItems(Alignment.BASELINE);
        formLayout.setWidthFull();

        // Excel import
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".xlsx", ".xls");
        upload.setUploadButton(new Button("Import from Excel"));
        upload.addSucceededListener(event -> {
            try {
                List<HolidayImportRow> previewRows = adminService.previewHolidayImport(buffer.getInputStream());
                showImportPreviewDialog(previewRows);
            } catch (Exception ex) {
                showError("Failed to read file: " + ex.getMessage());
            }
        });

        HorizontalLayout toolbar = new HorizontalLayout(upload);
        toolbar.setAlignItems(Alignment.BASELINE);

        add(title, formLayout, toolbar, grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(HolidayResponse::getName).setHeader("Name").setSortable(true);
        grid.addColumn(HolidayResponse::getDate).setHeader("Date").setSortable(true);
        grid.addColumn(h -> h.getType().name()).setHeader("Type").setSortable(true);

        grid.addComponentColumn(holiday -> {
            Button editBtn = new Button("Edit", e -> editHoliday(holiday));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            Button deleteBtn = new Button("Delete", e -> confirmDelete(holiday));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("Actions");

        grid.setSizeFull();
    }

    private void configureForm() {
        nameField.setWidth("250px");

        typeCombo.setItems(HolidayType.values());
        typeCombo.setItemLabelGenerator(HolidayType::name);
        typeCombo.setWidth("180px");

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveHoliday());
    }

    private void refreshGrid() {
        grid.setItems(adminService.getAllHolidays());
    }

    private void editHoliday(HolidayResponse holiday) {
        editingId = holiday.getHolidayId();
        nameField.setValue(holiday.getName());
        datePicker.setValue(holiday.getDate());
        typeCombo.setValue(holiday.getType());
        saveButton.setText("Update");
    }

    private void saveHoliday() {
        try {
            CreateHolidayRequest request = new CreateHolidayRequest();
            request.setName(nameField.getValue());
            request.setDate(datePicker.getValue());
            request.setType(typeCombo.getValue());

            if (editingId == null) {
                adminService.createHoliday(request);
                showSuccess("Holiday created successfully");
            } else {
                adminService.updateHoliday(editingId, request);
                showSuccess("Holiday updated successfully");
                editingId = null;
                saveButton.setText("Add Holiday");
            }

            clearForm();
            refreshGrid();
        } catch (ValidationException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Failed to save holiday: " + ex.getMessage());
        }
    }

    private void showImportPreviewDialog(List<HolidayImportRow> previewRows) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Holiday Import Preview");
        dialog.setWidth("800px");
        dialog.setHeight("600px");

        Grid<HolidayImportRow> previewGrid = new Grid<>(HolidayImportRow.class, false);
        previewGrid.addColumn(HolidayImportRow::getRowNumber).setHeader("Row").setWidth("60px");
        previewGrid.addColumn(HolidayImportRow::getName).setHeader("Name");
        previewGrid.addColumn(HolidayImportRow::getDate).setHeader("Date");
        previewGrid.addColumn(HolidayImportRow::getType).setHeader("Type");
        previewGrid.addComponentColumn(row -> {
            Span statusSpan = new Span(row.getErrorMessage());
            if (row.isValid()) {
                statusSpan.getStyle().set("color", "green");
                statusSpan.getStyle().set("font-weight", "bold");
            } else {
                statusSpan.getStyle().set("color", "red");
                statusSpan.getStyle().set("font-weight", "bold");
            }
            return statusSpan;
        }).setHeader("Status").setWidth("200px");

        previewGrid.setItems(previewRows);
        previewGrid.setSizeFull();

        long validCount = previewRows.stream().filter(HolidayImportRow::isValid).count();
        long invalidCount = previewRows.size() - validCount;
        Span summary = new Span(validCount + " valid, " + invalidCount + " invalid out of " + previewRows.size() + " rows");

        Button confirmBtn = new Button("Confirm Import", e -> {
            var result = adminService.confirmHolidayImport(previewRows);
            dialog.close();
            showSuccess(result.getSuccessCount() + " holidays imported successfully. "
                    + result.getFailedCount() + " rows skipped.");
            refreshGrid();
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        if (validCount == 0) {
            confirmBtn.setEnabled(false);
        }

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        HorizontalLayout buttons = new HorizontalLayout(confirmBtn, cancelBtn);

        VerticalLayout dialogContent = new VerticalLayout(summary, previewGrid, buttons);
        dialogContent.setSizeFull();
        dialog.add(dialogContent);
        dialog.open();
    }

    private void confirmDelete(HolidayResponse holiday) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Holiday");
        dialog.setText("Are you sure you want to delete \"" + holiday.getName() + "\"?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            try {
                adminService.deleteHoliday(holiday.getHolidayId());
                showSuccess("Holiday deleted");
                refreshGrid();
            } catch (ValidationException ex) {
                showError(ex.getMessage());
            } catch (Exception ex) {
                showError("Error deleting holiday");
            }
        });
        dialog.open();
    }

    private void clearForm() {
        nameField.clear();
        datePicker.clear();
        typeCombo.clear();
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
