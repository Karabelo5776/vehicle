package com.example.managementsystem;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.*;
import javafx.scene.control.Alert;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.time.Month;

public class AdminReportsController {
    @FXML private Pane chartPane;
    @FXML private BarChart<String, Number> revenueChart;
    @FXML private PieChart utilizationChart;
    @FXML private LineChart<String, Number> activityChart;

    @FXML
    public void initialize() {
        // Initialize all charts as hidden
        revenueChart.setVisible(false);
        utilizationChart.setVisible(false);
        activityChart.setVisible(false);
    }

    @FXML
    private void showRevenueReport() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT MONTH(payment_date) as month, SUM(amount) as total " +
                    "FROM payments " +
                    "WHERE YEAR(payment_date) = YEAR(CURRENT_DATE()) " +
                    "GROUP BY MONTH(payment_date) " +
                    "ORDER BY MONTH(payment_date)";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            revenueChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Monthly Revenue");

            while (rs.next()) {
                int monthNumber = rs.getInt("month");
                double total = rs.getDouble("total");
                String monthName = Month.of(monthNumber).toString();
                series.getData().add(new XYChart.Data<>(monthName, total));
            }

            revenueChart.getData().add(series);
            revenueChart.setVisible(true);
            utilizationChart.setVisible(false);
            activityChart.setVisible(false);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Could not load revenue data", e.getMessage());
        }
    }

    @FXML
    private void showUtilizationReport() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT v.brand, v.model, COUNT(r.rental_id) as rental_count " +
                    "FROM vehicles v " +
                    "LEFT JOIN rentals r ON v.vehicle_id = r.vehicle_id " +
                    "GROUP BY v.vehicle_id, v.brand, v.model";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            utilizationChart.getData().clear();

            while (rs.next()) {
                String vehicleName = rs.getString("brand") + " " + rs.getString("model");
                int rentalCount = rs.getInt("rental_count");
                utilizationChart.getData().add(new PieChart.Data(vehicleName, rentalCount));
            }

            utilizationChart.setVisible(true);
            revenueChart.setVisible(false);
            activityChart.setVisible(false);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Could not load utilization data", e.getMessage());
        }
    }

    @FXML
    private void showActivityReport() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT MONTH(r.rental_date) as month, COUNT(r.rental_id) as rental_count " +
                    "FROM rentals r " +
                    "WHERE YEAR(r.rental_date) = YEAR(CURRENT_DATE()) " +
                    "GROUP BY MONTH(r.rental_date) " +
                    "ORDER BY MONTH(r.rental_date)";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            activityChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Monthly Rentals");

            while (rs.next()) {
                int monthNumber = rs.getInt("month");
                int rentalCount = rs.getInt("rental_count");
                String monthName = Month.of(monthNumber).toString();
                series.getData().add(new XYChart.Data<>(monthName, rentalCount));
            }

            activityChart.getData().add(series);
            activityChart.setVisible(true);
            revenueChart.setVisible(false);
            utilizationChart.setVisible(false);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Could not load activity data", e.getMessage());
        }
    }

    @FXML
    private void handleExportPDF() {
        try {

            Chart chartToExport = null;
            if (revenueChart.isVisible()) {
                chartToExport = revenueChart;
            } else if (utilizationChart.isVisible()) {
                chartToExport = utilizationChart;
            } else if (activityChart.isVisible()) {
                chartToExport = activityChart;
            }

            if (chartToExport != null) {

                WritableImage image = chartToExport.snapshot(new SnapshotParameters(), null);
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);


                PDDocument document = new PDDocument();
                PDPage page = new PDPage();
                document.addPage(page);

                PDImageXObject pdImage = LosslessFactory.createFromImage(document, bufferedImage);
                PDPageContentStream contentStream = new PDPageContentStream(document, page);


                float scale = 0.5f;
                contentStream.drawImage(pdImage, 50, 400, pdImage.getWidth() * scale, pdImage.getHeight() * scale);
                contentStream.close();


                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save PDF Report");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
                File file = fileChooser.showSaveDialog(chartPane.getScene().getWindow());

                if (file != null) {
                    document.save(file);
                    showAlert("Success", "PDF Exported", "Report saved to: " + file.getAbsolutePath());
                }

                document.close();
            } else {
                showAlert("Warning", "No Chart Selected", "Please display a chart before exporting.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Export Failed", "An error occurred while exporting PDF:\n" + e.getMessage());
        }
    }

    private void showAlert(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
