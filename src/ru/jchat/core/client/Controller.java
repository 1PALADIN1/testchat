package ru.jchat.core.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    TextArea textArea;
    @FXML
    TextField msgField;
    @FXML
    HBox authPanel;
    @FXML
    HBox msgPanel;
    @FXML
    TextField loginField;
    @FXML
    PasswordField passField;
    @FXML
    ListView<String> clientsListView;

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private boolean authorized = false;
    private ObservableList<String> clientList; //обновляемый список

    final String SERVER_IP = "localhost";
    final int SERVER_PORT = 8189;

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
        if (authorized) {
            msgPanel.setVisible(true);
            msgPanel.setManaged(true);
            clientsListView.setVisible(true);
            clientsListView.setManaged(true);
            authPanel.setVisible(false);
            authPanel.setManaged(false);
        } else {
            msgPanel.setVisible(false);
            msgPanel.setManaged(false);
            clientsListView.setVisible(false);
            clientsListView.setManaged(false);
            authPanel.setVisible(true);
            authPanel.setManaged(true);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthorized(false);
    }

    public void connect() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            clientList = FXCollections.observableArrayList();
            clientsListView.setItems(clientList);

            clientsListView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
                @Override
                public ListCell<String> call(ListView<String> param) {
                    return new ListCell<String>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (!empty) {
                                setText(item);
                                if (item.equals("nick1")) {
                                    setStyle("-fx-font-weight: bold; -fx-background-color: cornflowerblue;");
                                } else {
                                    setGraphic(null);
                                }
                            }
                        }
                    };
                }
            });

            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        String s = in.readUTF();
                        //для служебных сообщений
                        if (s.startsWith("/")) {
                            //авторизация
                            if (s.equals("/authok")) {
                                setAuthorized(true);
                            }
                            //список пользователей в сети
                            if (s.startsWith("/clientslist ")) {
                                String data[] = s.split("\\s");
                                Platform.runLater(() -> {
                                    clientList.clear();
                                    for (int i=1; i<data.length; i++) {
                                        clientList.addAll(data[i]);
                                    }
                                });
                            }
                            continue;
                        }
                        textArea.appendText(s + "\n");
                    }
                } catch (IOException e) {
                    showAlert("Сервер перестал отвечать");
                } finally {
                    setAuthorized(false);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            t.setDaemon(true);
            t.start();
        } catch (IOException e) {
            showAlert("Не удалось подключиться к серверу. Проверьте сетевое соединение.");
        }
    }

    public void sendMsg() {
        try {
            out.writeUTF(msgField.getText());
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            //ДЗ: если не получается отправить сообщение
            showAlert("Не удалось отправить сообщение. Проверьте соодинение.");
            //e.printStackTrace();
        }
    }

    public void sendAuthMsg() {
        if (loginField.getText().isEmpty() || passField.getText().isEmpty()) {
            showAlert("Поля логин и пароль должны быть заполнены");
            return;
        }

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF("/auth " + loginField.getText() + " " + passField.getText());
            loginField.clear();
            passField.clear();
        } catch (IOException e) {
            //ДЗ: ошибка аутентификации
            showAlert("Ошибка аутентификации. Проверьте соединение.");
            //e.printStackTrace();
        }
    }

    public void showAlert(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Возникли проблемы");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }
}
