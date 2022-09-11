package com.geekbrains.chatlibrary;

public class Protocol {

    public static final String DELIMITER = "±";
    public static final String AUTH_REQUEST = "/auth_request";
    public static final String AUTH_ACCEPT = "/auth_accept";
    public static final String AUTH_DENIED = "/auth_denied";
    public static final String MSG_FORMAT_ERROR = "/msg_format_error";
    public static final String TYPE_BROADCAST = "/bcast";
    public static final String USER_LIST = "/user_list";
    public static final String CHANGE_NICKNAME = "/change_nick";
    public static final String USER_BROADCAST = "/user_bcast";
    public static final String PRIVATE_USER_BROADCAST = "/private_user_bcast";
    public static final String LAST_MESSAGES = "/last_msg";

    public static String getAuthRequest(String login, String password) {
        return AUTH_REQUEST + DELIMITER + login + DELIMITER + password;
    }

    public static String getAuthAccept(String nickname) {
        return AUTH_ACCEPT + DELIMITER + nickname;
    }

    public static String getAuthDenied() {
        return AUTH_DENIED;
    }

    public static String getTypeBroadcast(String src, String message) {
        return TYPE_BROADCAST + DELIMITER + System.currentTimeMillis() + DELIMITER
                + src + DELIMITER + message;
    }

    public static String getMsgFormatError(String message) {
        return  MSG_FORMAT_ERROR + DELIMITER + message;
    }

    public static String getUserList(String user) {
        return USER_LIST + DELIMITER + user;
    }

    public static String getUserBroadcast(String message) {
        return USER_BROADCAST + DELIMITER + message;
    }

    public static String getChangeNickname(String nickname) {
        return CHANGE_NICKNAME + DELIMITER + nickname;
    }

    public static String getPrivateUserBroadcast(String userFrom, String message) {
        return PRIVATE_USER_BROADCAST + DELIMITER + userFrom + DELIMITER + message;
    }

    public static String getLastMessages(String message) {
        return LAST_MESSAGES + DELIMITER + message;
    }
}
