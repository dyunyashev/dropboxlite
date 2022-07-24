package ru.myorg.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.myorg.client.controllers.ControllerAuthorization;
import ru.myorg.client.controllers.ControllerClientDropBox;

import java.io.IOException;

/**
 * Класс стартует приложение. Запускает основное окно и окно авторизации пользователя
 */
public class ClientStorageApplication extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        // Создаем основное окно
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ru.myorg.client_gui/clientdropbox.fxml"));
        Parent parent = fxmlLoader.load();

        ControllerClientDropBox controller = fxmlLoader.getController();
        controller.setStage(stage);
        stage.setOnCloseRequest(windowEvent -> controller.close());

        Scene scene = new Scene(parent);
        stage.setTitle("Drop Box Lite");
        stage.setScene(scene);

        // Создаем и открываем окно авторизации
        Stage authStage = new Stage();
        FXMLLoader authLoader = new FXMLLoader(getClass().getResource("/ru.myorg.client_gui/authorization.fxml"));
        Parent authParent = authLoader.load();

        ControllerAuthorization controllerAuthorization = authLoader.getController();

        controller.setAuthController(controllerAuthorization);

        controllerAuthorization.setStage(authStage);
        controllerAuthorization.setCommandQueue(controller.getCommandQueue());

        Scene authScene = new Scene(authParent);
        authStage.setTitle("Authorization");
        authStage.setScene(authScene);

        authStage.initModality(Modality.WINDOW_MODAL);
        authStage.initOwner(stage);
        authStage.showAndWait();

        // Если авторизация прошла успешно - открываем основное окно приложения
        if (controller.isAuthorized()) {
            stage.show();
        }
        else {
            controller.close();
        }
    }
}
