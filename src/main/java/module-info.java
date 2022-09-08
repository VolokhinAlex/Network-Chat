module com.geekbrains.onlinechat {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires com.almasb.fxgl.all;

    opens com.geekbrains.onlinechat to javafx.fxml;
    exports com.geekbrains.onlinechat;
}