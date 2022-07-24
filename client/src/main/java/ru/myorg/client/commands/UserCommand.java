package ru.myorg.client.commands;

import java.io.Serializable;

/**
 * Команды от пользователя на загрузку файла с сервера, авторизацию, получение списка файлов пользователя на сервере
 */
public final class UserCommand extends ApplicationCommands implements Serializable {
    private final UserCommandStatus status;

    private String fileName;

    private String login;

    private String password;

    public UserCommand(UserCommandStatus status) {
        this.status = status;
    }

    public UserCommand(UserCommandStatus status, String fileName) {
        this.status = status;
        this.fileName = fileName;
    }

    public UserCommand(UserCommandStatus status, String login, String password) {
        this.status = status;
        this.login = login;
        this.password = password;
    }

    public UserCommandStatus getStatus() {
        return status;
    }

    public String getFileName() {
        return fileName;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public void setLogin(String login) {
        this.login = login;
    }
}
