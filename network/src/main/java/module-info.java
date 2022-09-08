module com.geekbrains.network {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens com.geekbrains.network to javafx.fxml;
    exports com.geekbrains.network;
}