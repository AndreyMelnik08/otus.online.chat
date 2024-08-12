package ru.otus.java.online.chat;

import java.sql.SQLException;

public class ServerApplication {
    public static void main(String[] args) throws SQLException {

        new Server(8001).start();
    }
}
