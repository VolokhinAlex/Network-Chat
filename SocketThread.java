package com.geekbrains.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class SocketThread {
    private final SocketThreadListener listener;
    private final Socket socket;
    private DataOutputStream out;
    private ExecutorService executorService;

    public SocketThread(SocketThreadListener listener, String name, Socket socket, ExecutorService executorService) {
        this.listener = listener;
        this.socket = socket;
        this.executorService = executorService;
        executorService.submit(() -> {
            try {
                listener.onSocketStart(SocketThread.this, socket);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                listener.onSocketReady(SocketThread.this, socket);
                System.out.println(executorService);
                while (!executorService.isShutdown()) {
                    String msg = in.readUTF();
                    listener.onReceiveString(this, socket, msg);
                }
            } catch (IOException ex) {
                listener.onSocketException(SocketThread.this, ex);
                close();
            } finally {
                listener.onSocketStop(SocketThread.this);
            }
        });
    }

    public synchronized boolean sendMessage(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            close();
            return false;
        }
    }

    public synchronized void close() {
        try {
            socket.close();
        } catch (IOException ex) {
            listener.onSocketException(this, ex);
        }
    }
}
