package com.example.managementsystem;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;

public class EmployeeDashboardController {
    @FXML private Label welcomeLabel;
    @FXML private Pane contentPane;


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
        loadContent("/com/example/managementsystem/employee_home.fxml");
    }

    @FXML
    private void showNewBooking() {
        loadContent("/com/example/managementsystem/employee_new_booking.fxml");
    }

    @FXML
    private void showManageBookings() {
        loadContent("/com/example/managementsystem/employee_manage_bookings.fxml");
    }

    @FXML
    private void showProcessPayments() {
        loadContent("/com/example/managementsystem/employee_payments.fxml");
    }

    @FXML
    private void handleLogout() {
        try {

            Parent root = FXMLLoader.load(getClass().getResource("/com/example/managementsystem/login.fxml"));
            Stage stage = (Stage) contentPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to logout", e.getMessage());
        }
    }

    private void loadContent(String fxmlPath) {
        try {
            Parent content = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().clear();
            contentPane.getChildren().add(content);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load content", e.getMessage());
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