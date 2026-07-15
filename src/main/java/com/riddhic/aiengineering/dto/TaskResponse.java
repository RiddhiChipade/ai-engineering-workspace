package com.riddhic.aiengineering.dto;

import com.riddhic.aiengineering.enums.Priority;
import com.riddhic.aiengineering.enums.TaskStatus;

public class TaskResponse {
    private Long id;

    private String title;

    private String description;

    private TaskStatus status;

    private Priority priority;
}
