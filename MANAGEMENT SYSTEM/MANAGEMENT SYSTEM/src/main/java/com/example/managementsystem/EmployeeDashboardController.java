package com.example.managementsystem;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.io.IOException;

public class EmployeeDashboardController {

    @FXML private Label welcomeLabel;
    private String currentUsername;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome, " + (currentUsername != null ? currentUsername : "Employee"));
    }

    public void setCurrentUser(String username) {
        this.currentUsername = username;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + username);
        }
    }

    @FXML
    private void showDashboard() {
        openNewScene("/com/example/managementsystem/employee_home.fxml", "Employee Dashboard");
    }

    @FXML
    private void showNewBooking() {
        openNewScene("/com/example/managementsystem/employee_new_booking.fxml", "New Booking");
    }

    @FXML
    private void showManageBookings() {
        openNewScene("/com/example/managementsystem/employee_manage_bookings.fxml", "Manage Bookings");
    }

    @FXML
    private void showProcessPayments() {
        openNewScene("/com/example/managementsystem/employee_payments.fxml", "Process Payments");
    }

    @FXML
    private void handleLogout() {
        openNewScene("/com/example/managementsystem/login.fxml", "Login");
    }

    private void openNewScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();


            Stage currentStage = (Stage) welcomeLabel.getScene().getWindow();
            currentStage.setTitle(title);
            currentStage.setScene(new Scene(root));
            currentStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load: " + title, e.getMessage());
        }
    }

    private void showAlert(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
