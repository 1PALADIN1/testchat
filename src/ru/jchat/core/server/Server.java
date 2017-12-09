package ru.jchat.core.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Vector;

public class Server {
    private Vector<ClientHandler> clients;
    private AuthService authService;

    public Server() {
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            clients = new Vector<>();
            authService = new AuthService();
            authService.connect();
            System.out.println("Сервер запущен. Ожидаю клиентов...");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Клиент подключен: " + socket.getInetAddress() + ":" + socket.getPort() + "(" + socket.getLocalPort() + ")");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Не удалось запустить сервис авторизации");
        } finally {
            if (authService != null) authService.disconnect();
        }
    }

    //добавляем клиента в список онлайн
    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientsList();
    }

    //удаляем клиента из списка онлайн
    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientsList();
    }

    public void broadcastMsg(String msg) {
        for (ClientHandler o: clients) {
            o.sendMsg(msg);
        }
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean isNickBusy(String nick) {
        for (ClientHandler o: clients) {
            if (o.getNick().equals(nick)) return true;
        }
        return false;
    }

    //отправка личного сообщения
    public void sendPrivateMsg(ClientHandler from, String nickTo, String msg) {
        for (ClientHandler o: clients) {
            if (o.getNick().equals(nickTo)) {
                o.sendMsg("Личное сообщение от " + from.getNick() + ": " + msg);
                from.sendMsg("Личное сообщение отправлено " + nickTo);
                return;
            }
        }
        from.sendMsg(nickTo + " не найден");
    }

    //рассылка списка клиентов в чате
    public void broadcastClientsList() {
        StringBuilder sb = new StringBuilder("/clientslist ");
        for (ClientHandler o: clients) {
            sb.append(o.getNick() + " ");
        }

        // /clientslist nick1 nick2 nick3
        String out = sb.toString();
        for (ClientHandler o: clients) {
            o.sendMsg(out);
        }
    }
}
