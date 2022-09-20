package com.geekbrains.chatserver.core;

import org.sqlite.JDBC;

import java.sql.*;

public class SqlClient {

    private static Connection connection;
    private static Statement statement;
    private static PreparedStatement preparedStatement;
    public static final String DELIMITER = "±";

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(JDBC.PREFIX + "chat-server/clients.db");
            statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getNickname(String login, String password) {
        String query = String.format("select nickname from clients where login='%s' and password='%s'",
                login, password);
        try (ResultSet set = statement.executeQuery(query)) {
            if (set.next()) {
                return set.getString("nickname");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static String registration(String login, String password, String nickname) {
        String query = String.format("INSERT INTO clients (login, password, nickname) VALUES(\"%s\", \"%s\", \"%s\")", login, password, nickname);
        try {
            statement.executeUpdate(query);
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static Integer getId(String login, String password, String nickname) {
        try {
            preparedStatement = connection.prepareStatement("select id from clients where login=? and password=? and nickname=?");
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            preparedStatement.setString(3, nickname);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static boolean changeNickname(String oldNickname, String newNickname) {
        try {
            if (isAlreadyThereNickname(newNickname)) return false;
            preparedStatement = connection.prepareStatement("update clients SET nickname=? where nickname=?");
            preparedStatement.setString(1, newNickname);
            preparedStatement.setString(2, oldNickname);
            preparedStatement.executeUpdate();
            preparedStatement.close();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void disconnect() {
        try {
            statement.close();
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isAlreadyThereNickname(String nickname) {
        String query = String.format("select nickname from clients where nickname=\"%s\"", nickname);
        try (ResultSet set = statement.executeQuery(query)) {
            return set.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void savingUserMessages(int uid, String nickname, String message, long dateTime) {
        try {
            preparedStatement = connection.prepareStatement("INSERT INTO messages(uid, nickname, message, date_time) VALUES(?, ?, ?, ?);");
            preparedStatement.setInt(1, uid);
            preparedStatement.setString(2, nickname);
            preparedStatement.setString(3, message);
            preparedStatement.setLong(4, dateTime);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String showLastMessages(int count) {
        try {
            preparedStatement = connection.prepareStatement("SELECT * FROM messages ORDER BY id DESC LIMIT ?;");
            preparedStatement.setInt(1, count);
            ResultSet set = preparedStatement.executeQuery();
            StringBuffer stringBuffer = new StringBuffer();
            while (set.next()) {
                stringBuffer.append(set.getString("nickname") + ": " + set.getString("message")).append(DELIMITER);
            }
            preparedStatement.close();
            set.close();
            return stringBuffer.toString();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
