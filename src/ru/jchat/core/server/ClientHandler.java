package ru.jchat.core.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public ClientHandler(Server srv, Socket sock) {
        try {
            this.server = srv;
            this.socket = sock;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/auth ")) {
                            String[] data = msg.split(" ");
                            if (data[1].equals("login") && data[2].equals("password")) {
                                sendMsg("/authok");
                                System.out.println("Клиент авторизовался");
                                server.subscribe(this);
                                continue;
                            }
                            else {
                                sendMsg("Неверный логин и/или пароль");
                                continue;
                            }
                        }
                        System.out.println("От клиента: " + msg);
                        if (msg.equals("/end")) break;
                        server.broadcastMsg("client: " + msg);
                        //sendMsg("echo: " + msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
