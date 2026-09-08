package com.riddhic.aiengineering.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riddhic.aiengineering.dto.PaginatedResponse;
import com.riddhic.aiengineering.dto.TaskRequest;
import com.riddhic.aiengineering.dto.TaskResponse;
import com.riddhic.aiengineering.enums.Priority;
import com.riddhic.aiengineering.enums.TaskStatus;
import com.riddhic.aiengineering.exception.TaskNotFoundException;
import com.riddhic.aiengineering.model.Task;
import com.riddhic.aiengineering.model.User;
import com.riddhic.aiengineering.service.TaskService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @Test
    void getTasks_shouldReturnEmptyList() throws Exception {
        // Arrange
        PaginatedResponse<TaskResponse> response = new PaginatedResponse<>(List.of(), 0, 10, 0, 0L, true);
        when(taskService.getFilteredTasks(null, null, null, null, 0, 10, "id", "asc")).thenReturn(response);

        // Act + Assert
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.last").value(true));

        verify(taskService).getFilteredTasks(null, null, null, null, 0, 10, "id", "asc");
    }

    @Test
    void getTasks_shouldReturnMultipleTasks() throws Exception {
        // Arrange
        TaskResponse taskOne = buildTaskResponse(1L, "Learn Spring Boot", "Study JPA", TaskStatus.TODO, Priority.HIGH, 2L, "dev");
        TaskResponse taskTwo = buildTaskResponse(2L, "Build API", "Write controllers", TaskStatus.IN_PROGRESS, Priority.MEDIUM, 3L, "qa");
        PaginatedResponse<TaskResponse> response = new PaginatedResponse<>(List.of(taskOne, taskTwo), 0, 10, 1, 2L, true);
        when(taskService.getFilteredTasks(null, null, null, null, 0, 10, "id", "asc")).thenReturn(response);

        // Act + Assert
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Learn Spring Boot"))
                .andExpect(jsonPath("$.content[0].assignedTo.username").value("dev"))
                .andExpect(jsonPath("$.content[1].id").value(2));

        verify(taskService).getFilteredTasks(null, null, null, null, 0, 10, "id", "asc");
    }

    @Test
    void getTasks_shouldPassSearchStatusPriorityAndAssignedUserFiltersToService() throws Exception {
        // Arrange
        PaginatedResponse<TaskResponse> response = new PaginatedResponse<>(List.of(), 2, 5, 0, 0L, true);
        when(taskService.getFilteredTasks("spring", TaskStatus.TODO, Priority.HIGH, 42L, 2, 5, "title", "desc"))
                .thenReturn(response);

        // Act + Assert
        mockMvc.perform(get("/tasks")
                        .param("search", "spring")
                        .param("status", "TODO")
                        .param("priority", "HIGH")
                        .param("assignedUserId", "42")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "title,desc"))
                .andExpect(status().isOk());

        verify(taskService).getFilteredTasks("spring", TaskStatus.TODO, Priority.HIGH, 42L, 2, 5, "title", "desc");
    }

    @Test
    void getTasks_shouldReturnBadRequestForInvalidEnumValues() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/tasks").param("status", "INVALID"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", containsString("No enum constant")))
                .andExpect(jsonPath("$.statusCode").value(500));
    }

    @Test
    void getTaskById_shouldReturnTaskWhenItExists() throws Exception {
        // Arrange
        TaskResponse response = buildTaskResponse(1L, "Learn Spring Boot", "Study JPA", TaskStatus.TODO, Priority.HIGH, 2L, "dev");
        when(taskService.getTaskById(1L)).thenReturn(response);

        // Act + Assert
        mockMvc.perform(get("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Learn Spring Boot"))
                .andExpect(jsonPath("$.description").value("Study JPA"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.assignedTo.id").value(2))
                .andExpect(jsonPath("$.assignedTo.username").value("dev"));

        verify(taskService).getTaskById(1L);
    }

    @Test
    void getTaskById_shouldReturnNotFoundWhenTaskDoesNotExist() throws Exception {
        // Arrange
        when(taskService.getTaskById(99L)).thenThrow(new TaskNotFoundException("Task with id 99 not found"));

        // Act + Assert
        mockMvc.perform(get("/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task with id 99 not found"))
                .andExpect(jsonPath("$.statusCode").value(404));

        verify(taskService).getTaskById(99L);
    }

    @Test
    void createTask_shouldCreateTaskSuccessfully() throws Exception {
        // Arrange
        TaskRequest request = new TaskRequest("Learn Mockito", "Write unit tests", Priority.HIGH, 2L);
        TaskResponse response = buildTaskResponse(10L, "Learn Mockito", "Write unit tests", TaskStatus.TODO, Priority.HIGH, 2L, "dev");
        when(taskService.createTask(any(TaskRequest.class))).thenReturn(response);

        // Act + Assert
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Learn Mockito"))
                .andExpect(jsonPath("$.description").value("Write unit tests"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.assignedTo.id").value(2))
                .andExpect(jsonPath("$.assignedTo.username").value("dev"));

        ArgumentCaptor<TaskRequest> captor = ArgumentCaptor.forClass(TaskRequest.class);
        verify(taskService).createTask(captor.capture());
        assertEquals("Learn Mockito", captor.getValue().getTitle());
        assertEquals("Write unit tests", captor.getValue().getDescription());
        assertEquals(Priority.HIGH, captor.getValue().getPriority());
        assertEquals(2L, captor.getValue().getAssignedToId());
    }

    @Test
    void createTask_shouldReturnInternalServerErrorWhenRequestIsInvalid() throws Exception {
        // Arrange
        TaskRequest invalidRequest = new TaskRequest("", "Description", Priority.HIGH, null);

        // Act + Assert
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value(500));
    }

    @Test
    void createTask_shouldReturnInternalServerErrorWhenJsonIsMalformed() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value(500));
    }

    @Test
    void updateTask_shouldUpdateTaskSuccessfully() throws Exception {
        // Arrange
        TaskRequest request = new TaskRequest("Updated title", "Updated description", Priority.MEDIUM, 5L);
        TaskResponse response = buildTaskResponse(1L, "Updated title", "Updated description", TaskStatus.TODO, Priority.MEDIUM, 5L, "pm");
        when(taskService.updateTask(eq(1L), any(TaskRequest.class))).thenReturn(response);

        // Act + Assert
        mockMvc.perform(put("/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.assignedTo.id").value(5))
                .andExpect(jsonPath("$.assignedTo.username").value("pm"));

        ArgumentCaptor<TaskRequest> captor = ArgumentCaptor.forClass(TaskRequest.class);
        verify(taskService).updateTask(eq(1L), captor.capture());
        assertEquals("Updated title", captor.getValue().getTitle());
        assertEquals(Priority.MEDIUM, captor.getValue().getPriority());
        assertEquals(5L, captor.getValue().getAssignedToId());
    }

    @Test
    void updateTask_shouldReturnNotFoundWhenTaskDoesNotExist() throws Exception {
        // Arrange
        TaskRequest request = new TaskRequest("Updated title", "Updated description", Priority.MEDIUM, null);
        when(taskService.updateTask(eq(99L), any(TaskRequest.class)))
                .thenThrow(new TaskNotFoundException("Task with id 99 not found"));

        // Act + Assert
        mockMvc.perform(put("/tasks/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task with id 99 not found"))
                .andExpect(jsonPath("$.statusCode").value(404));

        verify(taskService).updateTask(eq(99L), any(TaskRequest.class));
    }

    @Test
    void deleteTask_shouldDeleteTaskSuccessfully() throws Exception {
        // Arrange
        TaskResponse response = buildTaskResponse(1L, "Delete task", "Remove this", TaskStatus.TODO, Priority.LOW, null, null);
        when(taskService.deleteTask(1L)).thenReturn(response);

        // Act + Assert
        mockMvc.perform(delete("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Delete task"))
                .andExpect(jsonPath("$.priority").value("LOW"));

        verify(taskService).deleteTask(1L);
    }

    @Test
    void deleteTask_shouldReturnNotFoundWhenTaskDoesNotExist() throws Exception {
        // Arrange
        when(taskService.deleteTask(99L)).thenThrow(new TaskNotFoundException("Task with id 99 not found"));

        // Act + Assert
        mockMvc.perform(delete("/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task with id 99 not found"))
                .andExpect(jsonPath("$.statusCode").value(404));

        verify(taskService).deleteTask(99L);
    }

    private TaskResponse buildTaskResponse(Long id, String title, String description, TaskStatus status,
                                          Priority priority, Long assignedUserId, String username) throws Exception {
        User user = null;
        if (assignedUserId != null) {
            user = new User();
            user.setId(assignedUserId);
            user.setUsername(username);
            user.setEmail(username + "@example.com");
        }

        Task task = new Task(id, title, description, status, priority, user);
        setFieldValue(task, "createdAt", LocalDateTime.now());
        setFieldValue(task, "updatedAt", LocalDateTime.now());
        return new TaskResponse(task);
    }

    private void setFieldValue(Object target, String fieldName, Object value) throws Exception {
        Field field = Task.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
