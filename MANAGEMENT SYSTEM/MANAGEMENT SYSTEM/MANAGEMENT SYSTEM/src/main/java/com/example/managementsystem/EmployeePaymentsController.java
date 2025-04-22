package com.example.managementsystem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EmployeePaymentsController {
    @FXML private TableView<Payment> paymentTable;
    @FXML private TableColumn<Payment, Integer> idColumn;
    @FXML private TableColumn<Payment, Integer> bookingIdColumn;
    @FXML private TableColumn<Payment, String> customerColumn;
    @FXML private TableColumn<Payment, Double> amountColumn;
    @FXML private TableColumn<Payment, String> dateColumn;
    @FXML private TableColumn<Payment, String> methodColumn;
    @FXML private TableColumn<Payment, String> statusColumn;

    @FXML private TextField searchField;
    @FXML private Pane paymentForm;
    @FXML private Label bookingIdLabel;
    @FXML private Label customerNameLabel;
    @FXML private Label amountDueLabel;
    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private TextField amountPaidField;

    private ObservableList<Payment> paymentList = FXCollections.observableArrayList();
    private int currentRentalId = -1;
    private double currentAmountDue = 0.0;

    @FXML
    public void initialize() {

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        bookingIdColumn.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        customerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
        methodColumn.setCellValueFactory(new PropertyValueFactory<>("method"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));


        paymentMethodCombo.getItems().addAll("Cash", "Credit Card", "Bank Transfer", "Mobile Payment");
        loadPayments();
    }

    private void loadPayments() {
        paymentList.clear();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT p.*, c.full_name " +
                             "FROM payments p " +
                             "JOIN rentals r ON p.rental_id = r.rental_id " +
                             "JOIN customers c ON r.customer_id = c.customer_id " +
                             "ORDER BY p.payment_date DESC")) {

            while (rs.next()) {
                Payment payment = new Payment(
                        rs.getInt("payment_id"),
                        rs.getInt("rental_id"),
                        rs.getString("full_name"),
                        rs.getDouble("amount"),
                        rs.getTimestamp("payment_date").toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        rs.getString("payment_method"),
                        rs.getString("status")
                );
                paymentList.add(payment);
            }

            paymentTable.setItems(paymentList);
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load payments: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim().toLowerCase();

        if (searchTerm.isEmpty()) {
            paymentTable.setItems(paymentList);
            return;
        }

        ObservableList<Payment> filteredList = FXCollections.observableArrayList();
        for (Payment payment : paymentList) {
            if (payment.getCustomerName().toLowerCase().contains(searchTerm) ||
                    payment.getMethod().toLowerCase().contains(searchTerm) ||
                    payment.getStatus().toLowerCase().contains(searchTerm)) {
                filteredList.add(payment);
            }
        }

        paymentTable.setItems(filteredList);
    }

    @FXML
    private void handleViewHistory() {
        searchField.clear();
        paymentTable.setItems(paymentList);
    }

    @FXML
    private void handleProcessPayment() {

        Rental selectedRental = getSelectedRental();
        if (selectedRental == null) {
            showAlert("Selection Required", "Please select a booking from the rentals table first");
            return;
        }

        if (isRentalPaid(selectedRental.getRentalId())) {
            showAlert("Payment Exists", "This booking already has a completed payment");
            return;
        }


        currentRentalId = selectedRental.getRentalId();
        currentAmountDue = selectedRental.getTotalAmount();

        bookingIdLabel.setText(String.valueOf(selectedRental.getRentalId()));
        customerNameLabel.setText(selectedRental.getCustomerName());
        amountDueLabel.setText(String.format("$%.2f", selectedRental.getTotalAmount()));
        amountPaidField.setText(String.format("%.2f", selectedRental.getTotalAmount()));

        paymentForm.setVisible(true);
    }

    private Rental getSelectedRental() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT r.*, c.full_name " +
                             "FROM rentals r " +
                             "JOIN customers c ON r.customer_id = c.customer_id " +
                             "WHERE r.status = 'Active' LIMIT 1")) {

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Rental(
                        rs.getInt("rental_id"),
                        rs.getInt("customer_id"),
                        rs.getInt("vehicle_id"),
                        rs.getTimestamp("rental_date").toLocalDateTime(),
                        rs.getTimestamp("return_date") != null ? rs.getTimestamp("return_date").toLocalDateTime() : null,
                        rs.getDouble("total_amount"),
                        rs.getString("status"),
                        rs.getString("full_name")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isRentalPaid(int rentalId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM payments WHERE rental_id = ? AND status = 'Completed'")) {

            stmt.setInt(1, rentalId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @FXML
    private void handleConfirmPayment() {
        if (paymentMethodCombo.getValue() == null || paymentMethodCombo.getValue().isEmpty()) {
            showAlert("Validation Error", "Please select a payment method");
            return;
        }

        try {
            double amountPaid = Double.parseDouble(amountPaidField.getText());
            if (amountPaid <= 0) {
                showAlert("Validation Error", "Amount paid must be positive");
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);

                try {

                    String sql = "INSERT INTO payments (rental_id, amount, payment_date, payment_method, status) " +
                            "VALUES (?, ?, NOW(), ?, ?)";
                    PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    stmt.setInt(1, currentRentalId);
                    stmt.setDouble(2, amountPaid);
                    stmt.setString(3, paymentMethodCombo.getValue());
                    stmt.setString(4, "Completed");

                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        throw new SQLException("Creating payment failed, no rows affected.");
                    }


                    if (amountPaid >= currentAmountDue) {
                        PreparedStatement updateStmt = conn.prepareStatement(
                                "UPDATE rentals SET status = 'Completed' WHERE rental_id = ?");
                        updateStmt.setInt(1, currentRentalId);
                        updateStmt.executeUpdate();
                    }


                    conn.commit();

                    // Refresh payment list
                    loadPayments();

                    paymentForm.setVisible(false);

                    showAlert("Success", "Payment processed successfully");

                } catch (SQLException e) {

                    conn.rollback();
                    showAlert("Database Error", "Failed to process payment: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Please enter a valid amount");
        } catch (SQLException e) {
            showAlert("Database Error", "Error processing payment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancelPayment() {
        paymentForm.setVisible(false);
        currentRentalId = -1;
        currentAmountDue = 0.0;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    public static class Rental {
        private int rentalId;
        private int customerId;
        private int vehicleId;
        private LocalDateTime rentalDate;
        private LocalDateTime returnDate;
        private double totalAmount;
        private String status;
        private String customerName;

        public Rental(int rentalId, int customerId, int vehicleId, LocalDateTime rentalDate,
                      LocalDateTime returnDate, double totalAmount, String status, String customerName) {
            this.rentalId = rentalId;
            this.customerId = customerId;
            this.vehicleId = vehicleId;
            this.rentalDate = rentalDate;
            this.returnDate = returnDate;
            this.totalAmount = totalAmount;
            this.status = status;
            this.customerName = customerName;
        }


        public int getRentalId() { return rentalId; }
        public int getCustomerId() { return customerId; }
        public int getVehicleId() { return vehicleId; }
        public LocalDateTime getRentalDate() { return rentalDate; }
        public LocalDateTime getReturnDate() { return returnDate; }
        public double getTotalAmount() { return totalAmount; }
        public String getStatus() { return status; }
        public String getCustomerName() { return customerName; }
    }
}