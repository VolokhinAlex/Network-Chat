module com.geekbrains.chatlibrary {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens com.geekbrains.chatlibrary to javafx.fxml;
    exports com.geekbrains.chatlibrary;
}