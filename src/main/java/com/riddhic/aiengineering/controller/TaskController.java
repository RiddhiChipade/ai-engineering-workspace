package com.riddhic.aiengineering.controller;

import com.riddhic.aiengineering.dto.TaskResponse;
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
    public List<Task> getTask() {
        return taskService.getallTasks();
    }

    @PostMapping
    public TaskResponse createTask(@Valid @RequestBody Task task) {
        return taskService.createTask(task);
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(@PathVariable String id, @Valid @RequestBody Task task) {
        return taskService.updateTask(id, task);
    }

    @DeleteMapping("/{id}")
    public Task deleteTask(@PathVariable String id) {
        return taskService.deleteTask(id);
    }


    @GetMapping("/{id}")
    public Task getTaskById(@PathVariable String id) {
        return taskService.getTaskbyId(id);
    }

}
