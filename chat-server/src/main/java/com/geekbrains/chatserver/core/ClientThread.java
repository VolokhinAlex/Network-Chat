package com.geekbrains.chatserver.core;

import com.geekbrains.chatlibrary.Protocol;
import com.geekbrains.network.SocketThread;
import com.geekbrains.network.SocketThreadListener;


import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class ClientThread extends SocketThread {

    private String nickname;
    private String login;
    private String password;
    private boolean isAuthorized;
    private boolean isReconnecting;

    public ClientThread(SocketThreadListener listener, String name, Socket socket, ExecutorService executorService) {
        super(listener, name, socket, executorService);
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

    public void reconnect() {
        isReconnecting = true;
        close();
    }

    public boolean isReconnecting() {
        return isReconnecting;
    }

    void getData(String login, String password) {
        this.login = login;
        this.password = password;
    }

    void authAccept(String nickname) {
        isAuthorized = true;
        this.nickname = nickname;
        sendMessage(Protocol.getAuthAccept(nickname));
    }

    void authFail() {
        sendMessage(Protocol.getAuthDenied());
        close();
    }

    void msgFormatError(String msg) {
        sendMessage(Protocol.getMsgFormatError(msg));
        close();
    }

    void sendPrivateMessage(String nickname, String msg) {
        sendMessage(Protocol.getTypeBroadcast(nickname, msg));
    }

}