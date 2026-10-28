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
import com.riddhic.aiengineering.service.TaskEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.time.LocalDate;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;


@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskEventProducer taskEventProducer;
    private final Clock clock;

    @Autowired
    public TaskService(TaskRepository taskRepository, UserRepository userRepository, TaskEventProducer taskEventProducer, Clock clock) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskEventProducer = taskEventProducer;
        this.clock = clock;
    }

    public TaskService(TaskRepository taskRepository, UserRepository userRepository, TaskEventProducer taskEventProducer) {
        this(taskRepository, userRepository, taskEventProducer, Clock.systemUTC());
    }

    public List<TaskResponse> getAllTasks() {
        log.info("Fetching all tasks");
        return taskRepository.findAll().stream().map(TaskResponse::new).toList();
    }

    public PaginatedResponse<TaskResponse> getPendingTasksForCurrentUser(int page, int size) {
        User caller = authenticatedCaller();
        validateToolPage(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Task> taskPage = taskRepository.findByAssignedToIdAndStatusIn(caller.getId(),
                List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS), pageable);
        return new PaginatedResponse<>(taskPage.getContent().stream().map(TaskResponse::new).toList(), page, size,
                taskPage.getTotalPages(), taskPage.getTotalElements(), taskPage.isLast());
    }

    public PaginatedResponse<TaskResponse> getOverdueTasksForCurrentUser(int page, int size) {
        User caller = authenticatedCaller();
        validateToolPage(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "dueDate"));
        Page<Task> taskPage = taskRepository.findByAssignedToIdAndDueDateBeforeAndStatusNot(
                caller.getId(), LocalDate.now(clock), TaskStatus.DONE, pageable);
        return new PaginatedResponse<>(taskPage.getContent().stream().map(TaskResponse::new).toList(), page, size,
                taskPage.getTotalPages(), taskPage.getTotalElements(), taskPage.isLast());
    }

    private User authenticatedCaller() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal()))
            throw new AccessDeniedException("Authentication required");
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user no longer exists"));
    }

    private void validateToolPage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page must be >= 0 and size must be between 1 and 100");
    }

    @Cacheable(value = "task-query", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication() == null ? 'anonymous:' + T(java.util.Objects).toString(#search) + ':' + T(java.util.Objects).toString(#status) + ':' + T(java.util.Objects).toString(#priority) + ':' + T(java.util.Objects).toString(#assignedUserId) + ':' + #page + ':' + #size + ':' + T(java.util.Objects).toString(#sortBy) + ':' + T(java.util.Objects).toString(#sortDirection) : T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + ':' + T(java.util.Objects).toString(#search) + ':' + T(java.util.Objects).toString(#status) + ':' + T(java.util.Objects).toString(#priority) + ':' + T(java.util.Objects).toString(#assignedUserId) + ':' + #page + ':' + #size + ':' + T(java.util.Objects).toString(#sortBy) + ':' + T(java.util.Objects).toString(#sortDirection)")
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

        Sort.Direction direction;
        try { direction = Sort.Direction.fromString(sortDirection); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("sortDirection must be asc or desc"); }
        String validSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
        if (!List.of("id", "title", "status", "priority", "createdAt", "updatedAt").contains(validSortBy))
            throw new IllegalArgumentException("Unsupported sort field: " + validSortBy);
        if (page < 0) throw new IllegalArgumentException("page must be zero or greater");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be between 1 and 100");
        Long callerId = currentUserId();
        if (callerId != null) {
            if (assignedUserId != null && !assignedUserId.equals(callerId)) throw new AccessDeniedException("Cannot view another user's tasks");
            assignedUserId = callerId;
        }
        int validPage = page;
        int validSize = size;

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
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException("Task with id " + id + " not found"));
        assertCanAccess(task);
        return new TaskResponse(task);
    }

    @CacheEvict(value = "task-query", allEntries = true)
    public TaskResponse createTask(TaskRequest request) {

        Task task = new Task();

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setStatus(request.getStatus() == null ? TaskStatus.TODO : request.getStatus());
        task.setDueDate(request.getDueDate());

        Long requestedAssignee = request.getAssignedToId() == null && currentUserId() != null ? currentUserId() : request.getAssignedToId();
        if (requestedAssignee != null) {
            if (currentUserId() != null && !requestedAssignee.equals(currentUserId())) throw new AccessDeniedException("Cannot assign tasks to another user");
            User user = userRepository.findById(requestedAssignee)
                    .orElseThrow(() -> new UserNotFoundException("User with id " + requestedAssignee + " not found"));
            task.setAssignedTo(user);
        }

        task = taskRepository.save(task);
        log.info("Task created with id: " + task.getId());

        // publish CREATED event after successful DB save
        try {
            taskEventProducer.publishTaskEvent(new com.riddhic.aiengineering.model.TaskEvent(com.riddhic.aiengineering.model.TaskEventType.CREATED, task.getId(), task.getTitle()));
        } catch (Exception ex) {
            log.error("Error publishing task created event for taskId={}", task.getId(), ex);
        }

        return new TaskResponse(task);
    }

    @CacheEvict(value = "task-query", allEntries = true)
    public TaskResponse updateTask(Long id, TaskRequest request) {

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + id + " not found"));
        assertCanAccess(task);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        task.setDueDate(request.getDueDate());

        if (request.getAssignedToId() != null) {
            if (currentUserId() != null && !request.getAssignedToId().equals(currentUserId())) throw new AccessDeniedException("Cannot assign tasks to another user");
            User user = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new UserNotFoundException("User with id " + request.getAssignedToId() + " not found"));
            task.setAssignedTo(user);
        } else if (currentUserId() == null) {
            task.setAssignedTo(null);
        }

        task = taskRepository.save(task);
        log.info("Task updated with id: " + task.getId());

        // publish UPDATED event
        try {
            taskEventProducer.publishTaskEvent(new com.riddhic.aiengineering.model.TaskEvent(com.riddhic.aiengineering.model.TaskEventType.UPDATED, task.getId(), task.getTitle()));
        } catch (Exception ex) {
            log.error("Error publishing task updated event for taskId={}", task.getId(), ex);
        }

        return new TaskResponse(task);
    }

    @CacheEvict(value = "task-query", allEntries = true)
    public TaskResponse deleteTask(Long id) {

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + id + " not found"));
        assertCanAccess(task);

        taskRepository.delete(task);
        log.info("Task deleted with id: " + id);

        // publish DELETED event
        try {
            taskEventProducer.publishTaskEvent(new com.riddhic.aiengineering.model.TaskEvent(com.riddhic.aiengineering.model.TaskEventType.DELETED, task.getId(), task.getTitle()));
        } catch (Exception ex) {
            log.error("Error publishing task deleted event for taskId={}", task.getId(), ex);
        }

        return new TaskResponse(task);
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())
                || authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) return null;
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user no longer exists")).getId();
    }

    private void assertCanAccess(Task task) {
        Long callerId = currentUserId();
        if (callerId != null && (task.getAssignedTo() == null || !callerId.equals(task.getAssignedTo().getId())))
            throw new AccessDeniedException("Cannot access another user's task");
    }
}
