package ru.jchat.core.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public Server() {
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            System.out.println("Сервер запущен. Ожидаю клиентов...");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Клиент подключен");
                new ClientHandler(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
