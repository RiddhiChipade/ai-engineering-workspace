package com.riddhic.aiengineering.controller;

import com.riddhic.aiengineering.dto.TaskRequest;
import com.riddhic.aiengineering.dto.TaskResponse;
import com.riddhic.aiengineering.dto.PaginatedResponse;
import com.riddhic.aiengineering.enums.TaskStatus;
import com.riddhic.aiengineering.enums.Priority;
import com.riddhic.aiengineering.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/tasks")
@Tag(name = "Tasks", description = "Task CRUD and filtering operations")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(
            summary = "List tasks",
            description = "Returns paginated tasks with optional search, status, priority, and assigned user filters."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully",
                   content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginatedResponse.class)))
    })
    @GetMapping
    public PaginatedResponse<TaskResponse> getTasks(
            @Parameter(description = "Search term for task title or description") @RequestParam(required = false) String search,
            @Parameter(description = "Task status filter", example = "TODO") @RequestParam(required = false) String status,
            @Parameter(description = "Priority filter", example = "HIGH") @RequestParam(required = false) String priority,
            @Parameter(description = "Assigned user ID filter") @RequestParam(required = false) Long assignedUserId,
            @Parameter(description = "Page number", example = "0") @Min(0) @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1 to 100)", example = "10") @Min(1) @Max(100) @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction in the form field,dir (for example createdAt,desc)") @RequestParam(required = false) String sort,
            @Parameter(description = "Sort field", example = "id") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction", example = "asc") @RequestParam(defaultValue = "asc") String sortDirection) {
        
        // support combined `sort=field,dir` (e.g. sort=createdAt,desc)
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            if (parts.length >= 1 && parts[0] != null && !parts[0].isBlank()) {
                sortBy = parts[0];
            }
            if (parts.length >= 2 && parts[1] != null && !parts[1].isBlank()) {
                sortDirection = parts[1];
            }
            if (parts.length > 2) throw new IllegalArgumentException("sort must use the form field,direction");
        }

        TaskStatus taskStatus = null;
        if (status != null && !status.isBlank()) {
            taskStatus = TaskStatus.valueOf(status.toUpperCase());
        }
        
        Priority taskPriority = null;
        if (priority != null && !priority.isBlank()) {
            taskPriority = Priority.valueOf(priority.toUpperCase());
        }
        
        return taskService.getFilteredTasks(search, taskStatus, taskPriority, assignedUserId, page, size, sortBy, sortDirection);
    }

    @Operation(summary = "Get task by ID", description = "Returns a single task by its identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @GetMapping("/{id}")
    public TaskResponse getTaskById(@Parameter(description = "Task identifier") @PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    @Operation(summary = "Create task", description = "Creates a new task with optional assignee assignment.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    @PostMapping
    public TaskResponse createTask(@Valid @RequestBody TaskRequest request) {
        return taskService.createTask(request);
    }

    @Operation(summary = "Update task", description = "Updates an existing task and optionally reassigns it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @PutMapping("/{id}")
    public TaskResponse updateTask(
            @Parameter(description = "Task identifier") @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {

        return taskService.updateTask(id, request);
    }

    @Operation(summary = "Delete task", description = "Deletes a task by identifier and returns the deleted task.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @DeleteMapping("/{id}")
    public TaskResponse deleteTask(@Parameter(description = "Task identifier") @PathVariable Long id) {
        return taskService.deleteTask(id);
    }
}
