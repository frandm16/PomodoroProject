package com.frandm.studytracker.ui.views.logs;

import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.core.Logger;
import com.frandm.studytracker.controllers.TrackerController;
import com.frandm.studytracker.models.Session;
import com.frandm.studytracker.core.NotificationManager;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;
import java.util.List;

public class LogsController {

    private final TrackerController mainController;
    private HistoryTab historyTab;
    private FocusTab focusTab;
    private CalendarTab calendarTab;

    private Session sessionToDelete;
    private Session sessionToEdit;
    private int editRating = 0;
    private java.util.Map<String, String> tagColors = new java.util.HashMap<>();

    public LogsController(TrackerController mainController) {
        this.mainController = mainController;
    }

    public void setViews(HistoryTab h, FocusTab f, CalendarTab c) {
        this.historyTab = h;
        this.focusTab = f;
        this.calendarTab = c;
    }

    public void requestDelete(Session s) {
        this.sessionToDelete = s;
        mainController.toggleConfirmDelete();
    }

    public void executeDeletion() {
        if (sessionToDelete != null) {
            try {
                ApiClient.deleteSession(sessionToDelete.getId());
            } catch (Exception e) {
                Logger.error("Error deleting session", e);
                mainController.showBackendOperationError("Session could not be deleted", e);
                return;
            }
            refreshAll();
            sessionToDelete = null;
            NotificationManager.show("Session Deleted", "Success", NotificationManager.NotificationType.SUCCESS);
        }
    }

    public void requestEdit(Session s) {
        this.sessionToEdit = s;
        this.editRating = s.getRating();
        mainController.openEditSession();
    }

    public void populateEditForm(TextField title, TextArea desc, ComboBox<String> tagCombo, ComboBox<String> taskCombo, List<FontIcon> starNodes) {
        if (sessionToEdit == null) return;

        title.setText(sessionToEdit.getTitle());
        desc.setText(sessionToEdit.getDescription());
        tagCombo.getItems().clear();
        taskCombo.getItems().clear();

        tagColors.clear();
        try {
            ApiClient.getTags().forEach(t -> {
                String name = (String) t.get("name");
                String color = (String) t.get("color");
                tagCombo.getItems().add(name);
                tagColors.put(name, color);
            });
        } catch (Exception e) {
            if (ApiClient.isConfigured()) {
                Logger.error("Error loading tags", e);
            }
        }
        tagCombo.setValue(sessionToEdit.getTag());

        updateTaskCombo(taskCombo, sessionToEdit.getTag());
        taskCombo.setValue(sessionToEdit.getTask());

        tagCombo.setOnAction(_ -> {
            String selectedTag = tagCombo.getValue();
            if (selectedTag != null) {
                updateTaskCombo(taskCombo, selectedTag);
                taskCombo.setValue(null);
            }
        });

        updateStarsUI(starNodes);
    }

    private void updateTaskCombo(ComboBox<String> taskCombo, String tagName) {
        taskCombo.getItems().clear();
        try {
            ApiClient.getTasks(tagName).forEach(t -> taskCombo.getItems().add((String) t.get("name")));
        } catch (Exception e) {
            if (ApiClient.isConfigured()) {
                Logger.error("Error loading tasks", e);
            }
        }
    }

    public void handleStarClick(int rating, List<FontIcon> starNodes) {
        this.editRating = (rating == this.editRating) ? 0 : rating;
        updateStarsUI(starNodes);
    }

    public void updateStarsUI(List<FontIcon> starNodes) {
        for (int i = 0; i < starNodes.size(); i++) {
            starNodes.get(i).getStyleClass().removeAll("selectedStar", "unselectedStar");
            if (i < editRating) {
                starNodes.get(i).getStyleClass().add("selectedStar");
            } else {
                starNodes.get(i).getStyleClass().add("unselectedStar");
            }
        }
    }

    public void setupEditStars(HBox container, List<FontIcon> starNodes) {
        container.getChildren().clear();
        starNodes.clear();
        for (int i = 1; i <= 5; i++) {
            int val = i;
            FontIcon star = new FontIcon("fas-star");
            star.setIconSize(30);
            star.setCursor(javafx.scene.Cursor.HAND);

            star.setOnMouseClicked(_ -> handleStarClick(val, starNodes));

            starNodes.add(star);
            container.getChildren().add(star);
        }
    }

    public void saveEdit(String title, String desc, String tagName, String taskName) {
        if (sessionToEdit != null) {
            String tagColor = tagColors.getOrDefault(tagName, sessionToEdit.getTagColor());
            try {
                ApiClient.patchSession(
                        sessionToEdit.getId(),
                        tagName,
                        tagColor,
                        taskName,
                        title,
                        desc,
                        editRating
                );
            } catch (Exception e) {
                Logger.error("Error updating session", e);
                mainController.showBackendOperationError("Session could not be updated", e);
                return;
            }
            refreshAll();
            sessionToEdit = null;
            NotificationManager.show("Success", "Session updated", NotificationManager.NotificationType.SUCCESS);
        }
    }

    public void refreshSessionData() {
        if (historyTab != null) historyTab.reload();
        if (calendarTab != null) {
            calendarTab.loadWeekSessions();
            calendarTab.refresh();
        }
    }

    public void refreshAll() {
        refreshSessionData();
        if (focusTab != null) focusTab.refreshFocusAreasGrid();
    }

    public void playTask(String tag, String task) {
        mainController.playScheduleSession(tag, task);
        mainController.switchToTimer();
    }

    public String getTheme() {
        return mainController.getCurrentTheme();
    }

    public void updateIcon(Button button, String style, String iconCode, String tooltipText) {
        mainController.updateIcon(button, style, iconCode, tooltipText);
    }

    public void openAddTagOverlay() {
        mainController.switchToTimer();
        mainController.toggleSetup();
    }

    public void openDeleteTagOverlay(long tagId) {
        mainController.openConfirmDeleteTag(tagId);
    }
}
