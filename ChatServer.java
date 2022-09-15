package com.geekbrains.chatserver.core;

import com.geekbrains.chatlibrary.Protocol;
import com.geekbrains.network.ServerSocketThread;
import com.geekbrains.network.ServerSocketThreadListener;
import com.geekbrains.network.SocketThread;
import com.geekbrains.network.SocketThreadListener;

import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.concurrent.ExecutorService;

public class ChatServer implements ServerSocketThreadListener, SocketThreadListener {

    private ServerSocketThread server;
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("[HH:mm:ss] ");
    private Vector<SocketThread> users;

    private ChatServerListener listener;
    private ExecutorService executorService;
    private final int LAST_MESSAGE_COUNT = 100;

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
            executorService.shutdown(); // Нужен ли тут shut down?
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
    public void onServerTimeout(ServerSocketThread thread, ServerSocket server) {
    }

    @Override
    public void onSocketAccepted(ServerSocketThread thread, ServerSocket server, Socket socket, ExecutorService executorService) {
        putLog("Client Connection");
        String name = "SocketThread" + socket.getInetAddress() + ":" + socket.getPort();
        new ClientThread(this, name, socket, executorService);
        this.executorService = executorService;
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
            case Protocol.USER_BROADCAST -> {
                sendToAllAuthorizedClients(Protocol.getTypeBroadcast(user.getNickname(), msgArray[1]));
                SqlClient.savingUserMessages(SqlClient.getId(user.getLogin(), user.getPassword(), user.getNickname()),
                        SqlClient.getId(user.getLogin(), user.getPassword(), user.getNickname()), msgArray[1], System.currentTimeMillis() / 1000L);
            }
            case Protocol.CHANGE_NICKNAME -> changeNickname(user, msgArray);
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
            user.msgFormatError(String.format("You failed to change your nickname to %s!", msgArray[1]));
            putLog(String.format("%s failed to changed nickname to %s!", user.getNickname(), msgArray[1]));
        } else {
            SqlClient.changeNickname(user.getNickname(), msgArray[1]);
            sendToAllAuthorizedClients(Protocol.getTypeBroadcast("Server", user.getNickname() + " changed nickname to " + msgArray[1]));
            SqlClient.savingUserMessages(0, 0, user.getNickname() + " changed nickname to " + msgArray[1], System.currentTimeMillis() / 1000L);
            user.setNickname(msgArray[1]);
            sendToAllAuthorizedClients(Protocol.getUserList(getUsers()));
            putLog(user.getNickname() + " changed nickname to " + msgArray[1]);
        }
    }

    private void printLastMessage(SocketThread thread, int lastMessageCount) {
        String[] lastMessages = SqlClient.showLastMessages(lastMessageCount).split(SqlClient.DELIMITER);
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
