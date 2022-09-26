package com.geekbrains.chatserver.core;

import com.geekbrains.chatlibrary.Protocol;
import com.geekbrains.network.ServerSocketThread;
import com.geekbrains.network.ServerSocketThreadListener;
import com.geekbrains.network.SocketThread;
import com.geekbrains.network.SocketThreadListener;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.logging.*;

public class ChatServer implements ServerSocketThreadListener, SocketThreadListener {

    private ServerSocketThread server;
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("[HH:mm:ss] ");
    private Vector<SocketThread> users;

    private ChatServerListener listener;
    private ExecutorService executorService;
    private final int LAST_MESSAGE_COUNT = 100;
    private final Logger logger = Logger.getLogger(ChatServer.class.getName());

    public ChatServer(ChatServerListener listener) {
        this.listener = listener;
        users = new Vector<>();
    }

    public void start(int port) {
        if (server != null && server.isAlive()) {
            putLog("Server already started");
        } else {
            server = new ServerSocketThread(this, "server", port, 2000);
        }
    }

    public void stop() {
        if (server == null || !server.isAlive()) {
            putLog("Server is not running");
        } else {
            server.interrupt();
        }
    }

    public void putLog(String msg) {
        msg = DATE_FORMAT.format(System.currentTimeMillis()) +
                Thread.currentThread().getName() + ": " + msg;
        listener.onChatServerMessage(msg);
    }

