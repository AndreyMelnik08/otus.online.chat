package ru.otus.java.online.chat;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Verification implements AuthenticationProvider, AutoCloseable {
    private static final String DATABASE_URL = "jdbc:sqlite:C:/Users/Компутер/IdeaProjects/otus-online-chat/usersBD.db";
    private static Connection connection;
    private static String USERVALUE = "SELECT * FROM usersBD";

    private static final String USERADD = "INSERT INTO usersBD (login, password, username, role) VALUES (?, ?, ?, ?)";

    private class User {
        private String login;
        private String password;
        private String username;
        private Role role;

        public User(String login, String password, String username, Role role) {
            this.login = login;
            this.password = password;
            this.username = username;
            this.role = role;
        }
    }

    private Server server;
    private List<User> users;


    public Verification(Server server) throws SQLException {
        connection = DriverManager.getConnection(DATABASE_URL);
        this.server = server;
        this.users = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(USERVALUE)) {
                while (resultSet.next()) {
                    String login = resultSet.getString(2);
                    String password = resultSet.getString(3);
                    String username = resultSet.getString(4);
                    Role role = Role.valueOf(resultSet.getString(5));
                    User user = new User(login, password, username, role);
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize() {
        System.out.println("Сервис аутентификации запущен: In-Memory режим");
    }

    private String getUsernameByLoginAndPassword(String login, String password) {
        for (User u : users) {
            if (u.login.equals(login) && u.password.equals(password)) {
                return u.username;
            }
        }
        return null;
    }

    private Role getRoleByLoginAndPassword(String login, String password) {
        for (User u : users) {
            if (u.login.equals(login) && u.password.equals(password)) {
                return u.role;
            }
        }
        return null;
    }

    private boolean isLoginAlreadyExist(String login) {
        for (User u : users) {
            if (u.login.equals(login)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUsernameAlreadyExist(String username) {
        for (User u : users) {
            if (u.username.equals(username)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean authenticate(ClientHandler clientHandler, String login, String password) {
        String authUsername = getUsernameByLoginAndPassword(login, password);
        Role role = getRoleByLoginAndPassword(login, password);
        if (authUsername == null) {
            clientHandler.sendMessage("Некорретный логин/пароль");
            return false;
        }
        if (server.isUsernameBusy(authUsername)) {
            clientHandler.sendMessage("Указанная учетная запись уже занята");
            return false;
        }
        clientHandler.setUsername(authUsername);
        clientHandler.setRole(role);
        clientHandler.setLogin(login);
        clientHandler.setOnline(true);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/authok " + authUsername);
        return true;
    }

    @Override
    public boolean registration(ClientHandler clientHandler, String login, String password, String username) {
        if (login.trim().length() < 3 || password.trim().length() < 6 || username.trim().length() < 1) {
            clientHandler.sendMessage("Логин 3+ символа, Пароль 6+ символов, Имя пользователя 1+ символ");
            return false;
        }
        if (isLoginAlreadyExist(login)) {
            clientHandler.sendMessage("Указанный логин уже занят");
            return false;
        }
        if (isUsernameAlreadyExist(username)) {
            clientHandler.sendMessage("Указанное имя пользователя уже занято");
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement(USERADD)) {
            statement.setString(1, login);
            statement.setString(2, password);
            statement.setString(3, username);
            statement.setString(4, String.valueOf(Role.USER));
            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                users.add(new User(login, password, username, Role.USER));
                clientHandler.setUsername(username);
                clientHandler.setRole(Role.USER);
                clientHandler.setLogin(login);
                clientHandler.setOnline(true);
                server.subscribe(clientHandler);
                clientHandler.sendMessage("/regok " + username);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean changeUsername(ClientHandler clientHandler, String newUsername) {
        if (newUsername.trim().length() < 1) {
            clientHandler.sendMessage("Имя пользователя должно содержать хотя бы 1 символ");
            return false;
        }
        if (isUsernameAlreadyExist(newUsername)) {
            clientHandler.sendMessage("Указанное имя пользователя уже занято");
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement("UPDATE usersBD SET username = ? WHERE login = ?")) {
            statement.setString(1, newUsername);
            statement.setString(2, clientHandler.getLogin());
            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated > 0) {
                clientHandler.setUsername(newUsername);
                clientHandler.sendMessage("Имя пользователя успешно изменено на " + newUsername);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean roleVerification(ClientHandler clientHandler) {
        if (clientHandler.getRole().equals(Role.ADMIN)) {
            return true;
        }
        return false;
    }

    @Override
    public void close() throws Exception {
        try {
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
