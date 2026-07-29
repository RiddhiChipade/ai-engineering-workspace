package com.riddhic.aiengineering.service;

import com.riddhic.aiengineering.dto.TaskRequest;
import com.riddhic.aiengineering.dto.TaskResponse;
import com.riddhic.aiengineering.enums.TaskStatus;
import com.riddhic.aiengineering.exception.TaskNotFoundException;
import com.riddhic.aiengineering.model.Task;
import com.riddhic.aiengineering.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private final TaskRepository taskRepository;


    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<TaskResponse> getAllTasks() {
        TaskService.log.info("Fetching all tasks");
        return taskRepository.findAll().stream().map(TaskResponse::new).toList();
    }

    public TaskResponse getTaskById(Long id) {
        TaskService.log.info("Searching for task with id: " + id);
        return new TaskResponse(taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + id + " not found"))
        );
    }

    public TaskResponse createTask(TaskRequest request) {

        Task task = new Task();

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setStatus(TaskStatus.TODO);

        taskRepository.save(task);
        TaskService.log.info("Task created with id: " + task.getId());
        return new TaskResponse(task);
    }

    public TaskResponse updateTask(Long id, TaskRequest request) {

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + id + " not found"));

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());

        taskRepository.save(task);
        TaskService.log.info("Task updated with id: " + task.getId());
        return new TaskResponse(task);
    }

    public TaskResponse deleteTask(Long id) {

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + id + " not found"));

        taskRepository.delete(task);
        TaskService.log.info("Task deleted with id: " + id);

        return new TaskResponse(task);
    }
}