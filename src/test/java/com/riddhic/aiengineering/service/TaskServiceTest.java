package com.riddhic.aiengineering.service;

import com.riddhic.aiengineering.dto.PaginatedResponse;
import com.riddhic.aiengineering.dto.TaskRequest;
import com.riddhic.aiengineering.dto.TaskResponse;
import com.riddhic.aiengineering.enums.Priority;
import com.riddhic.aiengineering.enums.TaskStatus;
import com.riddhic.aiengineering.model.Task;
import com.riddhic.aiengineering.model.User;
import com.riddhic.aiengineering.repository.TaskRepository;
import com.riddhic.aiengineering.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.riddhic.aiengineering.exception.UserNotFoundException;
import com.riddhic.aiengineering.exception.TaskNotFoundException;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private TaskRequest validTaskRequest;
    private Task savedTask;

    @BeforeEach
    void setUp() throws Exception {
        validTaskRequest = new TaskRequest(
                "Learn Spring Boot",
                "Study Spring Data JPA and REST APIs",
                Priority.HIGH,
                null
        );

        savedTask = new Task(
                1L,
                "Learn Spring Boot",
                "Study Spring Data JPA and REST APIs",
                TaskStatus.TODO,
                Priority.HIGH,
                null
        );

        LocalDateTime now = LocalDateTime.now();
        setFieldValue(savedTask, "createdAt", now);
        setFieldValue(savedTask, "updatedAt", now);
    }

    @Test
    void testCreateTaskSuccessfully() {
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskResponse response = taskService.createTask(validTaskRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Learn Spring Boot", response.getTitle());
        assertEquals("Study Spring Data JPA and REST APIs", response.getDescription());
        assertEquals(TaskStatus.TODO, response.getStatus());
        assertEquals(Priority.HIGH, response.getPriority());
        assertNull(response.getAssignedTo());
    }

    @Test
    void createTask_shouldAssignUserAndSaveTask() throws Exception {
        // Arrange
        TaskRequest request = new TaskRequest();
        request.setTitle("Learn Mockito");
        request.setDescription("Write unit tests");
        request.setPriority(Priority.HIGH);
        request.setAssignedToId(1L);

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        Task savedTask = new Task();
        savedTask.setId(10L);
        savedTask.setTitle("Learn Mockito");
        savedTask.setDescription("Write unit tests");
        savedTask.setPriority(Priority.HIGH);
        savedTask.setStatus(TaskStatus.TODO);
        savedTask.setAssignedTo(user);

        LocalDateTime now = LocalDateTime.now();
        setFieldValue(savedTask, "createdAt", now);
        setFieldValue(savedTask, "updatedAt", now);

        when(taskRepository.save(any(Task.class)))
                .thenReturn(savedTask);

        // Act
        TaskResponse response = taskService.createTask(request);

        // Assert
        assertEquals(10L, response.getId());
        assertEquals("Learn Mockito", response.getTitle());
        assertEquals(Priority.HIGH, response.getPriority());

        verify(userRepository).findById(1L);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_shouldThrowUserNotFoundException() {
        // Arrange
        TaskRequest request = new TaskRequest();
        request.setTitle("Task with Invalid User");
        request.setDescription("This should fail");
        request.setPriority(Priority.MEDIUM);
        request.setAssignedToId(99L);

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(UserNotFoundException.class, () -> {
            taskService.createTask(request);
        });

        // Verify
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void getTaskById_shouldReturnTaskWhenTaskExists() throws Exception {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(savedTask));

        // Act
        TaskResponse response = taskService.getTaskById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Learn Spring Boot", response.getTitle());
        assertEquals("Study Spring Data JPA and REST APIs", response.getDescription());
        assertEquals(TaskStatus.TODO, response.getStatus());
        assertEquals(Priority.HIGH, response.getPriority());

        // Verify
        verify(taskRepository).findById(1L);
    }

    @Test
    void getTaskById_shouldThrowTaskNotFoundExceptionWhenTaskDoesNotExist() {
        // Arrange
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class, () -> {
            taskService.getTaskById(99L);
        });

        assertEquals("Task with id 99 not found", exception.getMessage());

        // Verify
        verify(taskRepository).findById(99L);
    }

    @Test
    void updateTask_shouldUpdateTaskSuccessfully() throws Exception {
        // Arrange
        Long id = 1L;
        when(taskRepository.findById(id)).thenReturn(Optional.of(savedTask));

        TaskRequest request = new TaskRequest();
        request.setTitle("Learn Spring Boot - Updated");
        request.setDescription("Updated description");
        request.setPriority(Priority.MEDIUM);

        Task updatedTask = new Task();
        updatedTask.setId(id);
        updatedTask.setTitle(request.getTitle());
        updatedTask.setDescription(request.getDescription());
        updatedTask.setPriority(request.getPriority());
        updatedTask.setStatus(TaskStatus.TODO);
        updatedTask.setAssignedTo(null);

        LocalDateTime now = LocalDateTime.now();
        setFieldValue(updatedTask, "createdAt", now.minusDays(1));
        setFieldValue(updatedTask, "updatedAt", now);

        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        // Act
        TaskResponse response = taskService.updateTask(id, request);

        // Assert
        assertNotNull(response);
        assertEquals(id, response.getId());
        assertEquals("Learn Spring Boot - Updated", response.getTitle());
        assertEquals("Updated description", response.getDescription());
        assertEquals(Priority.MEDIUM, response.getPriority());

        // Verify
        verify(taskRepository).findById(id);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void updateTask_shouldThrowTaskNotFoundExceptionWhenTaskDoesNotExist() {
        // Arrange
        Long id = 99L;
        when(taskRepository.findById(id)).thenReturn(Optional.empty());

        TaskRequest request = new TaskRequest();
        request.setTitle("Non-existent");
        request.setDescription("Should not update");
        request.setPriority(Priority.LOW);

        // Act + Assert
        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class, () -> {
            taskService.updateTask(id, request);
        });

        assertEquals("Task with id 99 not found", exception.getMessage());

        // Verify
        verify(taskRepository).findById(id);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateTask_shouldAssignUserAndSaveTask() throws Exception {
        // Arrange
        Long taskId = 1L;
        Long userId = 5L;

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));

        User user = new User();
        user.setId(userId);
        user.setUsername("assigneduser");
        user.setEmail("assigned@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        TaskRequest request = new TaskRequest();
        request.setTitle("Learn Spring Boot - With User");
        request.setDescription("Updated with assignee");
        request.setPriority(Priority.HIGH);
        request.setAssignedToId(userId);

        Task updatedTask = new Task();
        updatedTask.setId(taskId);
        updatedTask.setTitle(request.getTitle());
        updatedTask.setDescription(request.getDescription());
        updatedTask.setPriority(request.getPriority());
        updatedTask.setStatus(TaskStatus.TODO);
        updatedTask.setAssignedTo(user);

        LocalDateTime now = LocalDateTime.now();
        setFieldValue(updatedTask, "createdAt", now.minusDays(1));
        setFieldValue(updatedTask, "updatedAt", now);

        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        // Act
        TaskResponse response = taskService.updateTask(taskId, request);

        // Assert
        assertNotNull(response);
        assertEquals(taskId, response.getId());
        assertEquals("Learn Spring Boot - With User", response.getTitle());
        assertEquals("Updated with assignee", response.getDescription());
        assertEquals(Priority.HIGH, response.getPriority());
        assertNotNull(response.getAssignedTo());
        assertEquals(userId, response.getAssignedTo().getId());
        assertEquals("assigneduser", response.getAssignedTo().getUsername());

        // Verify
        verify(taskRepository).findById(taskId);
        verify(userRepository).findById(userId);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void updateTask_shouldThrowUserNotFoundExceptionWhenAssignedUserDoesNotExist() {
        // Arrange
        Long taskId = 1L;
        Long invalidUserId = 99L;

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));

        when(userRepository.findById(invalidUserId)).thenReturn(Optional.empty());

        TaskRequest request = new TaskRequest();
        request.setTitle("Task with Invalid Assignee");
        request.setDescription("This should fail");
        request.setPriority(Priority.MEDIUM);
        request.setAssignedToId(invalidUserId);

        // Act + Assert
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            taskService.updateTask(taskId, request);
        });

        assertEquals("User with id 99 not found", exception.getMessage());

        // Verify
        verify(taskRepository).findById(taskId);
        verify(userRepository).findById(invalidUserId);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateTask_shouldClearAssigneeWhenAssignedToIdIsNull() throws Exception {
        // Arrange
        Long taskId = 1L;
        User assignedUser = new User();
        assignedUser.setId(5L);
        assignedUser.setUsername("existing");
        assignedUser.setEmail("existing@example.com");

        savedTask.setAssignedTo(assignedUser);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));

        TaskRequest request = new TaskRequest();
        request.setTitle("Updated task without assignee");
        request.setDescription("Should clear assignment");
        request.setPriority(Priority.LOW);
        request.setAssignedToId(null);

        Task updatedTask = new Task();
        updatedTask.setId(taskId);
        updatedTask.setTitle(request.getTitle());
        updatedTask.setDescription(request.getDescription());
        updatedTask.setPriority(request.getPriority());
        updatedTask.setStatus(TaskStatus.TODO);
        updatedTask.setAssignedTo(null);

        LocalDateTime now = LocalDateTime.now();
        setFieldValue(updatedTask, "createdAt", now.minusDays(1));
        setFieldValue(updatedTask, "updatedAt", now);

        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        // Act
        TaskResponse response = taskService.updateTask(taskId, request);

        // Assert
        assertNotNull(response);
        assertNull(response.getAssignedTo());

        // Verify
        verify(taskRepository).findById(taskId);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void getFilteredTasks_shouldReturnAllTasksWhenNoFiltersAreProvided() {
        // Arrange
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(2, 5), 1);
        when(taskRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        PaginatedResponse<TaskResponse> response = taskService.getFilteredTasks(null, null, null, null, 2, 5, null, "asc");

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getPage());
        assertEquals(5, response.getSize());
        assertTrue(response.getTotalElements() >= 1);
        assertEquals(1, response.getContent().size());

        // Verify
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findAll(pageableCaptor.capture());
        assertEquals(2, pageableCaptor.getValue().getPageNumber());
        assertEquals(5, pageableCaptor.getValue().getPageSize());
        assertEquals(Sort.Direction.ASC, pageableCaptor.getValue().getSort().getOrderFor("id").getDirection());
    }

    @Test
    void getFilteredTasks_shouldFilterByStatus() {
        // Arrange
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 10), 1);
        when(taskRepository.findByStatus(eq(TaskStatus.TODO), any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks(null, TaskStatus.TODO, null, null, 0, 10, "id", "asc");

        // Verify
        verify(taskRepository).findByStatus(eq(TaskStatus.TODO), any(Pageable.class));
        verify(taskRepository, never()).findByPriority(eq(Priority.HIGH), any(Pageable.class));
        verify(taskRepository, never()).findByAssignedToId(eq(7L), any(Pageable.class));
        verify(taskRepository, never()).searchByTitleOrDescription(eq("spring"), any(Pageable.class));
    }

    @Test
    void getFilteredTasks_shouldFilterByPriority() {
        // Arrange
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 10), 1);
        when(taskRepository.findByPriority(eq(Priority.HIGH), any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks(null, null, Priority.HIGH, null, 0, 10, "id", "asc");

        // Verify
        verify(taskRepository).findByPriority(eq(Priority.HIGH), any(Pageable.class));
    }

    @Test
    void getFilteredTasks_shouldFilterByAssignedUser() {
        // Arrange
        Long assignedUserId = 7L;
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 10), 1);
        when(taskRepository.findByAssignedToId(eq(assignedUserId), any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks(null, null, null, assignedUserId, 0, 10, "id", "asc");

        // Verify
        verify(taskRepository).findByAssignedToId(eq(assignedUserId), any(Pageable.class));
    }

    @Test
    void getFilteredTasks_shouldFilterByStatusAndPriority() {
        // Arrange
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 10), 1);
        when(taskRepository.findByStatusAndPriority(eq(TaskStatus.TODO), eq(Priority.HIGH), any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks(null, TaskStatus.TODO, Priority.HIGH, null, 0, 10, "id", "asc");

        // Verify
        verify(taskRepository).findByStatusAndPriority(eq(TaskStatus.TODO), eq(Priority.HIGH), any(Pageable.class));
    }

    @Test
    void getFilteredTasks_shouldFilterByStatusAndAssignedUser() {
        // Arrange
        Long assignedUserId = 7L;
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 10), 1);
        when(taskRepository.findByStatusAndAssignedToId(eq(TaskStatus.TODO), eq(assignedUserId), any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks(null, TaskStatus.TODO, null, assignedUserId, 0, 10, "id", "asc");

        // Verify
        verify(taskRepository).findByStatusAndAssignedToId(eq(TaskStatus.TODO), eq(assignedUserId), any(Pageable.class));
    }

    @Test
    void getFilteredTasks_shouldFilterByPriorityAndAssignedUser() {
        // Arrange
        Long assignedUserId = 7L;
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 10), 1);
        when(taskRepository.findByPriorityAndAssignedToId(eq(Priority.HIGH), eq(assignedUserId), any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks(null, null, Priority.HIGH, assignedUserId, 0, 10, "id", "asc");

        // Verify
        verify(taskRepository).findByPriorityAndAssignedToId(eq(Priority.HIGH), eq(assignedUserId), any(Pageable.class));
    }

    @Test
    void getFilteredTasks_shouldFilterByStatusPriorityAndAssignedUser() {
        // Arrange
        Long assignedUserId = 7L;
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 10), 1);
        when(taskRepository.findByStatusAndPriorityAndAssignedToId(eq(TaskStatus.TODO), eq(Priority.HIGH), eq(assignedUserId), any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks(null, TaskStatus.TODO, Priority.HIGH, assignedUserId, 0, 10, "id", "asc");

        // Verify
        verify(taskRepository).findByStatusAndPriorityAndAssignedToId(eq(TaskStatus.TODO), eq(Priority.HIGH), eq(assignedUserId), any(Pageable.class));
    }

    @Test
    void getFilteredTasks_shouldSearchByTitleOrDescription() {
        // Arrange
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 10), 1);
        when(taskRepository.searchByTitleOrDescription(eq("spring"), any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks("spring", null, null, null, 0, 10, "id", "asc");

        // Verify
        verify(taskRepository).searchByTitleOrDescription(eq("spring"), any(Pageable.class));
    }

    @Test
    void getFilteredTasks_shouldSearchWithStatus() {
        // Arrange
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 10), 1);
        when(taskRepository.searchByTitleOrDescriptionAndStatus(eq("spring"), eq(TaskStatus.TODO), any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks("spring", TaskStatus.TODO, null, null, 0, 10, "id", "asc");

        // Verify
        verify(taskRepository).searchByTitleOrDescriptionAndStatus(eq("spring"), eq(TaskStatus.TODO), any(Pageable.class));
    }

    @Test
    void getFilteredTasks_shouldSearchWithPriority() {
        // Arrange
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 10), 1);
        when(taskRepository.searchByTitleOrDescriptionAndPriority(eq("spring"), eq(Priority.HIGH), any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks("spring", null, Priority.HIGH, null, 0, 10, "id", "asc");

        // Verify
        verify(taskRepository).searchByTitleOrDescriptionAndPriority(eq("spring"), eq(Priority.HIGH), any(Pageable.class));
    }

    @Test
    void getFilteredTasks_shouldSearchWithAssignedUser() {
        // Arrange
        Long assignedUserId = 7L;
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 10), 1);
        when(taskRepository.searchByTitleOrDescriptionAndAssignedToId(eq("spring"), eq(assignedUserId), any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks("spring", null, null, assignedUserId, 0, 10, "id", "asc");

        // Verify
        verify(taskRepository).searchByTitleOrDescriptionAndAssignedToId(eq("spring"), eq(assignedUserId), any(Pageable.class));
    }

    @Test
    void getFilteredTasks_shouldSearchWithStatusPriority() {
        // Arrange
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 10), 1);
        when(taskRepository.searchByTitleOrDescriptionAndStatusAndPriority(eq("spring"), eq(TaskStatus.TODO), eq(Priority.HIGH), any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks("spring", TaskStatus.TODO, Priority.HIGH, null, 0, 10, "id", "asc");

        // Verify
        verify(taskRepository).searchByTitleOrDescriptionAndStatusAndPriority(eq("spring"), eq(TaskStatus.TODO), eq(Priority.HIGH), any(Pageable.class));
    }

    @Test
    void getFilteredTasks_shouldSearchWithStatusAndAssignedUser() {
        // Arrange
        Long assignedUserId = 7L;
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 10), 1);
        when(taskRepository.searchByTitleOrDescriptionAndStatusAndAssignedToId(eq("spring"), eq(TaskStatus.TODO), eq(assignedUserId), any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks("spring", TaskStatus.TODO, null, assignedUserId, 0, 10, "id", "asc");

        // Verify
        verify(taskRepository).searchByTitleOrDescriptionAndStatusAndAssignedToId(eq("spring"), eq(TaskStatus.TODO), eq(assignedUserId), any(Pageable.class));
    }

    @Test
    void getFilteredTasks_shouldSearchWithPriorityAndAssignedUser() {
        // Arrange
        Long assignedUserId = 7L;
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 10), 1);
        when(taskRepository.searchByTitleOrDescriptionAndPriorityAndAssignedToId(eq("spring"), eq(Priority.HIGH), eq(assignedUserId), any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks("spring", null, Priority.HIGH, assignedUserId, 0, 10, "id", "asc");

        // Verify
        verify(taskRepository).searchByTitleOrDescriptionAndPriorityAndAssignedToId(eq("spring"), eq(Priority.HIGH), eq(assignedUserId), any(Pageable.class));
    }

    @Test
    void getFilteredTasks_shouldSearchWithAllFilters() {
        // Arrange
        Long assignedUserId = 7L;
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 10), 1);
        when(taskRepository.searchByTitleOrDescriptionAndStatusAndPriorityAndAssignedToId(eq("spring"), eq(TaskStatus.TODO), eq(Priority.HIGH), eq(assignedUserId), any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks("spring", TaskStatus.TODO, Priority.HIGH, assignedUserId, 0, 10, "id", "asc");

        // Verify
        verify(taskRepository).searchByTitleOrDescriptionAndStatusAndPriorityAndAssignedToId(eq("spring"), eq(TaskStatus.TODO), eq(Priority.HIGH), eq(assignedUserId), any(Pageable.class));
    }

    @Test
    void getFilteredTasks_shouldApplyPagination() {
        // Arrange
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(2, 5), 1);
        when(taskRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks(null, null, null, null, 2, 5, "id", "asc");

        // Verify
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findAll(pageableCaptor.capture());
        assertEquals(2, pageableCaptor.getValue().getPageNumber());
        assertEquals(5, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void getFilteredTasks_shouldHandleZeroOrNegativePage() {
        // Arrange
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 10), 1);
        when(taskRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks(null, null, null, null, -5, 10, "id", "asc");

        // Verify
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findAll(pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
    }

    @Test
    void getFilteredTasks_shouldHandleInvalidPageSize() {
        // Arrange
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 1), 1);
        when(taskRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks(null, null, null, null, 0, -3, "id", "asc");

        // Verify
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findAll(pageableCaptor.capture());
        assertEquals(1, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void getFilteredTasks_shouldSortAscending() {
        // Arrange
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "title")), 1);
        when(taskRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks(null, null, null, null, 0, 10, "title", "asc");

        // Verify
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findAll(pageableCaptor.capture());
        assertEquals(Sort.Direction.ASC, pageableCaptor.getValue().getSort().getOrderFor("title").getDirection());
    }

    @Test
    void getFilteredTasks_shouldSortDescending() {
        // Arrange
        Page<Task> page = new PageImpl<>(List.of(savedTask), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "title")), 1);
        when(taskRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        taskService.getFilteredTasks(null, null, null, null, 0, 10, "title", "desc");

        // Verify
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findAll(pageableCaptor.capture());
        assertEquals(Sort.Direction.DESC, pageableCaptor.getValue().getSort().getOrderFor("title").getDirection());
    }

    @Test
    void deleteTask_shouldDeleteTaskAndReturnResponse() throws Exception {
        // Arrange
        Long taskId = 1L;
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));

        // Act
        TaskResponse response = taskService.deleteTask(taskId);

        // Assert
        assertNotNull(response);
        assertEquals(taskId, response.getId());
        assertEquals(savedTask.getTitle(), response.getTitle());

        // Verify
        verify(taskRepository).findById(taskId);
        verify(taskRepository).delete(savedTask);
    }

    @Test
    void deleteTask_shouldThrowTaskNotFoundExceptionWhenTaskDoesNotExist() {
        // Arrange
        Long taskId = 99L;
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        // Act + Assert
        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class, () -> {
            taskService.deleteTask(taskId);
        });

        assertEquals("Task with id 99 not found", exception.getMessage());

        // Verify
        verify(taskRepository).findById(taskId);
        verify(taskRepository, never()).delete(any(Task.class));
    }

    private void setFieldValue(Object target, String fieldName, Object value) throws Exception {
        Field field = Task.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

