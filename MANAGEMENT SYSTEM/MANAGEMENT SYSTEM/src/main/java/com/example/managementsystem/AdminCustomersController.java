package com.example.managementsystem;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.*;
import java.util.Objects;

public class AdminCustomersController {

    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, Integer> idColumn;
    @FXML private TableColumn<Customer, String> nameColumn;
    @FXML private TableColumn<Customer, String> emailColumn;
    @FXML private TableColumn<Customer, String> phoneColumn;
    @FXML private TableColumn<Customer, String> licenseColumn;
    @FXML private TableColumn<Customer, Integer> rentalsColumn;
    @FXML private TextField searchField;

    private final ObservableList<Customer> customerData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (!validateFXMLComponents()) return;

        setupTableColumns();
        loadCustomerData();
    }

    private boolean validateFXMLComponents() {
        if (customerTable == null || idColumn == null || nameColumn == null ||
                emailColumn == null || phoneColumn == null ||
                licenseColumn == null || rentalsColumn == null) {
            showAlert("Initialization Error", "Some UI components failed to load.");
            return false;
        }
        return true;
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(cell -> cell.getValue().idProperty().asObject());
        nameColumn.setCellValueFactory(cell -> cell.getValue().nameProperty());
        emailColumn.setCellValueFactory(cell -> cell.getValue().emailProperty());
        phoneColumn.setCellValueFactory(cell -> cell.getValue().phoneProperty());
        licenseColumn.setCellValueFactory(cell -> cell.getValue().licenseProperty());
        rentalsColumn.setCellValueFactory(cell -> cell.getValue().rentalCountProperty().asObject());
    }

    private void loadCustomerData() {
        customerData.clear();
        String query = "SELECT c.*, COUNT(r.rental_id) AS rental_count " +
                "FROM customers c LEFT JOIN rentals r ON c.customer_id = r.customer_id " +
                "GROUP BY c.customer_id";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                customerData.add(new Customer(
                        rs.getInt("customer_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("license_number"),
                        rs.getInt("rental_count")
                ));
            }
            customerTable.setItems(customerData);
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load customers: " + e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().toLowerCase();
        if (searchTerm.isEmpty()) {
            customerTable.setItems(customerData);
            return;
        }

        ObservableList<Customer> filtered = FXCollections.observableArrayList();
        for (Customer c : customerData) {
            if (c.getName().toLowerCase().contains(searchTerm) ||
                    c.getEmail().toLowerCase().contains(searchTerm) ||
                    c.getLicense().toLowerCase().contains(searchTerm)) {
                filtered.add(c);
            }
        }
        customerTable.setItems(filtered);
    }

    @FXML
    private void handleAddCustomer() {
        Customer newCustomer = showCustomerDialog(new Customer(0, "", "", "", "", 0));
        if (newCustomer != null) {
            saveCustomerToDatabase(newCustomer);
            loadCustomerData();
        }
    }

    @FXML
    private void handleEditCustomer() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a customer to edit.");
            return;
        }

        Customer updatedCustomer = showCustomerDialog(selected);
        if (updatedCustomer != null) {
            updateCustomerInDatabase(updatedCustomer);
            loadCustomerData();
        }
    }

    @FXML
    private void handleDeleteCustomer() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a customer to delete.");
            return;
        }

        String sql = "DELETE FROM customers WHERE customer_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, selected.getId());

            if (stmt.executeUpdate() > 0) {
                customerData.remove(selected);
                showAlert("Deleted", "Customer deleted successfully.");
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Could not delete customer: " + e.getMessage());
        }
    }

    private Customer showCustomerDialog(Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/managementsystem/CustomerDialog.fxml"));
            GridPane pane = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(customer.getId() == 0 ? "Add Customer" : "Edit Customer");
            dialogStage.setScene(new Scene(pane));

            CustomerDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setCustomer(customer);

            dialogStage.showAndWait();

            return controller.isSaveClicked() ? controller.getCustomer() : null;
        } catch (IOException e) {
            showAlert("Dialog Error", "Could not open customer dialog: " + e.getMessage());
            return null;
        }
    }

    private void saveCustomerToDatabase(Customer customer) {
        String sql = "INSERT INTO customers (full_name, email, phone, license_number) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, customer.getName());
            stmt.setString(2, customer.getEmail());
            stmt.setString(3, customer.getPhone());
            stmt.setString(4, customer.getLicense());

            if (stmt.executeUpdate() == 0) {
                throw new SQLException("Creating customer failed.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    customer.setId(keys.getInt(1));
                }
            }

            showAlert("Success", "Customer added successfully.");
        } catch (SQLException e) {
            showAlert("Database Error", "Could not save customer: " + e.getMessage());
        }
    }

    private void updateCustomerInDatabase(Customer customer) {
        String sql = "UPDATE customers SET full_name = ?, email = ?, phone = ?, license_number = ? WHERE customer_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, customer.getName());
            stmt.setString(2, customer.getEmail());
            stmt.setString(3, customer.getPhone());
            stmt.setString(4, customer.getLicense());
            stmt.setInt(5, customer.getId());

            if (stmt.executeUpdate() > 0) {
                showAlert("Updated", "Customer updated successfully.");
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Could not update customer: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackButton() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/example/managementsystem/admin_dashboard.fxml")));
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert("Navigation Error", "Could not return to dashboard: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
