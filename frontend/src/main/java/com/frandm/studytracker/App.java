package com.frandm.studytracker;

import atlantafx.base.theme.PrimerDark;
import fr.brouillard.oss.cssfx.CSSFX;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.Objects;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/com/frandm/studytracker/fxml/main_view.fxml"));
        Parent root = fxmlLoader.load();

        Font.loadFont(getClass().getResourceAsStream("/com/frandm/studytracker/fonts/SF-Pro-Display-Regular.otf"), 12);
        Font.loadFont(getClass().getResourceAsStream("/com/frandm/studytracker/fonts/SF-Pro-Display-Bold.otf"), 12);

        javafx.scene.Node content = ((javafx.scene.layout.Pane) root).getChildren().getFirst();

        content.setOpacity(0);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        String mainStyles = Objects.requireNonNull(App.class.getResource("/com/frandm/studytracker/css/styles.css")).toExternalForm();
        scene.getStylesheets().add(mainStyles);

        stage.setScene(scene);

        root.applyCss();
        root.layout();

        stage.show();

        URL iconUrl = getClass().getResource("/com/frandm/studytracker/images/STlogo.png");
        if (iconUrl != null) {
            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }

        stage.setTitle("Pomodoro Tracker");
        stage.setMaximized(true);
        stage.setResizable(true);

        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (KeyCode.F11.equals(event.getCode())) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });

        CSSFX.start();


        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), content);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        Platform.runLater(fadeIn::play);
    }

    public static void main(String[] args) {
        launch();
    }
}