package ru.jchat.core.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            String msg = in.readUTF();
                            System.out.println("От клиента: " + msg);
                            out.writeUTF("echo: " + msg);
                            if (msg.equals("/end")) break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
