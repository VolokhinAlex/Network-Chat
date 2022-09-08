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
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.Arrays.*;

public class ClientGUI extends Application implements EventListener,
        Thread.UncaughtExceptionHandler, SocketThreadListener{

    private static final int WIDTH = 650;
    private static final int HEIGHT = 400;
    private static final String WINDOW_TITLE = "Chat Client";
    private final CheckBox cbAlwaysOnTop = new CheckBox("Always on top");
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("[HH:mm] ");
    private boolean shownIoErrors = false;
    private SocketThread socketThread;

    @FXML
    TextArea log;

    @FXML
    TextField tfIpAddress, tfPort, tfLogin, tfMessage, tfChangeNickname;

    @FXML
    HBox panelTop, panelBottom, panelTopForChangeNick;

    @FXML
    PasswordField tfPassword;

    @FXML
    ListView<String> usersList;

    @FXML
    Button btnLogin, btnDisconnect, btnSend, btnChange;

    private ComboBox dropDownUsersList = new ComboBox<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(this);
        ScrollPane scrollLog = new ScrollPane(log);
        ScrollPane scrollUsers = new ScrollPane(usersList);
        scrollUsers.setPrefSize(100, 0);
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("main.fxml")));
        primaryStage.setTitle(WINDOW_TITLE);
        primaryStage.setScene(new Scene(root, WIDTH, HEIGHT));
        primaryStage.show();
        primaryStage.setAlwaysOnTop(true);
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

    /**
     * The method creates a new log file, in which after each message there is a record.
     * Created a file named LYearMonthDay.log (Example: L20220818.log)
     *
     * @throws IOException - file exceptions.
     */

    private void writingLogToFile(String msg, String username) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
            String date = simpleDateFormat.format(new Date());
            BufferedWriter writeLog = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("L" + date + ".log"), StandardCharsets.UTF_8));
            writeLog.write(username + ": " + msg);
            writeLog.flush();
        } catch (IOException e) {
            if (!shownIoErrors) {
                shownIoErrors = true;
                showException(Thread.currentThread(), e);
            }
        }
    }

    private void putLog(String msg) {
        if ("".equals(msg)) return;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                log.appendText(msg + "\n");
                log.positionCaret(log.getLength());
            }
        });
    }

    /**
     * Method for printing messages to the log.
     */

    @FXML
    private void sendMessage() {
        String message = tfMessage.getText();
        if (message.equals("") || message.trim().length() == 0) return;
        tfMessage.setText(null);
        tfMessage.requestFocus();
        socketThread.sendMessage(Protocol.getUserBroadcast(message));
    }

    @FXML
    public void connect() {
        try {
            Socket socket = new Socket(tfIpAddress.getText(), Integer.parseInt(tfPort.getText()));
            socketThread = new SocketThread(this, "Client", socket);
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
        putLog("Socket started");
    }

    @Override
    public void onSocketStop(SocketThread thread) {
        panelTop.setVisible(true);
        panelBottom.setVisible(false);
        panelTopForChangeNick.setVisible(false);
        putLog("Socket stopped");
        Platform.runLater(() -> {
            ObservableList<String> clients = FXCollections.observableArrayList("");
            usersList.setItems(clients);
        });
    }

    @FXML
    public void changeNickname() {
        socketThread.sendMessage(Protocol.getChangeNickname(tfChangeNickname.getText()));
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        putLog("Socket is ready");
        socketThread.sendMessage(Protocol.getAuthRequest(tfLogin.getText(), tfPassword.getText()));
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
                panelTop.setVisible(false);
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
                });
            }
            default -> throw new RuntimeException("Unknown message type");
        }
    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {
        exception.printStackTrace();
    }
}
