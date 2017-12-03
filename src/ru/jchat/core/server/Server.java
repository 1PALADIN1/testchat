package ru.jchat.core.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class Server {
    private Vector<ClientHandler> clients;
    public Server() {
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            clients = new Vector<>();
            System.out.println("Сервер запущен. Ожидаю клиентов...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Клиент подключен: " + socket.getInetAddress() + ":" + socket.getPort() + "(" + socket.getLocalPort() + ")");
                clients.add(new ClientHandler(this, socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastMsg(String msg) {
        for (ClientHandler o: clients) {
            o.sendMsg(msg);
        }
    }
}
