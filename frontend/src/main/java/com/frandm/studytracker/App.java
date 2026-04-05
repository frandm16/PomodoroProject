package com.frandm.studytracker;

import atlantafx.base.theme.PrimerDark;
import com.frandm.studytracker.controllers.TrackerController;
import com.frandm.studytracker.core.NotificationManager;
import com.frandm.studytracker.core.ShortcutManager;
import fr.brouillard.oss.cssfx.CSSFX;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import java.net.URL;
import java.util.Objects;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        Font.loadFont(getClass().getResourceAsStream("/com/frandm/studytracker/fonts/SF-Pro-Display-Regular.otf"), 12);
        Font.loadFont(getClass().getResourceAsStream("/com/frandm/studytracker/fonts/Excalifont-Regular.otf"), 12);
        Font.loadFont(getClass().getResourceAsStream("/com/frandm/studytracker/fonts/SpaceGrotesk-Regular.ttf"), 12);

        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");

        Stage finalStage;
        Parent root;
        TrackerController controller;

        if (isWindows) {
            MainStage mainStage = new MainStage();
            finalStage = mainStage;
            root = mainStage.getScene().getRoot();
            controller = mainStage.getLoader().getController();
        } else {
            finalStage = stage;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/frandm/studytracker/fxml/main_view.fxml"));
            root = loader.load();
            controller = loader.getController();
            controller.titleBar.setVisible(false);
            controller.titleBar.setManaged(false);
            finalStage.setScene(new Scene(root));
        }

        finalStage.setOnCloseRequest(event -> {
            if (controller != null && controller.isTimerActive()) {
                NotificationManager.show(
                        "Close blocked",
                        "You must finish your current session to close the app.",
                        NotificationManager.NotificationType.WARNING
                );
                event.consume();
            } else {
                Platform.exit();
                System.exit(0);
            }
        });

        if (!isWindows && controller != null && controller.closeBtn != null) {
            controller.closeBtn.setOnAction(_ -> {
                finalStage.fireEvent(new WindowEvent(finalStage, WindowEvent.WINDOW_CLOSE_REQUEST));
            });
        }

        Scene scene = finalStage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            finalStage.setScene(scene);
        }

        scene.setFill(Color.TRANSPARENT);
        String mainStyles = Objects.requireNonNull(getClass().getResource("/com/frandm/studytracker/css/styles.css")).toExternalForm();
        scene.getStylesheets().add(mainStyles);

        finalStage.setTitle("StudyZen");
        finalStage.setResizable(true);

        if (controller != null && controller.titleBar != null) {
            finalStage.fullScreenProperty().addListener((_, _, isFullScreen) -> {
                controller.titleBar.setVisible(!isFullScreen);
                controller.titleBar.setManaged(!isFullScreen);
            });
        }

        ShortcutManager shortcutManager = new ShortcutManager();
        if (controller != null) {
            controller.setShortcutManager(shortcutManager);
            controller.setFullscreenToggleAction(() -> finalStage.setFullScreen(!finalStage.isFullScreen()));
            shortcutManager.setActionHandler("open_timer_tab", controller::switchToTimer);
            shortcutManager.setActionHandler("open_planner_tab", controller::openPlannerPanel);
            shortcutManager.setActionHandler("open_stats_tab", controller::openStatsPanel);
            shortcutManager.setActionHandler("open_history_tab", controller::openHistoryPanel);
            shortcutManager.setActionHandler(
                    "toggle_start_pause", controller::switchToTimer, () -> Platform.runLater(controller::toggleStartPauseAction)
            );
            shortcutManager.setActionHandler("skip_session", controller::triggerSkipAction);
            shortcutManager.setActionHandler("finish_session", controller::triggerFinishAction);
            shortcutManager.setActionHandler("toggle_settings", controller::toggleSettings);
            shortcutManager.setActionHandler("open_setup", controller::openSetupAction);
            shortcutManager.setActionHandler("toggle_fullscreen", controller::toggleFullscreenAction);
            shortcutManager.setActionHandler("toggle_shortcut_menu", controller::toggleShortcutMenu);
            shortcutManager.configureShortcutMenuState(controller::isShortcutMenuVisible, controller::closeShortcutMenu);
        }
        shortcutManager.install(scene);

        URL iconUrl = getClass().getResource("/com/frandm/studytracker/images/SZlogo.png");

        if (iconUrl != null) {
            finalStage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }

        finalStage.show();

        Platform.runLater(() -> {
            try {
                finalStage.setMaximized(true);
            } catch (Exception ignored) {
            }

            root.requestFocus();

            CSSFX.start();

            if (root instanceof Pane pane && !pane.getChildren().isEmpty()) {
                javafx.scene.Node content = pane.getChildren().getFirst();
                content.setOpacity(0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(800), content);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            }
        });
    }

    public static void main() {
        launch();
    }
}
