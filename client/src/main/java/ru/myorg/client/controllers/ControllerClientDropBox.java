package ru.myorg.client.controllers;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.myorg.client.ClientStorage;
import ru.myorg.client.commands.ApplicationCommands;
import ru.myorg.client.commands.UserCommand;
import ru.myorg.client.commands.UserCommandStatus;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Контроллер для обработки команд с основного окна приложения.
 * Также контроллер может запускать дополнительные окна выбора каталога и т.п.
 */
public class ControllerClientDropBox implements Initializable, ControllerGUI {
    public TextArea eventList;
    public ListView filesOnServer;
    public ListView sharedFiles;

    private ClientStorage clientStorage;

    private ControllerAuthorization authController;

    private final ConcurrentLinkedQueue<ApplicationCommands> commandQueue = new ConcurrentLinkedQueue<>();

    private boolean authorized = false;

    private String login;

    private Stage stage;

    private String catalog;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // контекстное меню для списка файлов пользователя на сервере
        filesOnServer.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        ContextMenu contextFilesOnServer = new ContextMenu();
        MenuItem menuDownloadFile = new MenuItem("Download selected files");
        menuDownloadFile.setOnAction(actionEvent -> {
            ObservableList<String> selectedFiles = filesOnServer.getSelectionModel().getSelectedItems();
            for(String selectedFile : selectedFiles){
                commandQueue.add(new UserCommand(UserCommandStatus.DOWNLOAD_FILE, selectedFile));
            }

        });
        MenuItem menuDownloadAllFiles = new MenuItem("Download all files");
        menuDownloadAllFiles.setOnAction(actionEvent -> {
            ObservableList<String> selectedFiles = filesOnServer.getItems();
            for(String selectedFile : selectedFiles){
                commandQueue.add(new UserCommand(UserCommandStatus.DOWNLOAD_FILE, selectedFile));
            }
        });
        MenuItem menuSharedFile = new MenuItem("Shared selected files");
        menuSharedFile.setOnAction(actionEvent -> {
            ObservableList<String> selectedFiles = filesOnServer.getItems();
            ArrayList<String> files = new ArrayList<>(selectedFiles);
            openSharedLoginSelection(files);
        });
        contextFilesOnServer.getItems().addAll(menuDownloadFile, menuDownloadAllFiles,menuSharedFile);
        filesOnServer.setContextMenu(contextFilesOnServer);

        // контекстное меню для списка расшаренных файлов для пользователя на сервере
        sharedFiles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        ContextMenu contextSharedFiles = new ContextMenu();

        MenuItem menuDownloadSharedFile = new MenuItem("Download selected files");
        menuDownloadSharedFile.setOnAction(actionEvent -> {
            ObservableList<String> selectedFiles = sharedFiles.getSelectionModel().getSelectedItems();
            for(String selectedFile : selectedFiles){
                commandQueue.add(new UserCommand(UserCommandStatus.DOWNLOAD_SHARED_FILE, selectedFile));
            }

        });

        MenuItem menuDownloadSharedAllFiles = new MenuItem("Download all files");
        menuDownloadSharedAllFiles.setOnAction(actionEvent -> {
            ObservableList<String> selectedFiles = sharedFiles.getItems();
            for(String selectedFile : selectedFiles){
                commandQueue.add(new UserCommand(UserCommandStatus.DOWNLOAD_SHARED_FILE, selectedFile));
            }
        });

        MenuItem menuReloadFiles = new MenuItem("Reload list");
        menuReloadFiles.setOnAction(actionEvent -> commandQueue.add(new UserCommand(UserCommandStatus.LIST_OF_SHARED_FILES_ON_SERVER)));

        contextSharedFiles.getItems().addAll(menuDownloadSharedFile, menuDownloadSharedAllFiles, menuReloadFiles);
        sharedFiles.setContextMenu(contextSharedFiles);

