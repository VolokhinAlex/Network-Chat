package com.geekbrains.chatserver.gui;

import com.geekbrains.chatserver.core.ChatServer;
import com.geekbrains.chatserver.core.ChatServerListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.EventListener;
import java.util.Objects;

public class ServerGUI extends Application implements EventListener,
        Thread.UncaughtExceptionHandler, ChatServerListener {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 300;
    private final int port = 8189;

    private final ChatServer chatServer = new ChatServer(this);

    @FXML
    TextArea log;

    @FXML
    HBox panelTop;

    @FXML
    Button btnStart, btnStop;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(this);
        ScrollPane scrollLog = new ScrollPane(log);
        primaryStage.setAlwaysOnTop(true);
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/geekbrains/chatserver/main.fxml")));
        primaryStage.setTitle("Chat Server");
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(root, WIDTH, HEIGHT));
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> System.exit(1));
    }

    public void startServer() {
        chatServer.start(port);
    }

    public void stopServer() {
        chatServer.stop();
    }

    private void showException(Thread thread, Throwable exception) {
        String message;
        StackTraceElement[] ste = exception.getStackTrace();
        if (ste.length == 0) {
            message = "Empty StackTrace";
        } else {
            message = String.format("Exception in thread \"%s\" %s: %s\n\tat %s",
                    thread.getName(), exception.getClass().getCanonicalName(),
                    exception.getMessage(), ste[0]);
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
                alert.showAndWait();
                Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                stage.setAlwaysOnTop(true);
            });
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        exception.printStackTrace();
        showException(thread, exception);
        System.exit(1);
    }

    @Override
    public void onChatServerMessage(String msg) {
        if ("".equals(msg)) return;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                log.appendText(msg + "\n");
            }
        });
    }
}
