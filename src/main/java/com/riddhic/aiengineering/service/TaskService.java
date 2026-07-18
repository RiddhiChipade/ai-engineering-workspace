package com.riddhic.aiengineering.service;

import com.riddhic.aiengineering.dto.TaskRequest;
import com.riddhic.aiengineering.dto.TaskResponse;
import com.riddhic.aiengineering.enums.TaskStatus;
import com.riddhic.aiengineering.exception.TaskNotFoundException;
import com.riddhic.aiengineering.model.Task;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class TaskService {

    private final List<Task> taskList = new ArrayList<>();
    private Long nextId = 1L;

    public List<TaskResponse> getAllTasks() {
        return taskList.stream().map(TaskResponse::new).toList();
    }

    public TaskResponse getTaskById(Long id) {
        System.out.println("Searching for task with id: " + id);
        return new TaskResponse(taskList.stream()
                .filter(task -> task.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + id + " not found"))
        );
    }

    public TaskResponse createTask(TaskRequest request) {

        Task task = new Task();

        task.setId(nextId++);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setStatus(TaskStatus.TODO);

        taskList.add(task);

        return new TaskResponse(task);
    }

    public TaskResponse updateTask(Long id, TaskRequest request) {

        Task task = getTaskById(id);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());

        return new TaskResponse(task);
    }

    public TaskResponse deleteTask(Long id) {

        Task task = getTaskById(id);

        taskList.remove(task);

        return new TaskResponse(task);
    }
}