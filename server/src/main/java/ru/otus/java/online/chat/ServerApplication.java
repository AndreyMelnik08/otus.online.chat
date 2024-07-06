package ru.otus.java.online.chat;

public class ServerApplication {
    public static void main(String[] args) {
        new Server(8000).start();
    }
}
