package com.example.managementsystem;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import java.sql.*;

public class AdminCustomersController {
    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, Integer> idColumn;
    @FXML private TableColumn<Customer, String> nameColumn;
    @FXML private TableColumn<Customer, String> emailColumn;
    @FXML private TableColumn<Customer, String> phoneColumn;
    @FXML private TableColumn<Customer, String> licenseColumn;
    @FXML private TableColumn<Customer, Integer> rentalsColumn;
    @FXML private TextField searchField;

    private ObservableList<Customer> customerData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Set up the table columns
        idColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        emailColumn.setCellValueFactory(cellData -> cellData.getValue().emailProperty());
        phoneColumn.setCellValueFactory(cellData -> cellData.getValue().phoneProperty());
        licenseColumn.setCellValueFactory(cellData -> cellData.getValue().licenseProperty());
        rentalsColumn.setCellValueFactory(cellData -> cellData.getValue().rentalCountProperty().asObject());

        // Load customer data
        loadCustomerData();
    }

    private void loadCustomerData() {
        customerData.clear();

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT c.*, COUNT(r.rental_id) as rental_count " +
                    "FROM customers c LEFT JOIN rentals r ON c.customer_id = r.customer_id " +
                    "GROUP BY c.customer_id";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

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
            e.printStackTrace();
            showAlert("Database Error", "Could not load customer data: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddCustomer() {
        // Implementation would show a form to add new customer
        showAlert("Info", "Add customer functionality would be implemented here");
    }

    @FXML
    private void handleEditCustomer() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Open edit dialog with selected customer
            showAlert("Info", "Editing customer: " + selected.getName());
        } else {
            showAlert("Error", "Please select a customer to edit");
        }
    }

    @FXML
    private void handleDeleteCustomer() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "DELETE FROM customers WHERE customer_id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, selected.getId());

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    customerData.remove(selected);
                    showAlert("Success", "Customer deleted successfully");
                }
            } catch (SQLException e) {
                showAlert("Database Error", "Could not delete customer: " + e.getMessage());
            }
        } else {
            showAlert("Error", "Please select a customer to delete");
        }
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().toLowerCase();
        if (searchTerm.isEmpty()) {
            customerTable.setItems(customerData);
            return;
        }

        ObservableList<Customer> filteredData = FXCollections.observableArrayList();
        for (Customer customer : customerData) {
            if (customer.getName().toLowerCase().contains(searchTerm) ||
                    customer.getEmail().toLowerCase().contains(searchTerm) ||
                    customer.getLicense().toLowerCase().contains(searchTerm)) {
                filteredData.add(customer);
            }
        }

        customerTable.setItems(filteredData);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}