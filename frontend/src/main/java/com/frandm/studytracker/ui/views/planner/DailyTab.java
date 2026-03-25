package com.frandm.studytracker.ui.views.planner;

import atlantafx.base.theme.Styles;
import com.frandm.studytracker.controllers.PomodoroController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class DailyTab extends VBox {

    private final VBox deadlinesContainer = new VBox(10);
    private final VBox dayEventsContainer = new VBox(10);
    private final PomodoroController pomodoroController;
    private LocalDate currentDate = LocalDate.now();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("d MMM • HH:mm");

    public DailyTab(PomodoroController pomodoroController) {
        this.pomodoroController = pomodoroController;
        this.getStyleClass().add("daily-tab");
        VBox.setVgrow(this, Priority.ALWAYS);
        initLayout();
    }

    private void initLayout() {
        deadlinesContainer.getStyleClass().add("daily-container");
        dayEventsContainer.getStyleClass().add("daily-container");

        VBox content = new VBox(20);
        content.getStyleClass().add("daily-content-wrapper");
        content.setPadding(new Insets(15, 0, 0, 0));
        content.getChildren().addAll(
                createHeader("Deadlines"), deadlinesContainer,
                createHeader("Scheduled Events"), dayEventsContainer
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().addAll(Styles.FLAT, "planner-scroll-pane");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().add(scroll);
    }

    public void updateHeaderDate(LocalDate date) {
        this.currentDate = date;
    }

    public void refreshData(List<Map<String, Object>> scheduled, List<Map<String, Object>> deadlines) {
        fill(deadlinesContainer, deadlines, "No deadlines for this day.", this::createDeadlineRow);
        fill(dayEventsContainer, scheduled, "No events scheduled.", this::createEventRow);
    }

    public String getHeaderTitle() {
        String dayName = currentDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault());
        return dayName.substring(0, 1).toUpperCase() + dayName.substring(1) + ", " +
                currentDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()));
    }

    private void fill(VBox container, List<Map<String, Object>> data, String msg, Function<Map<String, Object>, Node> mapper) {
        container.getChildren().clear();
        if (data == null || data.isEmpty()) {
            Label empty = new Label(msg);
            empty.getStyleClass().add("empty-state-label");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            container.getChildren().add(empty);
        } else {
            for (Map<String, Object> item : data) {
                container.getChildren().add(mapper.apply(item));
            }
        }
    }

    private HBox createDeadlineRow(Map<String, Object> data) {
        HBox row = baseRow();
        row.getStyleClass().add("deadline-row");

        LocalDateTime due = parse(data.get("start_time"));
        long diff = ChronoUnit.DAYS.between(LocalDate.now(), due.toLocalDate());

        VBox info = new VBox(2);
        info.getStyleClass().add("row-info-container");

        Label title = new Label(String.valueOf(data.getOrDefault("title", "Untitled")));
        title.getStyleClass().add("row-title");

        Label sub = new Label(String.valueOf(data.getOrDefault("taskName", "General")));
        sub.getStyleClass().add(Styles.TEXT_MUTED);

        Label status = new Label(diff < 0 ? "Overdue " + Math.abs(diff) + " days" : (diff == 0 ? "Due Today" : "Due in " + diff + " days"));
        status.getStyleClass().add(diff < 0 ? Styles.DANGER : Styles.SUCCESS);
        status.getStyleClass().add(Styles.TEXT_SMALL);

        info.getChildren().addAll(title, sub, status);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_RIGHT);
        badges.getChildren().addAll(
                badge(due.format(DateTimeFormatter.ofPattern("HH:mm")), MaterialDesignC.CLOCK_OUTLINE, "badge-time"),
                badge(String.valueOf(data.getOrDefault("tagName", "")), null, "badge-tag")
        );

        row.getChildren().addAll(new FontIcon(MaterialDesignP.PENCIL), info, spacer, badges);
        return row;
    }

    private HBox createEventRow(Map<String, Object> data) {
        HBox row = baseRow();
        row.getStyleClass().add("event-row");

        String tagColor = String.valueOf(data.getOrDefault("tagColor", "#3b82f6"));
        row.setStyle("-fx-border-color: transparent transparent transparent " + tagColor + "; -fx-border-width: 0 0 0 4;");

        LocalDateTime start = parse(data.get("start_time"));
        VBox info = new VBox(2);
        info.getStyleClass().add("row-info-container");

        Label title = new Label(String.valueOf(data.getOrDefault("title", "Event")));
        title.getStyleClass().add("row-title");

        Label time = new Label(start.format(DateTimeFormatter.ofPattern("HH:mm")));
        time.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        info.getChildren().addAll(title, time);
        row.getChildren().add(info);
        return row;
    }

    private HBox baseRow() {
        HBox row = new HBox(15);
        row.getStyleClass().add("planner-row-base");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));
        return row;
    }

    private Label badge(String text, MaterialDesignC icon, String customClass) {
        if (text == null || text.isEmpty() || text.equals("null")) return new Label();
        Label l = new Label(text);
        if (icon != null) l.setGraphic(new FontIcon(icon));
        l.getStyleClass().addAll("planner-badge", customClass);
        return l;
    }

    private Label createHeader(String title) {
        Label header = new Label(title.toUpperCase());
        header.getStyleClass().add("section-header");
        return header;
    }

    private LocalDateTime parse(Object val) {
        if (val instanceof LocalDateTime) return (LocalDateTime) val;
        return LocalDateTime.now();
    }
}