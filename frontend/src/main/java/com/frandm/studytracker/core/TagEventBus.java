package com.frandm.studytracker.core;

import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TagEventBus {

    private static volatile TagEventBus instance;

    public enum Type { CREATED, UPDATED, DELETED, ARCHIVE_TOGGLED, FAVORITE_TOGGLED }

    public record TagEvent(Type type, Long tagId, String tagName) {}

    public interface Listener {
        void onTagChanged(TagEvent event);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private TagEventBus() {}

    public static TagEventBus getInstance() {
        if (instance == null) {
            synchronized (TagEventBus.class) {
                if (instance == null) {
                    instance = new TagEventBus();
                }
            }
        }
        return instance;
    }

    public void subscribe(Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void publish(Type type, Long tagId, String tagName) {
        TagEvent event = new TagEvent(type, tagId, tagName);
        for (Listener listener : listeners) {
            Platform.runLater(() -> listener.onTagChanged(event));
        }
    }

    public void clear() {
        listeners.clear();
    }
}
