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
    private String nick;
    private final int DICSONNECT_INTERVAL = 120000; //интервал отключения неавторизованных пользователей
    private boolean isAuth;

    public ClientHandler(Server srv, Socket sock) {
        try {
            this.server = srv;
            this.socket = sock;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.isAuth = false;
            new Thread(() -> {
                try {
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/auth ")) {
                            String[] data = msg.split("\\s");

                            //запускаем таймер авторизации
                            checkAuth();

                            //получаем логин и пароль из базы
                            if (data.length == 3) {
                                String newNick = server.getAuthService().getNickByLoginAndPass(data[1], data[2]);
                                if (newNick != null) {
                                    if (!server.isNickBusy(newNick)) {
                                        nick = newNick;
                                        isAuth = true;
                                        sendMsg("/authok " + newNick);
                                        System.out.println("Пользователь " + newNick + " авторизовался");
                                        server.subscribe(this);
                                    } else {
                                        sendMsg("Учётная запись уже занята");
                                    }
                                } else {
                                    sendMsg("Неверный логин и/или пароль");
                                }
                            }
                            continue;
                        }
                        System.out.println(nick + ": " + msg);
                        //служебные команды
                        if (msg.startsWith("/")) {
                            if (msg.startsWith("/w ")) {
                                String[] data = msg.split("\\s", 3);
                                server.sendPrivateMsg(this, data[1], data[2]);
                            }
                            if (msg.equals("/end")) break;
                        } else {
                            server.broadcastMsg(nick + ": " + msg);
                        }
                    }
                } catch (IOException e) {
                    //e.printStackTrace();
                    if (nick != null) System.out.println("Пользователь " + nick + " покинул нас");
                    else
                        System.out.println("Соединение было разорвано по непонятным администратору причинам");
                } finally {
                    nick = null;
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

    public String getNick() {
        return nick;
    }

    private void checkAuth() {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(DICSONNECT_INTERVAL);
                if (!isAuth && !socket.isClosed()) {
                    sendMsg("Соединение закрыто");
                    System.out.println("Пользователь " + socket.getInetAddress()
                            + ":" + socket.getPort() + "(" + socket.getLocalPort() + ") был отключен от сервера");
                    socket.close();
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
