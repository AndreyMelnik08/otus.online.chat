package ru.otus.java.online.chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private String login;
    private Role role;
    private boolean online;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                System.out.println("Подключился новый клиент");
                while (true) {
                    String message = in.readUTF();
                    if (message.equals("/exit")) {
                        sendMessage("/exitok");
                        return;
                    }
                    if (message.startsWith("/auth ")) {
                        String[] elements = message.split(" ");
                        if (elements.length != 3) {
                            sendMessage("Неверный формат команды /auth");
                            continue;
                        }
                        if (server.getAuthenticationProvider().authenticate(this, elements[1], elements[2])) {
                            break;
                        }
                        continue;
                    }
                    if (message.startsWith("/register ")) {
                        String[] elements = message.split(" ");
                        if (elements.length != 4) {
                            sendMessage("Неверный формат команды /register");
                            continue;
                        }
                        if (server.getAuthenticationProvider().registration(this, elements[1], elements[2], elements[3])) {
                            break;
                        }
                        continue;
                    }
                    sendMessage("Перед работой с чатом необходимо выполнить аутентификацию '/auth login password' или регистрацию '/register login password username'");
                }
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.equals("/exit")) {
                            sendMessage("/exitok");
                            break;
                        }
                        if (message.startsWith("/w ")) {
                            String[] elements = message.split(" ");
                            if (elements.length != 3) {
                                sendMessage("Неверный формат команды /w");
                                continue;
                            }
                            server.privateMessage(elements[1], elements[2]);
                            continue;
                        }
                        if (message.startsWith("/ban ")) {
                            String[] elements = message.split(" ");
                            if (elements.length != 2) {
                                sendMessage("Неверный формат команды /ban");
                                continue;
                            } else if (!server.getAuthenticationProvider().roleVerification(this)) {
                                sendMessage("Недостаточно прав");
                                continue;
                            }
                            server.kickUsername(elements[1]);
                            sendMessage("Вы удалили: " + elements[1]);
                            continue;
                        }
                        if (message.startsWith("/changenick ")) {
                            String[] elements = message.split(" ");
                            if (elements.length != 2) {
                                sendMessage("Неверный формат команды /changenick");
                                continue;
                            }
                            server.getAuthenticationProvider().changeUsername(this, elements[1]);
                            sendMessage("Вы удалили: " + elements[1]);
                            continue;
                        }
                        if (message.startsWith("/activelist")) {
                            server.onlineUsersInfo(this);
                            continue;
                        }
                        if (message.startsWith("/shutdown")) {
                            if (!server.getAuthenticationProvider().roleVerification(this)) {
                                sendMessage("Недостаточно прав");
                                continue;
                            }
                            System.out.println("Сервер остановлен");
                            server.close();
                            disconnect();
                        }
                    }
                    server.broadcastMessage(username + ": " + message);
                    disconnectIfInactive();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    public void sendMessage(String message) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String messageWithTime = message + " [" + LocalTime.now().format(formatter) + "]";
            out.writeUTF(messageWithTime);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnectIfInactive() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    sendMessage("Вы были отключены из-за неактивности в течении 20 минут");
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    disconnect();
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 20 * 60 * 10000);
    }
}
