package ru.otus.java.online.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private int port;
    private List<ClientHandler> clients;
    private AuthenticationProvider authenticationProvider;
    private ServerSocket serverSocket;

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public Server(int port) throws SQLException, IOException {
        this.port = port;
        this.clients = new ArrayList<>();
        this.authenticationProvider = new Verification(this);
        this.serverSocket = new ServerSocket(port);
    }

    public void start() throws IOException {

        System.out.println("Сервер запущен на порту: " + port);
        authenticationProvider.initialize();
        while (true) {
            Socket socket = serverSocket.accept();
            try {
                new ClientHandler(this, socket);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public void close() throws IOException {
        for (ClientHandler c : clients) {
            c.sendMessage("/shutdown");
        }
        serverSocket.close();
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        broadcastMessage("В чат зашел: " + clientHandler.getUsername());
        clients.add(clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastMessage("Из чата вышел: " + clientHandler.getUsername());
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler c : clients) {
            c.sendMessage(message);
        }
    }

    public synchronized void privateMessage(String username, String message) {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(username)) {
                c.sendMessage("Личное сообщение: " + message);
            }
        }
    }

    public boolean isUsernameBusy(String username) {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public void kickUsername(String username) {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(username)) {
                c.sendMessage("/ban");
            }
        }
    }

    public void onlineUsersInfo(ClientHandler clientHandler) {
        List<String> onlineUsers = new ArrayList<>();
        for (ClientHandler c : clients) {
            if (c.isOnline()) {
                onlineUsers.add(c.getUsername());
            }
        }
        clientHandler.sendMessage("Список онлайн пользователей: " + onlineUsers);
    }
}
