package com.geekbrains.chatserver.core;

import org.sqlite.JDBC;

import java.sql.*;
import java.text.SimpleDateFormat;

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
        String query = String.format("SELECT nickname FROM user WHERE login='%s' AND password='%s'",
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

    public static Integer getId(String login, String password, String nickname) {
        try {
            preparedStatement = connection.prepareStatement("SELECT id FROM user WHERE login=? AND password=? AND nickname=?");
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

    public static Integer getIdByNickname(String nickname) {
        try {
            preparedStatement = connection.prepareStatement("SELECT id FROM user WHERE nickname=?");
            preparedStatement.setString(1, nickname);
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
            preparedStatement = connection.prepareStatement("UPDATE user SET nickname=? WHERE nickname=?");
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
        String query = String.format("SELECT nickname FROM user WHERE nickname=\"%s\"", nickname);
        try (ResultSet set = statement.executeQuery(query)) {
            return set.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void savingUserMessages(int userFromId, int userToId, String message, long dateTime) {
        try {
            preparedStatement = connection.prepareStatement("INSERT INTO message(user_from_id, user_to_id, message, date_time) VALUES(?, ?, ?, ?);");
            preparedStatement.setInt(1, userFromId);
            preparedStatement.setInt(2, userToId);
            preparedStatement.setString(3, message);
            preparedStatement.setLong(4, dateTime);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getNickname(int userId) {
        try {
            preparedStatement = connection.prepareStatement("SELECT nickname FROM user WHERE id=?");
            preparedStatement.setInt(1, userId);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                return rs.getString("nickname");
            }
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static String showLastMessages(int count) {
        try {
            preparedStatement = connection.prepareStatement("SELECT * FROM message ORDER BY id DESC LIMIT ?;");
            preparedStatement.setInt(1, count);
            ResultSet set = preparedStatement.executeQuery();
            StringBuilder lastMessage = new StringBuilder();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("[yyyy-MM-dd] [HH:mm]  ");
            while (set.next()) {
                Date date = new Date(set.getLong("date_time") * 1000L);
                lastMessage.append(String.format("%s%s: %s", simpleDateFormat.format(date),
                        getNickname(set.getInt("user_to_id")), set.getString("message"))).append(DELIMITER);
            }
            preparedStatement.close();
            set.close();
            return lastMessage.toString();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