        Thread t = new Thread(() -> {
            try {
                clientStorage = new ClientStorage();
                clientStorage.connect(ControllerClientDropBox.this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Процедура выводит пользователю информацию по обработке происходящих событий в приложении
     * @param msg - сообщение
     */
    @Override
    public void printEvent(String msg) {
        Platform.runLater(() -> eventList.appendText(msg + "\n"));
    }

    /**
     * Процедура по выводу списка файлов пользователя, которые хранятся на сервере
     * @param files - список файлов
     */
    @Override
    public void setListFiles(ArrayList<String> files) {
        Platform.runLater(() -> {
            filesOnServer.getItems().clear();
            filesOnServer.getItems().addAll(files);
        });
    }

    /**
     * Процедура по выводу списка расшаренных файлов для пользователя, которые хранятся на сервере
     * @param files - список расшаренных файлов
     */
    @Override
    public void setSharedFiles(ArrayList<String> files) {
        Platform.runLater(() -> {
            sharedFiles.getItems().clear();
            sharedFiles.getItems().addAll(files);
        });
    }

    public void close() {
        clientStorage.disconnect();
    }

    @Override
    public ConcurrentLinkedQueue<ApplicationCommands> getCommandQueue() {
        return commandQueue;
    }

    public void setAuthController(ControllerAuthorization authController) {
        this.authController = authController;
    }

    /**
     * Процедура, которая выводит сообщения об ошибках аутентификации
     * @param msg - сообщение
     */
    @Override
    public void printAuthError(String msg) {
        Platform.runLater(() -> authController.printErrors(msg));
    }

    /**
     * Процедура, которая открывает основное окно приложения, после аутентификации пользователя в системе
     */
    @Override
    public void openMainWindow() {
        Platform.runLater(() -> {
            authorized = true;
            this.login = authController.getLogin();
            stage.setTitle("Drop Box Lite - '" + login + "'");
            authController.close();
        });
    }

    public boolean isAuthorized() {
        return authorized;
    }

    @Override
    public String getLogin() {
        return login;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Override
    public String getCatalog() {
        return catalog;
    }

    @Override
    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    /**
     * Процедура обрабатывает выбор каталога пользователя
     */
    @Override
    public void openDirectorySelection() {
        Platform.runLater(() -> {
            try {
                Stage choosingStage = new Stage();
                FXMLLoader choosingLoader = new FXMLLoader(getClass().getResource("/ru.myorg.client_gui/choosing_catalog.fxml"));

                Parent choosingParent = choosingLoader.load();

                ControllerChoosingCatalog controllerChoosingCatalog = choosingLoader.getController();
                controllerChoosingCatalog.setStage(choosingStage);
                controllerChoosingCatalog.setCommandQueue(commandQueue);
                controllerChoosingCatalog.setControllerGUI(this);
                choosingStage.setOnCloseRequest(controllerChoosingCatalog::closeWindow);

                Scene choosingScene = new Scene(choosingParent);
                choosingStage.setTitle("Choosing catalog for exchange");
                choosingStage.setScene(choosingScene);
                choosingStage.initModality(Modality.WINDOW_MODAL);
                choosingStage.initOwner(stage);
                choosingStage.showAndWait();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Процедура обрабатывает расшаривание файлов для других пользователей
     * @param files - выбранные файлы для расшаривания
     */
    private void openSharedLoginSelection(ArrayList<String> files) {
        Platform.runLater(() -> {
            try {
                Stage sharedStage = new Stage();
                FXMLLoader sharedLoader = new FXMLLoader(getClass().getResource("/ru.myorg.client_gui/choosing_shared.fxml"));

                Parent sharedParent = sharedLoader.load();

                ControllerChoosingShared controllerChoosingShared = sharedLoader.getController();
                controllerChoosingShared.setSharedFiles(files);
                controllerChoosingShared.setStage(sharedStage);
                controllerChoosingShared.setCommandQueue(commandQueue);

                Scene sharedScene = new Scene(sharedParent);
                sharedStage.setTitle("Choosing login for sharing");
                sharedStage.setScene(sharedScene);
                sharedStage.initModality(Modality.WINDOW_MODAL);
                sharedStage.initOwner(stage);
                sharedStage.showAndWait();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
