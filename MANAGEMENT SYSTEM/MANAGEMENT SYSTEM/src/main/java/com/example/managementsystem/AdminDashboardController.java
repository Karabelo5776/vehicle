package com.example.managementsystem;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class AdminDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private AnchorPane contentPane;

    private String currentUsername;

    public void initialize() {
        welcomeLabel.setText("Welcome, " + (currentUsername != null ? currentUsername : "Admin"));
    }

    public void setCurrentUser(String username) {
        this.currentUsername = username;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + username);
        }
    }

    @FXML
    private void showDashboard() {
        openNewWindow("/com/example/managementsystem/admin_home.fxml", "Dashboard");
    }

    @FXML
    private void showVehicleManagement() {
        openNewWindow("/com/example/managementsystem/admin_vehicles.fxml", "Vehicle Management");
    }

    @FXML
    private void showCustomerManagement() {
        openNewWindow("/com/example/managementsystem/admin_customers.fxml", "Customer Management");
    }

    @FXML
    private void showUserManagement() {
        openNewWindow("/com/example/managementsystem/admin_users.fxml", "User Management");
    }

    @FXML
    private void showReports() {
        openNewWindow("/com/example/managementsystem/admin_reports.fxml", "Reports");
    }

    @FXML
    private void handleLogout() {
        openNewWindow("/com/example/managementsystem/login.fxml", "Login");
    }

    private void openNewWindow(String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();


            Stage currentStage = (Stage) contentPane.getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
