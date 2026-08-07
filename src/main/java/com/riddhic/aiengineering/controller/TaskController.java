package com.riddhic.aiengineering.controller;

import com.riddhic.aiengineering.dto.TaskRequest;
import com.riddhic.aiengineering.dto.TaskResponse;
import com.riddhic.aiengineering.dto.PaginatedResponse;
import com.riddhic.aiengineering.enums.TaskStatus;
import com.riddhic.aiengineering.enums.Priority;
import com.riddhic.aiengineering.model.Task;
import com.riddhic.aiengineering.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public PaginatedResponse<TaskResponse> getTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assignedUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort, // accepts "field,dir"
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        
        // support combined `sort=field,dir` (e.g. sort=createdAt,desc)
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            if (parts.length >= 1 && parts[0] != null && !parts[0].isBlank()) {
                sortBy = parts[0];
            }
            if (parts.length >= 2 && parts[1] != null && !parts[1].isBlank()) {
                sortDirection = parts[1];
            }
        }

        TaskStatus taskStatus = null;
        if (status != null && !status.isBlank()) {
            taskStatus = TaskStatus.valueOf(status.toUpperCase());
        }
        
        Priority taskPriority = null;
        if (priority != null && !priority.isBlank()) {
            taskPriority = Priority.valueOf(priority.toUpperCase());
        }
        
        return taskService.getFilteredTasks(taskStatus, taskPriority, assignedUserId, page, size, sortBy, sortDirection);
    }

    @GetMapping("/{id}")
    public TaskResponse getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    @PostMapping
    public TaskResponse createTask(@Valid @RequestBody TaskRequest request) {
        return taskService.createTask(request);
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {

        return taskService.updateTask(id, request);
    }

    @DeleteMapping("/{id}")
    public TaskResponse deleteTask(@PathVariable Long id) {
        return taskService.deleteTask(id);
    }
}