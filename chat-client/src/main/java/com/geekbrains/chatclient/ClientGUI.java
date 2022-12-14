package com.geekbrains.chatclient;

import com.geekbrains.chatlibrary.Protocol;
import com.geekbrains.network.SocketThread;
import com.geekbrains.network.SocketThreadListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Arrays.*;

public class ClientGUI extends Application implements EventListener,
        Thread.UncaughtExceptionHandler, SocketThreadListener {

    private static final int WIDTH = 650;
    private static final int HEIGHT = 400;
    private static final String WINDOW_TITLE = "Chat Client";
    private static Stage stage;
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("[HH:mm] ");
    private SocketThread socketThread;
    private boolean shownIoErrors = false;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @FXML
    TextArea log;

    @FXML
    TextField tfIpAddress, tfPort, tfLogin, tfMessage, tfChangeNickname;

    @FXML
    HBox panelBottom, panelTopForChangeNick, panelLogin;

    @FXML
    GridPane panelTop;

    @FXML
    PasswordField tfPassword;

    @FXML
    ListView<String> usersList;

    @FXML
    Button btnLogin, btnDisconnect, btnSend, btnChange, btnSendPrivateMessage;

    @FXML
    ComboBox<String> dropDownUsersList;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        Thread.setDefaultUncaughtExceptionHandler(this);
        ScrollPane scrollUsers = new ScrollPane(usersList);
        scrollUsers.setPrefSize(100, 0);
        primaryStage.setResizable(false);
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("main.fxml")));
        primaryStage.setTitle(WINDOW_TITLE);
        primaryStage.setScene(new Scene(root, WIDTH, HEIGHT));
        primaryStage.show();
        primaryStage.setAlwaysOnTop(true);
        primaryStage.setOnCloseRequest(e -> System.exit(1));
    }

    private void setEmptyCellUserList() {
        usersList.setOnMouseClicked(event -> {
            int indexElement = usersList.getSelectionModel().getSelectedIndex();
            usersList.getSelectionModel().clearSelection(indexElement);
        });
    }

    private void showException(Thread thread, Throwable exception) {
        String message;
        StackTraceElement[] ste = exception.getStackTrace();
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

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        exception.printStackTrace();
        showException(thread, exception);
        System.exit(1);
    }

    private void putLog(String msg) {
        if ("".equals(msg)) return;
        Platform.runLater(() -> {
            log.appendText(msg + "\n");
            log.positionCaret(log.getLength());
        });
    }

    private void writingLogToFile(String message, String username) {
        File file = new File("chat-server/src/main/java/com/geekbrains/chatserver/logs/client_logs");
        String fileName = file.getPath() + "/history_[" + username + "].log";
        if (!file.isDirectory()) {
            file.mkdirs();
        }
        DateFormat DATE_FORMAT = new SimpleDateFormat("[yyyy-MM-dd] [HH:mm] ");
        String date = DATE_FORMAT.format(new Date());
        try (BufferedWriter writeLog = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(fileName, true), StandardCharsets.UTF_8))) {
            writeLog.write(String.format("%s%s: %s\n", date, username, message));
            writeLog.flush();
        } catch (IOException e) {
            if (!shownIoErrors) {
                shownIoErrors = true;
                showException(Thread.currentThread(), e);
            }
        }
    }

    @FXML
    private void sendMessage() {
        String message = tfMessage.getText();
        if (message == null) return;
        if (message.equals("") || message.trim().length() == 0) return;
        tfMessage.setText(null);
        tfMessage.requestFocus();
        socketThread.sendMessage(Protocol.getUserBroadcast(message));
        writingLogToFile(message, tfLogin.getText());
    }

    @FXML
    private void sendPrivateMessage() {
        String message = tfMessage.getText();
        if (message == null) return;
        if (message.equals("") || message.trim().length() == 0) return;
        tfMessage.setText(null);
        tfMessage.requestFocus();
        String item = String.valueOf(dropDownUsersList.getValue());
        if (item.equals("Everybody")) return;
        socketThread.sendMessage(Protocol.getPrivateUserBroadcast(item, message));
    }

    @FXML
    public void connect() {
        try {
            Socket socket = new Socket(tfIpAddress.getText().trim(), Integer.parseInt(tfPort.getText().trim()));
            socketThread = new SocketThread(this, "Client", socket, executorService);
        } catch (IOException e) {
            showException(Thread.currentThread(), e);
        }
    }

    @FXML
    private void disconnect() {
        socketThread.close();
    }

    @Override
    public void onSocketStart(SocketThread thread, Socket socket) {
        putLog(String.format("%s %s", DATE_FORMAT.format(new Date()), "Socket started"));
    }

    @Override
    public void onSocketStop(SocketThread thread) {
        panelLogin.setVisible(true);
        panelBottom.setVisible(false);
        panelTopForChangeNick.setVisible(false);
        putLog(String.format("%s %s", DATE_FORMAT.format(new Date()), "Socket stopped"));
        Platform.runLater(() -> {
            stage.setTitle(WINDOW_TITLE);
            ObservableList<String> clients = FXCollections.observableArrayList("");
            usersList.setItems(clients);
        });
    }

    @FXML
    public void changeNickname() {
        socketThread.sendMessage(Protocol.getChangeNickname(tfChangeNickname.getText().trim()));
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        putLog(String.format("%s %s", DATE_FORMAT.format(new Date()), "Socket is ready"));
        Platform.runLater(this::setEmptyCellUserList);
        socketThread.sendMessage(Protocol.getAuthRequest(tfLogin.getText().trim(), tfPassword.getText()));
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String message) {
        handleMessage(message);
    }

    private void handleMessage(String message) {
        String[] arrayUserData = message.split(Protocol.DELIMITER);
        String msgType = arrayUserData[0];
        switch (msgType) {
            case Protocol.AUTH_ACCEPT -> {
                Platform.runLater(() -> stage.setTitle(WINDOW_TITLE + " nickname: " + arrayUserData[1]));
                panelLogin.setVisible(false);
                panelBottom.setVisible(true);
                panelTopForChangeNick.setVisible(true);
            }
            case Protocol.AUTH_DENIED -> putLog("Authorization failed");
            case Protocol.MSG_FORMAT_ERROR -> {
                putLog(message);
                socketThread.close();
            }
            case Protocol.TYPE_BROADCAST ->
                    putLog(String.format("%s %s: %s", DATE_FORMAT.format(Long.parseLong(arrayUserData[1])), arrayUserData[2], arrayUserData[3]));
            case Protocol.USER_LIST -> {
                String users = message.substring(Protocol.USER_LIST.length() + Protocol.DELIMITER.length());
                String[] usersArray = users.split(Protocol.DELIMITER);
                sort(usersArray);
                Platform.runLater(() -> {
                    ObservableList<String> clients = FXCollections.observableArrayList(usersArray);
                    usersList.setItems(clients);
                    dropDownUsersList.setItems(clients);
                    dropDownUsersList.getSelectionModel().selectFirst();
                });
            }
            case Protocol.LAST_MESSAGES -> putLog(arrayUserData[1]);
            default -> throw new RuntimeException("Unknown message type");
        }
    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {
        exception.printStackTrace();
    }
}