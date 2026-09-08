package com.riddhic.aiengineering.service;

import com.riddhic.aiengineering.dto.TaskRequest;
import com.riddhic.aiengineering.dto.TaskResponse;
import com.riddhic.aiengineering.dto.PaginatedResponse;
import com.riddhic.aiengineering.enums.TaskStatus;
import com.riddhic.aiengineering.enums.Priority;
import com.riddhic.aiengineering.exception.TaskNotFoundException;
import com.riddhic.aiengineering.exception.UserNotFoundException;
import com.riddhic.aiengineering.model.Task;
import com.riddhic.aiengineering.model.User;
import com.riddhic.aiengineering.repository.TaskRepository;
import com.riddhic.aiengineering.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    public PaginatedResponse<TaskResponse> getFilteredTasks(
            String search,
            TaskStatus status,
            Priority priority,
            Long assignedUserId,
            int page,
            int size,
            String sortBy,
            String sortDirection) {
        
        log.info("Fetching filtered tasks - search: {}, status: {}, priority: {}, assignedUserId: {}", search, status, priority, assignedUserId);

        Sort.Direction direction = Sort.Direction.fromString(sortDirection.toUpperCase());
        String validSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
        int validPage = Math.max(0, page);
        int validSize = Math.max(1, size);

        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(direction, validSortBy));

        Page<Task> taskPage;

        boolean hasSearch = search != null && !search.isBlank();

        if (hasSearch) {
            if (status != null && priority != null && assignedUserId != null) {
                taskPage = taskRepository.searchByTitleOrDescriptionAndStatusAndPriorityAndAssignedToId(search, status, priority, assignedUserId, pageable);
            } else if (status != null && priority != null) {
                taskPage = taskRepository.searchByTitleOrDescriptionAndStatusAndPriority(search, status, priority, pageable);
            } else if (status != null && assignedUserId != null) {
                taskPage = taskRepository.searchByTitleOrDescriptionAndStatusAndAssignedToId(search, status, assignedUserId, pageable);
            } else if (priority != null && assignedUserId != null) {
                taskPage = taskRepository.searchByTitleOrDescriptionAndPriorityAndAssignedToId(search, priority, assignedUserId, pageable);
            } else if (status != null) {
                taskPage = taskRepository.searchByTitleOrDescriptionAndStatus(search, status, pageable);
            } else if (priority != null) {
                taskPage = taskRepository.searchByTitleOrDescriptionAndPriority(search, priority, pageable);
            } else if (assignedUserId != null) {
                taskPage = taskRepository.searchByTitleOrDescriptionAndAssignedToId(search, assignedUserId, pageable);
            } else {
                taskPage = taskRepository.searchByTitleOrDescription(search, pageable);
            }
        } else {
            if (status != null && priority != null && assignedUserId != null) {
                taskPage = taskRepository.findByStatusAndPriorityAndAssignedToId(status, priority, assignedUserId, pageable);
            } else if (status != null && priority != null) {
                taskPage = taskRepository.findByStatusAndPriority(status, priority, pageable);
            } else if (status != null && assignedUserId != null) {
                taskPage = taskRepository.findByStatusAndAssignedToId(status, assignedUserId, pageable);
            } else if (priority != null && assignedUserId != null) {
                taskPage = taskRepository.findByPriorityAndAssignedToId(priority, assignedUserId, pageable);
            } else if (status != null) {
                taskPage = taskRepository.findByStatus(status, pageable);
            } else if (priority != null) {
                taskPage = taskRepository.findByPriority(priority, pageable);
            } else if (assignedUserId != null) {
                taskPage = taskRepository.findByAssignedToId(assignedUserId, pageable);
            } else {
                taskPage = taskRepository.findAll(pageable);
            }
        }

        List<TaskResponse> content = taskPage.getContent().stream().map(TaskResponse::new).toList();
        return new PaginatedResponse<>(content, validPage, validSize, taskPage.getTotalPages(), taskPage.getTotalElements(), taskPage.isLast());
    }

    public TaskResponse getTaskById(Long id) {
        log.info("Searching for task with id: {}", id);
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