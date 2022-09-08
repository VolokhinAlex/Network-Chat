module com.geekbrains.chatserver {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires com.almasb.fxgl.all;
    requires com.geekbrains.network;
    requires com.geekbrains.chatlibrary;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.geekbrains.chatserver.gui to javafx.fxml;
    exports com.geekbrains.chatserver.gui;
}