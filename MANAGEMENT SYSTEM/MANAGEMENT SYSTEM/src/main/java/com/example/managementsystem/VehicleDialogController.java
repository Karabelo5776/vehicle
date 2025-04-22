package com.example.managementsystem;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class VehicleDialogController {
    @FXML private TextField brandField;
    @FXML private TextField modelField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField yearField;
    @FXML private TextField priceField;
    @FXML private ComboBox<String> statusCombo;

    private Stage dialogStage;
    private Vehicle vehicle;
    private boolean saveClicked = false;

    @FXML
    public void initialize() {

        categoryCombo.getItems().addAll("Car", "Bike", "Van", "Truck", "SUV");
        statusCombo.getItems().addAll("Available", "Rented", "Maintenance");
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;

        if (vehicle != null) {

            brandField.setText(vehicle.getBrand());
            modelField.setText(vehicle.getModel());
            categoryCombo.setValue(vehicle.getCategory());
            yearField.setText(String.valueOf(vehicle.getYear()));
            priceField.setText(String.valueOf(vehicle.getPricePerDay()));
            statusCombo.setValue(vehicle.getStatus());
        } else {

            statusCombo.setValue("Available");
        }
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            if (vehicle == null) {
                vehicle = new Vehicle(
                        0,
                        brandField.getText(),
                        modelField.getText(),
                        categoryCombo.getValue(),
                        Integer.parseInt(yearField.getText()),
                        Double.parseDouble(priceField.getText()),
                        statusCombo.getValue()
                );
            } else {
                vehicle.setBrand(brandField.getText());
                vehicle.setModel(modelField.getText());
                vehicle.setCategory(categoryCombo.getValue());
                vehicle.setYear(Integer.parseInt(yearField.getText()));
                vehicle.setPricePerDay(Double.parseDouble(priceField.getText()));
                vehicle.setStatus(statusCombo.getValue());
            }

            saveClicked = true;
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (brandField.getText() == null || brandField.getText().isEmpty()) {
            errorMessage += "Brand is required!\n";
        }
        if (modelField.getText() == null || modelField.getText().isEmpty()) {
            errorMessage += "Model is required!\n";
        }
        if (categoryCombo.getValue() == null) {
            errorMessage += "Category is required!\n";
        }

        try {
            Integer.parseInt(yearField.getText());
        } catch (NumberFormatException e) {
            errorMessage += "Year must be a valid number!\n";
        }

        try {
            Double.parseDouble(priceField.getText());
        } catch (NumberFormatException e) {
            errorMessage += "Price must be a valid number!\n";
        }

        if (statusCombo.getValue() == null) {
            errorMessage += "Status is required!\n";
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            showAlert("Invalid Fields", "Please correct invalid fields", errorMessage);
            return false;
        }
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}