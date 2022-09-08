package com.geekbrains.chatserver.core;

import com.geekbrains.chatlibrary.Protocol;
import com.geekbrains.network.ServerSocketThread;
import com.geekbrains.network.ServerSocketThreadListener;
import com.geekbrains.network.SocketThread;
import com.geekbrains.network.SocketThreadListener;
import javafx.event.ActionEvent;

import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Vector;

public class ChatServer implements ServerSocketThreadListener, SocketThreadListener {

    private ServerSocketThread server;
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("[HH:mm:ss] ");
    private Vector<SocketThread> users;

    private ChatServerListener listener;

    private int id;

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

    @Override
    public void onServerStarted(ServerSocketThread thread) {
        putLog("Server socket thread started");
        SqlClient.connect();
    }

    @Override
    public void onServerStopped(ServerSocketThread thread) {
        putLog("Server socket thread stopped");
        SqlClient.disconnect();
        for (SocketThread user : users) {
            user.close();
        }
    }

    @Override
    public void onServerSocketCreated(ServerSocketThread thread, ServerSocket server) {
        putLog("Server socket created");
    }

    @Override
    public void onServerTimeout(ServerSocketThread thread, ServerSocket server) {}

    @Override
    public void onSocketAccepted(ServerSocketThread thread, ServerSocket server, Socket socket) {
        putLog("Client Connection");
        String name = "SocketThread" + socket.getInetAddress() + ":" + socket.getPort();
        new ClientThread(this, name, socket);
    }

    @Override
    public void onServerException(ServerSocketThread thread, Throwable exception) {
        exception.printStackTrace();
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
    }

    public void handleAuthClientMessage(ClientThread user, String message) {
        String[] msgArray = message.split(Protocol.DELIMITER);
        String msgType = msgArray[0];
        switch (msgType) {
            case Protocol.USER_BROADCAST ->
                    sendToAllAuthorizedClients(Protocol.getTypeBroadcast(user.getNickname(), msgArray[1]));
            case Protocol.CHANGE_NICKNAME -> {
                SqlClient.changeNickname(id, msgArray[1]);
                sendToAllAuthorizedClients(Protocol.getTypeBroadcast("Server", user.getNickname() + " changed nickname to " + msgArray[1]));
                putLog(user.getNickname() + " changed nickname to " + msgArray[1]);
            }
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
            nickname = arrayUser[1];
            SqlClient.registration(login, password, nickname);
            user.authAccept(nickname);
//            putLog("Invalid credentials attempt for login = " + login);
//            user.authFail();
//            return;
        } else {
            ClientThread oldUser = findClientByNickname(nickname);
            user.authAccept(nickname);
            if (oldUser == null) {
                sendToAllAuthorizedClients(Protocol.getTypeBroadcast("Server", nickname + " connected"));
            } else {
                oldUser.reconnect();
                users.remove(oldUser);
            }
        }
        id = SqlClient.getId(login, password, nickname);
        sendToAllAuthorizedClients(Protocol.getUserList(getUsers()));
    }

    private void sendToAllAuthorizedClients(String msg) {
        for (int i = 0; i < users.size(); i++) {
            ClientThread recipient = (ClientThread) users.get(i);
            if (!recipient.isAuthorized()) continue;
            recipient.sendMessage(msg);
        }
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
        for (int i = 0; i < users.size(); i++) {
            ClientThread user = (ClientThread) users.get(i);
            if (!user.isAuthorized()) continue;
            if (user.getNickname().equals(nickname)) {
                return user;
            }
        }
        return null;
    }
}
