module com.geekbrains.chatclient {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires com.almasb.fxgl.all;
    requires com.geekbrains.network;
    requires com.geekbrains.chatlibrary;
    requires java.desktop;

    opens com.geekbrains.chatclient to javafx.fxml;
    exports com.geekbrains.chatclient;
}