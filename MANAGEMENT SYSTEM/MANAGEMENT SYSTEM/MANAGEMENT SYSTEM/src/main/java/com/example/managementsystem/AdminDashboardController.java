package com.example.managementsystem;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
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
        loadContent("/com/example/managementsystem/admin_home.fxml");
    }

    @FXML
    private void showVehicleManagement() {
        loadContent("/com/example/managementsystem/admin_vehicles.fxml");
    }

    @FXML
    private void showCustomerManagement() {
        loadContent("/com/example/managementsystem/admin_customers.fxml");
    }

    @FXML
    private void showUserManagement() {
        loadContent("/com/example/managementsystem/admin_users.fxml");
    }

    @FXML
    private void showReports() {
        loadContent("/com/example/managementsystem/admin_reports.fxml");
    }

    @FXML
    private void handleLogout() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/example/managementsystem/login.fxml")));
            contentPane.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadContent(String fxmlPath) {
        try {
            Parent content = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            contentPane.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
