package com.riddhic.aiengineering.model;

import java.time.Instant;
import java.util.UUID;

public class TaskEvent {

    private String eventId;
    private TaskEventType eventType;
    private Long taskId;
    private Instant timestamp;
    private String title;

    public TaskEvent() {
    }

    public TaskEvent(TaskEventType eventType, Long taskId, String title) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.taskId = taskId;
        this.timestamp = Instant.now();
        this.title = title;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public TaskEventType getEventType() {
        return eventType;
    }

    public void setEventType(TaskEventType eventType) {
        this.eventType = eventType;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
