package ru.myorg.client.controllers;

import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import ru.myorg.client.commands.ApplicationCommands;
import ru.myorg.client.commands.UserCommand;
import ru.myorg.client.commands.UserCommandStatus;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Контроллер, для обработки выбора каталога пользователя,
 * с которым будет производиться синхронизация данных
 */
public class ControllerChoosingCatalog {
    public Label errorMessage;

    private Stage stage;

    private ConcurrentLinkedQueue<ApplicationCommands> commandQueue;

    private ControllerClientDropBox controllerGUI;

    private boolean catalogSelected = false;

    /**
     * Обработка события выбора каталога пользователя
     * @param actionEvent - событие выбора каталога
     */
    public void onChooseCatalogClick(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory == null) {
            errorMessage.setText("No directory selected");
        }
        else {
            String catalog = selectedDirectory.getAbsolutePath();
            commandQueue.add(new UserCommand(UserCommandStatus.SET_CATALOG, catalog));
            controllerGUI.setCatalog(catalog);
            catalogSelected = true;
            stage.close();
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setCommandQueue(ConcurrentLinkedQueue<ApplicationCommands> commandQueue) {
        this.commandQueue = commandQueue;
    }

    public void setControllerGUI(ControllerClientDropBox controllerGUI) {
        this.controllerGUI = controllerGUI;
    }

    public void closeWindow(WindowEvent windowEvent) {
        if (!catalogSelected) {
            errorMessage.setText("No directory selected");
            windowEvent.consume();
        }
    }
}
