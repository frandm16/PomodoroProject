package com.frandm.pomodoro;

import atlantafx.base.theme.PrimerDark;
import fr.brouillard.oss.cssfx.CSSFX;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.util.Objects;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("main_view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        String customStyles = Objects.requireNonNull(
                App.class.getResource("/com/frandm/pomodoro/styles.css")
        ).toExternalForm();
        scene.getStylesheets().add(customStyles);

        stage.setTitle("Pomodoro Tracker");
        stage.setMaximized(true);
        stage.setResizable(true);

        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (KeyCode.F11.equals(event.getCode())) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });

        CSSFX.start();

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}