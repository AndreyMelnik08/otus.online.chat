package ru.otus.java.online.chat;

public interface AuthenticationProvider {
    void initialize();
    boolean authenticate(ClientHandler clientHandler, String login, String password);
    boolean registration(ClientHandler clientHandler, String login, String password, String username);
    boolean roleVerification(ClientHandler clientHandler);
}