    private void createLoggingEvents() {
        try {
            File file = new File("chat-server/src/main/java/com/geekbrains/chatserver/logs/server_logs");
            if (!file.isDirectory()) {
                file.mkdirs();
            }
            DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
            String date = DATE_FORMAT.format(new Date());
            Handler handler = new FileHandler(String.format("%s/%s_logs.log", file.getPath(), date), true);
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
            logger.setUseParentHandlers(false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onServerStarted(ServerSocketThread thread) {
        putLog("Server socket thread started");
        SqlClient.connect();
        createLoggingEvents();
        logger.log(Level.INFO, "Server socket thread started");
    }

    @Override
    public void onServerStopped(ServerSocketThread thread) {
        putLog("Server socket thread stopped");
        SqlClient.disconnect();
        for (SocketThread user : users) {
            user.close();
        }
        logger.log(Level.INFO, "Server socket thread stopped");
    }

    @Override
    public void onServerSocketCreated(ServerSocketThread thread, ServerSocket server) {
        putLog("Server socket created");
    }

    @Override
    public void onServerTimeout(ServerSocketThread thread, ServerSocket server) {
    }

    @Override
    public void onSocketAccepted(ServerSocketThread thread, ServerSocket server, Socket socket, ExecutorService executorService) {
        putLog("Client Connection");
        logger.log(Level.INFO, "Client Connection");
        String name = "SocketThread" + socket.getInetAddress() + ":" + socket.getPort();
        new ClientThread(this, name, socket, executorService);
        this.executorService = executorService;
    }

    @Override
    public void onServerException(ServerSocketThread thread, Throwable exception) {
        exception.printStackTrace();
        logger.log(Level.SEVERE, exception.getMessage());
    }

    @Override
    public synchronized void onSocketStart(SocketThread thread, Socket socket) {
        putLog("Socket created");
    }

    @Override
    public synchronized void onSocketStop(SocketThread thread) {
        ClientThread client = (ClientThread) thread;
        users.remove(thread);
        if (client.isAuthorized() && !client.isReconnecting()) {
            sendToAllAuthorizedClients(Protocol.getTypeBroadcast(
                    "Server", client.getNickname() + " disconnected"));
        }
        sendToAllAuthorizedClients(Protocol.getUserList(getUsers()));
    }

    @Override
    public synchronized void onSocketReady(SocketThread thread, Socket socket) {
        users.add(thread);
    }

    @Override
    public synchronized void onReceiveString(SocketThread thread, Socket socket, String msg) {
        ClientThread client = (ClientThread) thread;
        if (client.isAuthorized()) {
            handleAuthClientMessage(client, msg);
        } else {
            handleNonAuthClientMessage(client, msg);
        }
    }

    @Override
    public synchronized void onSocketException(SocketThread thread, Exception exception) {
        exception.printStackTrace();
        logger.log(Level.WARNING, exception.getMessage());
    }

    public void handleAuthClientMessage(ClientThread user, String message) {
        String[] msgArray = message.split(Protocol.DELIMITER);
        String msgType = msgArray[0];
        switch (msgType) {
            case Protocol.USER_BROADCAST -> {
                sendToAllAuthorizedClients(Protocol.getTypeBroadcast(user.getNickname(), msgArray[1]));
                SqlClient.savingUserMessages(SqlClient.getId(user.getLogin(), user.getPassword(), user.getNickname()),
                        SqlClient.getId(user.getLogin(), user.getPassword(), user.getNickname()), msgArray[1], System.currentTimeMillis() / 1000L);
                logger.log(Level.INFO, "Client send message");
            }
            case Protocol.CHANGE_NICKNAME -> changeNickname(user, msgArray);
            case Protocol.PRIVATE_USER_BROADCAST ->
                    sendPrivateMessageToAuthorizedClient(user, msgArray[1], msgArray[2]);
            default -> user.msgFormatError(message);
        }
    }

    public void handleNonAuthClientMessage(ClientThread user, String message) {
        String[] arrayUser = message.split(Protocol.DELIMITER);
        if (arrayUser.length != 3 || !arrayUser[0].equals(Protocol.AUTH_REQUEST)) {
            user.msgFormatError(message);
            return;
        }
        String login = arrayUser[1];
        String password = arrayUser[2];
        String nickname = SqlClient.getNickname(login, password);
        if (nickname == null) {
            putLog("Invalid credentials attempt for login = " + login);
            logger.log(Level.WARNING, "Invalid credentials attempt for login = " + login);
            user.authFail();
            return;
        } else {
            ClientThread oldUser = findClientByNickname(nickname);
            user.authAccept(nickname);
            user.getData(login, password);
            if (oldUser == null) {
                sendToAllAuthorizedClients(Protocol.getTypeBroadcast("Server", nickname + " connected"));
            } else {
                oldUser.reconnect();
                users.remove(oldUser);
            }
        }
        sendToAllAuthorizedClients(Protocol.getUserList(getUsers()));
        printLastMessage(user, LAST_MESSAGE_COUNT);
    }

    private void sendToAllAuthorizedClients(String msg) {
        for (SocketThread user : users) {
            ClientThread recipient = (ClientThread) user;
            if (!recipient.isAuthorized()) continue;
            recipient.sendMessage(msg);
        }
    }

    private void sendPrivateMessageToAuthorizedClient(ClientThread userFrom, String userTo, String msg) {
        if (userFrom.getNickname().equals(userTo)) {
            userFrom.sendPrivateMessage(userFrom.getNickname() + "'s Favorites", msg);
            return;
        }
        for (SocketThread user : users) {
            ClientThread client = (ClientThread) user;
            if (client.getNickname().equals(userTo)) {
                client.sendPrivateMessage("From " + userFrom.getNickname(), msg);
                userFrom.sendPrivateMessage("To " + userTo, msg);
                return;
            }
        }
        userFrom.sendPrivateMessage("Server", "Client " + userTo + " did not find");
    }

    public String getUsers() {
        StringBuilder stringBuilder = new StringBuilder();
        for (SocketThread user : users) {
            ClientThread client = (ClientThread) user;
            stringBuilder.append(client.getNickname()).append(Protocol.DELIMITER);
        }
        return stringBuilder.toString();
    }

    private synchronized ClientThread findClientByNickname(String nickname) {
        for (SocketThread socketThread : users) {
            ClientThread user = (ClientThread) socketThread;
            if (!user.isAuthorized()) continue;
            if (user.getNickname().equals(nickname)) {
                return user;
            }
        }
        return null;
    }

    private void changeNickname(ClientThread user, String[] msgArray) {
        if (!(SqlClient.changeNickname(user.getNickname(), msgArray[1]))) {
            String message = String.format("%s failed to changed nickname to %s!", user.getNickname(), msgArray[1]);
            user.msgFormatError(String.format("You failed to change your nickname to %s!", msgArray[1]));
            putLog(message);
            logger.log(Level.WARNING, message);
        } else {
            SqlClient.changeNickname(user.getNickname(), msgArray[1]);
            sendToAllAuthorizedClients(Protocol.getTypeBroadcast("Server", user.getNickname() + " changed nickname to " + msgArray[1]));
            SqlClient.savingUserMessages(0, 0, user.getNickname() + " changed nickname to " + msgArray[1], System.currentTimeMillis() / 1000L);
            user.setNickname(msgArray[1]);
            sendToAllAuthorizedClients(Protocol.getUserList(getUsers()));
            putLog(user.getNickname() + " changed nickname to " + msgArray[1]);
            logger.log(Level.INFO, user.getNickname() + " changed nickname to " + msgArray[1]);
        }
    }

    private void printLastMessage(SocketThread thread, int lastMessageCount) {
        String[] lastMessages = Objects.requireNonNull(SqlClient.showLastMessages(lastMessageCount)).split(SqlClient.DELIMITER);
        invertArray(lastMessages);
        for (String lastMessage : lastMessages) {
            thread.sendMessage(Protocol.getLastMessages(lastMessage));
        }
    }

    private <T> T[] invertArray(T[] array) {
        for (int i = 0; i < array.length / 2; i++) {
            T tmp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = tmp;
        }
        return array;
    }

}