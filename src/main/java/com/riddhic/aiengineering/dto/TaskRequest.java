package com.riddhic.aiengineering.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.riddhic.aiengineering.enums.Priority;
import com.riddhic.aiengineering.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public class TaskRequest {

    @NotBlank(message = "Title cannot be blank")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Priority is required")
    private Priority priority;

    @JsonProperty("assignedUserId")
    @Positive(message = "assignedUserId must be positive")
    private Long assignedToId;

    private TaskStatus status;

    private LocalDate dueDate;

    public TaskRequest() {
    }

    public TaskRequest(String title, String description, Priority priority, Long assignedToId) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.assignedToId = assignedToId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Long getAssignedToId() {
        return assignedToId;
    }

    public void setAssignedToId(Long assignedToId) {
        this.assignedToId = assignedToId;
    }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

}
