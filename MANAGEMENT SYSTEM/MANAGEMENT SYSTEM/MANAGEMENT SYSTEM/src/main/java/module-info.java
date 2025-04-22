module com.example.managementsystem {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;
    requires javafx.swing;
    requires org.apache.pdfbox;


    opens com.example.managementsystem to javafx.fxml;
    exports com.example.managementsystem;
}