package ru.otus.java.online.chat;

import java.io.IOException;
import java.sql.SQLException;

public class ServerApplication {
    public static void main(String[] args) throws SQLException, IOException {
        new Server(8001).start();
    }
}
