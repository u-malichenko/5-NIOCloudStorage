package com.malichenko.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientApp extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) {
        try {
            stage.setTitle("Brains Cloud");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
            Parent root;
            root = loader.load();
            scene = new Scene(root);
            stage.setScene(scene);
            Controller controller = loader.getController();
            stage.setOnHidden(e -> controller.shutdown());
            stage.show();
        } catch (IOException e) {
            GUIHelper.showError(e);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}