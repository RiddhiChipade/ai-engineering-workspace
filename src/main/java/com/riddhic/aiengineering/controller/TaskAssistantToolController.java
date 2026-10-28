package com.riddhic.aiengineering.controller;

import com.riddhic.aiengineering.dto.PaginatedResponse;
import com.riddhic.aiengineering.dto.TaskResponse;
import com.riddhic.aiengineering.service.TaskService;
import com.riddhic.aiengineering.service.TaskAssistantToolExecutor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** HTTP tool boundary for an authenticated assistant or model orchestrator. */
@RestController
@RequestMapping("/ai/tools")
@Validated
public class TaskAssistantToolController {
    private final TaskService taskService;
    private final TaskAssistantToolExecutor toolExecutor;

    public TaskAssistantToolController(TaskService taskService, TaskAssistantToolExecutor toolExecutor) {
        this.taskService = taskService;
        this.toolExecutor = toolExecutor;
    }

    @GetMapping
    public Map<String, Object> listTools() {
        return Map.of("tools", toolExecutor.definitions());
    }

    @PostMapping("/getPendingTasks")
    public PaginatedResponse<TaskResponse> getPendingTasks(
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @Max(100) @RequestParam(defaultValue = "100") int size) {
        return taskService.getPendingTasksForCurrentUser(page, size);
    }

    @PostMapping("/getOverdueTasks")
    public PaginatedResponse<TaskResponse> getOverdueTasks(
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @Max(100) @RequestParam(defaultValue = "100") int size) {
        return taskService.getOverdueTasksForCurrentUser(page, size);
    }
}
