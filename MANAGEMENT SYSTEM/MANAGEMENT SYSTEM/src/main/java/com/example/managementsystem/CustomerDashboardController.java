package com.example.managementsystem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;

public class CustomerDashboardController {
    @FXML private Label welcomeLabel;
    @FXML private StackPane contentPane;
    @FXML private TableView<Vehicle> vehicleTable;
    @FXML private TableColumn<Vehicle, Integer> idColumn;
    @FXML private TableColumn<Vehicle, String> brandColumn;
    @FXML private TableColumn<Vehicle, String> modelColumn;
    @FXML private TableColumn<Vehicle, String> categoryColumn;
    @FXML private TableColumn<Vehicle, Double> priceColumn;
    @FXML private TableColumn<Vehicle, String> statusColumn;

    @FXML private TableView<Booking> bookingTable;

    private String currentUsername;
    private ObservableList<Vehicle> vehicleList = FXCollections.observableArrayList();
    private int customerId;

    public void setCurrentUser(String username) {
        this.currentUsername = username;
        welcomeLabel.setText("Welcome, " + username);
        try {
            this.customerId = getCustomerId(username);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brand"));
        modelColumn.setCellValueFactory(new PropertyValueFactory<>("model"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("pricePerDay"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        loadAvailableVehicles();
    }

    private void loadAvailableVehicles() {
        vehicleList.clear();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM vehicles WHERE status = 'Available'")) {

            while (rs.next()) {
                Vehicle vehicle = new Vehicle(
                        rs.getInt("vehicle_id"),
                        rs.getString("brand"),
                        rs.getString("model"),
                        rs.getString("category"),
                        rs.getInt("year"),
                        rs.getDouble("price_per_day"),
                        rs.getString("status")
                );
                vehicleList.add(vehicle);
            }
            vehicleTable.setItems(vehicleList);
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load vehicles: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBookVehicle() {
        Vehicle selectedVehicle = vehicleTable.getSelectionModel().getSelectedItem();

        if (selectedVehicle == null) {
            showAlert("Selection Required", "Please select a vehicle to book");
            return;
        }

        Dialog<BookingDetails> dialog = new Dialog<>();
        dialog.setTitle("Book Vehicle");
        dialog.setHeaderText("Enter booking details for " + selectedVehicle.getBrand() + " " + selectedVehicle.getModel());

        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setValue(LocalDate.now());
        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setValue(LocalDate.now().plusDays(1));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Start Date:"), 0, 0);
        grid.add(startDatePicker, 1, 0);
        grid.add(new Label("End Date:"), 0, 1);
        grid.add(endDatePicker, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new BookingDetails(
                        selectedVehicle.getId(),
                        startDatePicker.getValue(),
                        endDatePicker.getValue()
                );
            }
            return null;
        });

        Optional<BookingDetails> result = dialog.showAndWait();
        result.ifPresent(bookingDetails -> {
            try {
                createBooking(bookingDetails);
            } catch (SQLException e) {
                showAlert("Database Error", "Failed to create booking: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void createBooking(BookingDetails bookingDetails) throws SQLException {
        // Check if the customerId is valid before attempting to create a booking
        if (customerId < 0) {
            showAlert("Error", "Invalid customer ID. Please check your account details.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            long days = bookingDetails.endDate().toEpochDay() - bookingDetails.startDate().toEpochDay();
            double pricePerDay = getVehiclePrice(bookingDetails.vehicleId());
            double totalAmount = days * pricePerDay;

            String sql = "INSERT INTO rentals (customer_id, vehicle_id, rental_date, return_date, total_amount, status) " +
                    "VALUES (?, ?, ?, ?, ?, 'Pending')";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, customerId);
            stmt.setInt(2, bookingDetails.vehicleId());
            stmt.setTimestamp(3, Timestamp.valueOf(bookingDetails.startDate().atStartOfDay()));
            stmt.setTimestamp(4, Timestamp.valueOf(bookingDetails.endDate().atStartOfDay()));
            stmt.setDouble(5, totalAmount);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {

                PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE vehicles SET status = 'Reserved' WHERE vehicle_id = ?");
                updateStmt.setInt(1, bookingDetails.vehicleId());
                updateStmt.executeUpdate();

                showBookingConfirmation(bookingDetails, totalAmount);
                loadAvailableVehicles();
            }
        }
    }

    private void showBookingConfirmation(BookingDetails bookingDetails, double totalAmount) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Booking Confirmation");
        alert.setHeaderText("Your booking has been created!");

        Vehicle vehicle = vehicleList.stream()
                .filter(v -> v.getId() == bookingDetails.vehicleId())
                .findFirst()
                .orElse(null);

        String content = String.format(
                "Vehicle: %s %s\n" +
                        "Rental Period: %s to %s\n" +
                        "Total Amount: $%.2f\n\n" +
                        "Please proceed to payment to confirm your booking.",
                vehicle != null ? vehicle.getBrand() : "",
                vehicle != null ? vehicle.getModel() : "",
                bookingDetails.startDate(),
                bookingDetails.endDate(),
                totalAmount
        );

        alert.setContentText(content);
        alert.showAndWait();
    }

    private int getCustomerId(String username) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT customer_id FROM customers WHERE email = ?")) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("customer_id") : -1;
        }
    }

    private double getVehiclePrice(int vehicleId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT price_per_day FROM vehicles WHERE vehicle_id = ?")) {

            stmt.setInt(1, vehicleId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getDouble("price_per_day") : 0.0;
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout(ActionEvent event) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/managementsystem/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadAvailableVehicles(); // Refresh vehicle data
    }

    @FXML
    private void handleViewBooking() {
        Booking selectedBooking = bookingTable.getSelectionModel().getSelectedItem();

        if (selectedBooking == null) {
            showAlert("Selection Required", "Please select a booking to view its details.");
            return;
        }

        String content = String.format("Booking ID: %d\nVehicle: %s\nStart Date: %s\nEnd Date: %s\nAmount: $%.2f\nStatus: %s",
                selectedBooking.getId(),
                selectedBooking.getVehicle(),
                selectedBooking.getStartDate(),
                selectedBooking.getEndDate(),
                selectedBooking.getAmount(),
                selectedBooking.getStatus());

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Booking Details");
        alert.setHeaderText("Details for Booking ID: " + selectedBooking.getId());
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleCancelBooking() {
        Booking selectedBooking = bookingTable.getSelectionModel().getSelectedItem();

        if (selectedBooking == null) {
            showAlert("Selection Required", "Please select a booking to cancel.");
            return;
        }

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Cancel Booking");
        confirmationAlert.setHeaderText("Are you sure you want to cancel this booking?");
        confirmationAlert.setContentText("Booking ID: " + selectedBooking.getId());

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                cancelBooking(selectedBooking.getId());
                showAlert("Success", "Booking canceled successfully.");
                loadBookings();
            } catch (SQLException e) {
                showAlert("Database Error", "Failed to cancel booking: " + e.getMessage());
            }
        }
    }

    private void loadBookings() {
        ObservableList<Booking> bookingList = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM rentals WHERE customer_id = ?")) {
            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Booking booking = new Booking(
                        rs.getInt("rental_id"),
                        rs.getString("vehicle"),
                        rs.getTimestamp("rental_date").toLocalDateTime().toLocalDate(),
                        rs.getTimestamp("return_date").toLocalDateTime().toLocalDate(),
                        rs.getDouble("total_amount"),
                        rs.getString("status")
                );
                bookingList.add(booking);
            }
            bookingTable.setItems(bookingList);
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load bookings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cancelBooking(int bookingId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE rentals SET status = 'Cancelled' WHERE rental_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, bookingId);
                stmt.executeUpdate();
            }

            String updateVehicleSql = "UPDATE vehicles SET status = 'Available' WHERE vehicle_id = " +
                    "(SELECT vehicle_id FROM rentals WHERE rental_id = ?)";
            try (PreparedStatement updateVehicleStmt = conn.prepareStatement(updateVehicleSql)) {
                updateVehicleStmt.setInt(1, bookingId);
                updateVehicleStmt.executeUpdate();
            }
        }
    }
    @FXML
    private void handleAddNewCustomer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("customer_dialog.fxml"));
            GridPane page = loader.load();


            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add New Customer");
            dialogStage.setScene(new Scene(page));

            CustomerDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);


            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                Customer newCustomer = controller.getCustomer();
                saveCustomerToDatabase(newCustomer);
                showAlert("Success", "Customer registered successfully!");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load dialog: " + e.getMessage());
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
            showAlert("Database Error", "Could not save customer: " + e.getMessage());
        }
    }

    public void handleRefreshBookings(ActionEvent actionEvent) {
    }


    private record BookingDetails(int vehicleId, LocalDate startDate, LocalDate endDate) {}
}