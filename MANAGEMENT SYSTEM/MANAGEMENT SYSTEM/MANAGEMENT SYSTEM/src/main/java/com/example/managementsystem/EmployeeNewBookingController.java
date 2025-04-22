package com.example.managementsystem;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.util.StringConverter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class EmployeeNewBookingController {

    @FXML private ComboBox<Customer> customerCombo;
    @FXML private ComboBox<Vehicle> vehicleCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Label priceLabel;

    private ObservableList<Customer> customerData = FXCollections.observableArrayList();
    private ObservableList<Vehicle> vehicleData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(1));

        loadCustomers();
        loadAvailableVehicles();

        customerCombo.setCellFactory(param -> new ListCell<Customer>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getName() + " (" + item.getLicense() + ")");
            }
        });
        customerCombo.setConverter(new StringConverter<Customer>() {
            @Override public String toString(Customer customer) {
                return customer == null ? null : customer.getName() + " (" + customer.getLicense() + ")";
            }
            @Override public Customer fromString(String string) { return null; }
        });

        vehicleCombo.setCellFactory(param -> new ListCell<Vehicle>() {
            @Override
            protected void updateItem(Vehicle item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getBrand() + " " + item.getModel() + " - $" + item.getPricePerDay() + "/day");
            }
        });
        vehicleCombo.setConverter(new StringConverter<Vehicle>() {
            @Override public String toString(Vehicle vehicle) {
                return vehicle == null ? null : vehicle.getBrand() + " " + vehicle.getModel() + " - $" + vehicle.getPricePerDay() + "/day";
            }
            @Override public Vehicle fromString(String string) { return null; }
        });

        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> calculatePrice());
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> calculatePrice());
        vehicleCombo.valueProperty().addListener((obs, oldVal, newVal) -> calculatePrice());
    }

    private void loadCustomers() {
        customerData.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM customers ORDER BY full_name";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                customerData.add(new Customer(
                        rs.getInt("customer_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("license_number"),
                        0
                ));
            }
            customerCombo.setItems(customerData);
        } catch (SQLException e) {
            showAlert("Database Error", "Could not load customers", e.getMessage());
        }
    }

    private void loadAvailableVehicles() {
        vehicleData.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM vehicles WHERE status = 'Available' ORDER BY brand, model";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                vehicleData.add(new Vehicle(
                        rs.getInt("vehicle_id"),
                        rs.getString("brand"),
                        rs.getString("model"),
                        rs.getString("category"),
                        rs.getInt("year"),
                        rs.getDouble("price_per_day"),
                        rs.getString("status")
                ));
            }
            vehicleCombo.setItems(vehicleData);
        } catch (SQLException e) {
            showAlert("Database Error", "Could not load available vehicles", e.getMessage());
        }
    }

    private void calculatePrice() {
        if (vehicleCombo.getValue() == null || startDatePicker.getValue() == null || endDatePicker.getValue() == null) return;
        if (startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
            priceLabel.setText("Invalid dates");
            return;
        }
        long days = ChronoUnit.DAYS.between(startDatePicker.getValue(), endDatePicker.getValue());
        priceLabel.setText(String.format("$%.2f", days * vehicleCombo.getValue().getPricePerDay()));
    }

    @FXML
    private void handleNewCustomer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("customer_dialog.fxml"));
            GridPane page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("New Customer");
            dialogStage.setScene(new Scene(page));

            CustomerDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setCustomer(null);

            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                Customer newCustomer = controller.getCustomer();
                saveCustomerToDatabase(newCustomer);
                customerData.add(newCustomer);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load dialog", e.getMessage());
        }
    }

    private void saveCustomerToDatabase(Customer customer) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO customers (full_name, email, phone, license_number) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, customer.getName());
            stmt.setString(2, customer.getEmail());
            stmt.setString(3, customer.getPhone());
            stmt.setString(4, customer.getLicense());

            if (stmt.executeUpdate() > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    customer.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Could not save customer", e.getMessage());
        }
    }

    @FXML
    private void handleCreateBooking() {
        if (!validateInput()) return;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO rentals (customer_id, vehicle_id, rental_date, return_date, total_amount, status) VALUES (?, ?, ?, ?, ?, 'Active')";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, customerCombo.getValue().getId());
            stmt.setInt(2, vehicleCombo.getValue().getId());
            stmt.setDate(3, Date.valueOf(startDatePicker.getValue()));
            stmt.setDate(4, Date.valueOf(endDatePicker.getValue()));

            long days = ChronoUnit.DAYS.between(startDatePicker.getValue(), endDatePicker.getValue());
            stmt.setDouble(5, days * vehicleCombo.getValue().getPricePerDay());

            if (stmt.executeUpdate() > 0) {
                PreparedStatement vehicleStmt = conn.prepareStatement("UPDATE vehicles SET status = 'Rented' WHERE vehicle_id = ?");
                vehicleStmt.setInt(1, vehicleCombo.getValue().getId());
                vehicleStmt.executeUpdate();

                showAlert("Success", "Booking created successfully", "Booking reference: R" + System.currentTimeMillis());
                resetForm();
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Could not create booking", e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        ((VBox) priceLabel.getParent().getParent()).getScene().getWindow().hide();
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();
        if (customerCombo.getValue() == null) errors.append("Please select a customer\n");
        if (vehicleCombo.getValue() == null) errors.append("Please select a vehicle\n");
        if (startDatePicker.getValue() == null) errors.append("Please select a start date\n");
        if (endDatePicker.getValue() == null) errors.append("Please select an end date\n");
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null && startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
            errors.append("End date must be after start date\n");
        }
        if (errors.length() > 0) {
            showAlert("Validation Error", "Please correct the following:", errors.toString());
            return false;
        }
        return true;
    }

    private void resetForm() {
        customerCombo.getSelectionModel().clearSelection();
        vehicleCombo.getSelectionModel().clearSelection();
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(1));
        priceLabel.setText("$0.00");
        loadAvailableVehicles();
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
