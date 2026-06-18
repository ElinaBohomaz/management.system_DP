package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/ui/LoginWindow.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1920, 1080);
        scene.getStylesheets().add(
                getClass().getResource("/org/example/ui/styles.css").toExternalForm()
        );

        primaryStage.setTitle("Графік змін працівників - Полтававодоканал");
        primaryStage.getIcons().add(
                new Image(getClass().getResourceAsStream("/org/example/ui/icon.ico"))
        );
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}