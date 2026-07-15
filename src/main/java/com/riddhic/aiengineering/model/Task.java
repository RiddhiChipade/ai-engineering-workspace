package com.riddhic.aiengineering.model;

import com.riddhic.aiengineering.enums.Priority;
import com.riddhic.aiengineering.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class Task {
    private Long id;

    @NotBlank
    private String title;

    @Size(max = 500)
    private String description;

    private TaskStatus status;

    @NotBlank
    private Priority priority;

    public Task(Long id, TaskStatus status, String title, String description, Priority priority) {
        this.id = id;
        this.status = status;
        this.title = title;
        this.description = description;
        this.priority = priority;
    }

    public Long getId() {
        return id;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public Priority getPriority() {
        return priority;
    }
}
