package com.example.managementsystem;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.*;
import java.time.temporal.ChronoUnit;

public class ModifyBookingController {
    @FXML private Label customerLabel;
    @FXML private Label vehicleLabel;
    @FXML private Label currentStartDateLabel;
    @FXML private Label currentEndDateLabel;
    @FXML private DatePicker newEndDatePicker;
    @FXML private TextField additionalChargesField;

    private Stage dialogStage;
    private Rental rental;
    private boolean saveClicked = false;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setRental(Rental rental) {
        this.rental = rental;
        customerLabel.setText(rental.getCustomerName());
        vehicleLabel.setText(rental.getVehicleInfo());
        currentStartDateLabel.setText(rental.getStartDate());
        currentEndDateLabel.setText(rental.getEndDate());
        newEndDatePicker.setValue(java.sql.Date.valueOf(rental.getEndDate()).toLocalDate());
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Calculate additional days
                java.sql.Date oldEndDate = java.sql.Date.valueOf(rental.getEndDate());
                java.sql.Date newEndDate = java.sql.Date.valueOf(newEndDatePicker.getValue());

                // Get vehicle price per day
                double pricePerDay = 0;
                String priceSql = "SELECT price_per_day FROM vehicles v " +
                        "JOIN rentals r ON v.vehicle_id = r.vehicle_id " +
                        "WHERE r.rental_id = ?";
                PreparedStatement priceStmt = conn.prepareStatement(priceSql);
                priceStmt.setInt(1, rental.getId());
                ResultSet priceRs = priceStmt.executeQuery();
                if (priceRs.next()) {
                    pricePerDay = priceRs.getDouble("price_per_day");
                }

                // Calculate additional amount
                long additionalDays = ChronoUnit.DAYS.between(
                        oldEndDate.toLocalDate(),
                        newEndDate.toLocalDate()
                );
                double additionalAmount = additionalDays * pricePerDay;
                double extraCharges = additionalChargesField.getText().isEmpty() ? 0 :
                        Double.parseDouble(additionalChargesField.getText());
                double newTotalAmount = rental.getAmount() + additionalAmount + extraCharges;

                // Update the booking
                String updateSql = "UPDATE rentals SET return_date = ?, total_amount = ? " +
                        "WHERE rental_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setDate(1, newEndDate);
                updateStmt.setDouble(2, newTotalAmount);
                updateStmt.setInt(3, rental.getId());

                int rowsAffected = updateStmt.executeUpdate();
                if (rowsAffected > 0) {
                    saveClicked = true;
                    dialogStage.close();
                }
            } catch (SQLException e) {
                showAlert("Database Error", "Could not update booking", e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (newEndDatePicker.getValue() == null) {
            errorMessage += "Please select a new end date!\n";
        } else {
            java.sql.Date oldEndDate = java.sql.Date.valueOf(rental.getEndDate());
            if (newEndDatePicker.getValue().isBefore(oldEndDate.toLocalDate())) {
                errorMessage += "New end date must be after current end date!\n";
            }
        }

        if (!additionalChargesField.getText().isEmpty()) {
            try {
                Double.parseDouble(additionalChargesField.getText());
            } catch (NumberFormatException e) {
                errorMessage += "Additional charges must be a valid number!\n";
            }
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