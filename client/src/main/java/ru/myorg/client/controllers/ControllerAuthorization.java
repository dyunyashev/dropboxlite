package ru.myorg.client.controllers;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.myorg.client.commands.ApplicationCommands;
import ru.myorg.client.commands.UserCommand;
import ru.myorg.client.commands.UserCommandStatus;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Контроллер, обрабатывающий авторизацию пользователя
 */
public class ControllerAuthorization {
    public TextField login;

    public PasswordField password;

    public Label errors;

    public Button enterButton;

    private Stage stage;

    private ConcurrentLinkedQueue<ApplicationCommands> commandQueue;

    private boolean btnPressed = false;

    /**
     * Обработка события отправки логина-пароля на сервер
     * @param actionEvent - клик мышки по кнопке авторизации
     */
    public void onAuthorizationBtnClick(ActionEvent actionEvent) {
        if (btnPressed) {
            printErrors("In progress...");
            return;
        }

        if (login.getText().isEmpty() || password.getText().isEmpty()) {
            errors.setText("Username/password is not filled in");
            return;
        }
        UserCommand userCommand = new UserCommand(UserCommandStatus.AUTHORIZE, login.getText(), password.getText());
        commandQueue.add(userCommand);
        printErrors("In progress...");
        btnPressed = true;
    }

    /**
     * Запомнить окно, в котором открылось окно авторизации
     * @param stage - окно
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Вывести в окно авторизации информацию об ошибках аутентификации
     * @param msg - сообщение об ошибке
     */
    public void printErrors(String msg) {
        errors.setText(msg);
        btnPressed = false;
    }

    /**
     * Закрыть окно авторизации
     */
    public void close() {
        stage.close();
    }

    /**
     * Сохранить очередь команд, для того, чтобы записывать в нее команды по отправке данных пользователя на сервер
     * @param commandQueue - очередь команд
     */
    public void setCommandQueue(ConcurrentLinkedQueue<ApplicationCommands> commandQueue) {
        this.commandQueue = commandQueue;
    }

    /**
     * Получение логина пользователя
     * @return - возвращает логин пользователя, который прошел аутентификацию на сервере
     */
    public String getLogin() {
        return login.getText();
    }
}
