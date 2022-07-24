package ru.myorg.client.controllers;

import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.myorg.client.commands.ApplicationCommands;
import ru.myorg.client.commands.UserCommand;
import ru.myorg.client.commands.UserCommandStatus;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Контроллер позволяет работать с интерфейсом по расшариванию файлов другим пользователям
 */
public class ControllerChoosingShared {

    public TextField sharedLogin;

    private ArrayList<String> sharedFiles;

    private Stage stage;

    private ConcurrentLinkedQueue<ApplicationCommands> commandQueue;

    public void onBtnClick(ActionEvent actionEvent) {
        if (!sharedLogin.getText().equals("")) {
            for (String sharedFile:sharedFiles) {
                UserCommand userCommand = new UserCommand(UserCommandStatus.SHARE_FILE, sharedFile);
                userCommand.setLogin(sharedLogin.getText());
                commandQueue.add(userCommand);
            }
        }
        stage.close();
    }

    public void setSharedFiles(ArrayList<String> sharedFiles) {
        this.sharedFiles = sharedFiles;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setCommandQueue(ConcurrentLinkedQueue<ApplicationCommands> commandQueue) {
        this.commandQueue = commandQueue;
    }
}
