package com.example.managementsystem;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import java.sql.*;
import java.time.LocalDate;

public class EmployeeManageBookingsController {
    @FXML private TableView<Rental> bookingTable;
    @FXML private TableColumn<Rental, Integer> idColumn;
    @FXML private TableColumn<Rental, String> customerColumn;
    @FXML private TableColumn<Rental, String> vehicleColumn;
    @FXML private TableColumn<Rental, String> startDateColumn;
    @FXML private TableColumn<Rental, String> endDateColumn;
    @FXML private TableColumn<Rental, Double> amountColumn;
    @FXML private TableColumn<Rental, String> statusColumn;
    @FXML private TextField searchField;

    private ObservableList<Rental> bookingData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        idColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        customerColumn.setCellValueFactory(cellData -> cellData.getValue().customerNameProperty());
        vehicleColumn.setCellValueFactory(cellData -> cellData.getValue().vehicleInfoProperty());
        startDateColumn.setCellValueFactory(cellData -> cellData.getValue().startDateProperty());
        endDateColumn.setCellValueFactory(cellData -> cellData.getValue().endDateProperty());
        amountColumn.setCellValueFactory(cellData -> cellData.getValue().amountProperty().asObject());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());


        loadBookingData();
    }

    private void loadBookingData() {
        bookingData.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT r.*, c.full_name as customer_name, CONCAT(v.brand, ' ', v.model) as vehicle_info " +
                    "FROM rentals r JOIN customers c ON r.customer_id = c.customer_id " +
                    "JOIN vehicles v ON r.vehicle_id = v.vehicle_id " +
                    "ORDER BY r.rental_date DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                bookingData.add(new Rental(
                        rs.getInt("rental_id"),
                        rs.getString("customer_name"),
                        rs.getString("vehicle_info"),
                        rs.getDate("rental_date").toString(),
                        rs.getDate("return_date").toString(),
                        rs.getDouble("total_amount"),
                        rs.getString("status")
                ));
            }
            bookingTable.setItems(bookingData);
        } catch (SQLException e) {
            showAlert("Database Error", "Could not load bookings", e.getMessage());
        }
    }

    @FXML
    private void handleMarkReturned() {
        Rental selected = bookingTable.getSelectionModel().getSelectedItem();
        if (selected != null && "Active".equals(selected.getStatus())) {
            try (Connection conn = DatabaseConnection.getConnection()) {

                String rentalSql = "UPDATE rentals SET status = 'Completed' WHERE rental_id = ?";
                PreparedStatement rentalStmt = conn.prepareStatement(rentalSql);
                rentalStmt.setInt(1, selected.getId());


                String vehicleSql = "UPDATE vehicles v JOIN rentals r ON v.vehicle_id = r.vehicle_id " +
                        "SET v.status = 'Available' WHERE r.rental_id = ?";
                PreparedStatement vehicleStmt = conn.prepareStatement(vehicleSql);
                vehicleStmt.setInt(1, selected.getId());

                conn.setAutoCommit(false);
                rentalStmt.executeUpdate();
                vehicleStmt.executeUpdate();
                conn.commit();

                selected.setStatus("Completed");
                bookingTable.refresh();
                showAlert("Success", "Booking marked as returned",
                        "Vehicle is now available for new bookings");
            } catch (SQLException e) {
                showAlert("Database Error", "Could not update booking", e.getMessage());
            }
        } else {
            showAlert("Error", "Invalid Selection", "Please select an active booking to mark as returned");
        }
    }

    @FXML
    private void handleCancelBooking() {
        Rental selected = bookingTable.getSelectionModel().getSelectedItem();
        if (selected != null && "Active".equals(selected.getStatus())) {
            try (Connection conn = DatabaseConnection.getConnection()) {

                String rentalSql = "UPDATE rentals SET status = 'Cancelled' WHERE rental_id = ?";
                PreparedStatement rentalStmt = conn.prepareStatement(rentalSql);
                rentalStmt.setInt(1, selected.getId());

                String vehicleSql = "UPDATE vehicles v JOIN rentals r ON v.vehicle_id = r.vehicle_id " +
                        "SET v.status = 'Available' WHERE r.rental_id = ?";
                PreparedStatement vehicleStmt = conn.prepareStatement(vehicleSql);
                vehicleStmt.setInt(1, selected.getId());

                conn.setAutoCommit(false);
                rentalStmt.executeUpdate();
                vehicleStmt.executeUpdate();
                conn.commit();

                selected.setStatus("Cancelled");
                bookingTable.refresh();
                showAlert("Success", "Booking cancelled",
                        "Vehicle is now available for new bookings");
            } catch (SQLException e) {
                showAlert("Database Error", "Could not cancel booking", e.getMessage());
            }
        } else {
            showAlert("Error", "Invalid Selection", "Please select an active booking to cancel");
        }
    }

    @FXML
    private void handleModifyBooking() {
        Rental selected = bookingTable.getSelectionModel().getSelectedItem();
        if (selected != null && "Active".equals(selected.getStatus())) {
            try {

                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("modify_booking_dialog.fxml"));
                GridPane page = loader.load();


                Stage dialogStage = new Stage();
                dialogStage.setTitle("Modify Booking");
                Scene scene = new Scene(page);
                dialogStage.setScene(scene);


                ModifyBookingController controller = loader.getController();
                controller.setDialogStage(dialogStage);
                controller.setRental(selected);


                dialogStage.showAndWait();

                if (controller.isSaveClicked()) {

                    loadBookingData();
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Could not load modification dialog", e.getMessage());
            }
        } else {
            showAlert("Error", "Invalid Selection", "Please select an active booking to modify");
        }
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().toLowerCase();
        if (searchTerm.isEmpty()) {
            bookingTable.setItems(bookingData);
            return;
        }

        ObservableList<Rental> filteredData = FXCollections.observableArrayList();
        for (Rental rental : bookingData) {
            if (rental.getCustomerName().toLowerCase().contains(searchTerm) ||
                    rental.getVehicleInfo().toLowerCase().contains(searchTerm) ||
                    rental.getStatus().toLowerCase().contains(searchTerm)) {
                filteredData.add(rental);
            }
        }
        bookingTable.setItems(filteredData);
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}