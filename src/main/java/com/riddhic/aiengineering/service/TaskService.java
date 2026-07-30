package com.riddhic.aiengineering.service;

import com.riddhic.aiengineering.dto.TaskRequest;
import com.riddhic.aiengineering.dto.TaskResponse;
import com.riddhic.aiengineering.enums.TaskStatus;
import com.riddhic.aiengineering.exception.TaskNotFoundException;
import com.riddhic.aiengineering.exception.UserNotFoundException;
import com.riddhic.aiengineering.model.Task;
import com.riddhic.aiengineering.model.User;
import com.riddhic.aiengineering.repository.TaskRepository;
import com.riddhic.aiengineering.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;


    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public List<TaskResponse> getAllTasks() {
        log.info("Fetching all tasks");
        return taskRepository.findAll().stream().map(TaskResponse::new).toList();
    }

    public TaskResponse getTaskById(Long id) {
        log.info("Searching for task with id: " + id);
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

        if (request.getAssignedToId() != null) {
            User user = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new UserNotFoundException("User with id " + request.getAssignedToId() + " not found"));
            task.setAssignedTo(user);
        }

        task = taskRepository.save(task);
        log.info("Task created with id: " + task.getId());
        return new TaskResponse(task);
    }

    public TaskResponse updateTask(Long id, TaskRequest request) {

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + id + " not found"));

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());

        if (request.getAssignedToId() != null) {
            User user = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new UserNotFoundException("User with id " + request.getAssignedToId() + " not found"));
            task.setAssignedTo(user);
        } else {
            task.setAssignedTo(null);
        }

        task = taskRepository.save(task);
        log.info("Task updated with id: " + task.getId());
        return new TaskResponse(task);
    }

    public TaskResponse deleteTask(Long id) {

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + id + " not found"));

        taskRepository.delete(task);
        log.info("Task deleted with id: " + id);

        return new TaskResponse(task);
    }
}