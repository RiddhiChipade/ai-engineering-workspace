package com.riddhic.aiengineering.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riddhic.aiengineering.dto.PaginatedResponse;
import com.riddhic.aiengineering.dto.TaskResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The allowlisted read-only functions exposed to the assistant model. */
@Component
public class TaskAssistantToolExecutor {
    private static final int RESULT_LIMIT = 100;
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    public TaskAssistantToolExecutor(TaskService taskService, ObjectMapper objectMapper) {
        this.taskService = taskService;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> definitions() {
        Map<String, Object> emptyParameters = Map.of(
                "type", "object", "properties", Map.of(), "required", List.of(), "additionalProperties", false);
        return List.of(
                Map.of("type", "function", "name", "getPendingTasks",
                        "description", "Read up to 100 tasks assigned to the authenticated user that are not complete.",
                        "parameters", emptyParameters, "strict", true),
                Map.of("type", "function", "name", "getOverdueTasks",
                        "description", "Read up to 100 incomplete tasks assigned to the authenticated user with a due date before today.",
                        "parameters", emptyParameters, "strict", true));
    }

    public Map<String, Object> execute(String name, String arguments) {
        if (!"getPendingTasks".equals(name) && !"getOverdueTasks".equals(name))
            return Map.of("error", "Tool is not allowed");
        try {
            JsonNode args = objectMapper.readTree(arguments == null || arguments.isBlank() ? "{}" : arguments);
            if (!args.isObject() || args.size() != 0) return Map.of("error", "This tool accepts no arguments");
        } catch (Exception ex) {
            return Map.of("error", "Tool arguments must be an empty JSON object");
        }

        PaginatedResponse<TaskResponse> result = "getPendingTasks".equals(name)
                ? taskService.getPendingTasksForCurrentUser(0, RESULT_LIMIT)
                : taskService.getOverdueTasksForCurrentUser(0, RESULT_LIMIT);
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (TaskResponse task : result.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", task.getId());
            item.put("title", task.getTitle());
            item.put("description", task.getDescription());
            item.put("status", task.getStatus());
            item.put("priority", task.getPriority());
            item.put("dueDate", task.getDueDate());
            tasks.add(item);
        }
        return Map.of("tasks", tasks, "count", result.getContent().size(),
                "hasMore", result.getTotalElements() > result.getContent().size());
    }
}
