package com.geekbrains.network;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public interface ServerSocketThreadListener {

    void onServerStarted(ServerSocketThread thread);
    void onServerStopped(ServerSocketThread thread);
    void onServerSocketCreated(ServerSocketThread thread, ServerSocket server);
    void onServerTimeout(ServerSocketThread thread, ServerSocket server);
    void onSocketAccepted(ServerSocketThread thread, ServerSocket server, Socket socket, ExecutorService executorService);
    void onServerException(ServerSocketThread thread, Throwable exception);

}
