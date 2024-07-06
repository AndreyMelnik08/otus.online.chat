package ru.otus.java.online.chat;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private int port;
    private List<ClientHandler> clients;

    public Server(int port) {
        this.port = port;
        this.clients = new ArrayList<>();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту: " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                subscribe(new ClientHandler(this, socket));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        boardcastMessage("В чат зашел: " + clientHandler.getUsername());
        clients.add(clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        boardcastMessage("Пользователь вышел из чата: " + clientHandler.getUsername());
    }

    public synchronized void boardcastMessage(String message) {
        for (ClientHandler c : clients) {
            c.sendMessage(message);
        }
    }

    public void sendPrivateMessage(String privateUsername, String privateMessage) {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(privateUsername)) {
                c.sendMessage(privateUsername + ", Вам отправили личное сообщение: " + privateMessage);
            }
        }
    }
}
