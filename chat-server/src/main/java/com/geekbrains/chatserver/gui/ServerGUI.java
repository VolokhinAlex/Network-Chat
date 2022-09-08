package com.geekbrains.chatserver.gui;

import com.geekbrains.chatserver.core.ChatServer;
import com.geekbrains.chatserver.core.ChatServerListener;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.EventListener;
import java.util.Objects;

public class ServerGUI extends Application implements EventListener,
        Thread.UncaughtExceptionHandler, ChatServerListener {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 300;

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
        ScrollPane scrollLog = new ScrollPane(log);
        primaryStage.setAlwaysOnTop(true);
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/geekbrains/chatserver/main.fxml")));
        primaryStage.setTitle("Chat Server");
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(root, WIDTH, HEIGHT));
        primaryStage.show();
    }

//    private ServerGUI() {
//        Thread.setDefaultUncaughtExceptionHandler(this);
//        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//        setBounds(POS_X, POS_Y, WIDTH, HEIGHT);
//        setResizable(false);
//        setTitle("Chat Server");
//        setAlwaysOnTop(true);
//        btnStart.addActionListener(this);
//        btnStop.addActionListener(this);
//        log.setEditable(false);
//        log.setLineWrap(true);
//        JScrollPane scrollLog = new JScrollPane(log);
//        panelTop.add(btnStart);
//        panelTop.add(btnStop);
//        add(panelTop, BorderLayout.NORTH);
//        add(scrollLog, BorderLayout.CENTER);
//        setVisible(true);
//    }

    public void startServer() {
        chatServer.start(8189);
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
            //JOptionPane.showMessageDialog(this, message, "Exception", JOptionPane.ERROR_MESSAGE);
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
        log.appendText(msg + "\n");
        //log.setCaretPosition(log.getDocument().getLength());
    }
}
