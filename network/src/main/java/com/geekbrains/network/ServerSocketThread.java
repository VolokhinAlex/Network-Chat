package com.geekbrains.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ServerSocketThread extends Thread{

    private int port;
    private int timeout;
    private ServerSocketThreadListener listener;

    public ServerSocketThread(ServerSocketThreadListener listener, String name, int port, int timeout) {
        super(name);
        this.port = port;
        this.timeout = timeout;
        this.listener = listener;
        start();
    }

    @Override
    public void run() {
        listener.onServerStarted(this);
        try(ServerSocket server = new ServerSocket(port)) {
            server.setSoTimeout(timeout);
            listener.onServerSocketCreated(this, server);
            while (!isInterrupted()) {
                Socket socket;
                try {
                    socket = server.accept();
                    listener.onSocketAccepted(this, server, socket);
                } catch (SocketTimeoutException e) {
                    listener.onServerTimeout(this, server);
                }
            }
        } catch (IOException e) {
            listener.onServerException(this, e);
        } finally {
            listener.onServerStopped(this);
        }
    }
}
