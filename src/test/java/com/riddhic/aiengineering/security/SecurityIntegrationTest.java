package com.riddhic.aiengineering.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riddhic.aiengineering.dto.LoginRequest;
import com.riddhic.aiengineering.dto.RegisterRequest;
import com.riddhic.aiengineering.enums.Role;
import com.riddhic.aiengineering.model.User;
import com.riddhic.aiengineering.model.Task;
import com.riddhic.aiengineering.enums.Priority;
import com.riddhic.aiengineering.enums.TaskStatus;
import java.time.LocalDate;
import com.riddhic.aiengineering.repository.TaskRepository;
import com.riddhic.aiengineering.repository.UserRepository;
import com.riddhic.aiengineering.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "assistant.openai.api-key=")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void frontendShellAndAssets_shouldBePublic() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/assets/app.css"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/assets/app.js"))
                .andExpect(status().isOk());
    }
    @Test
    void publicHomeEndpoint_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk());
    }

    @Test
    void registerEndpoint_shouldCreateUserAndHidePassword() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("riddhi", "riddhi@example.com", "Password123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("riddhi"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User user = userRepository.findByUsername("riddhi").orElseThrow();
        assertTrue(passwordEncoder.matches("Password123", user.getPassword()));
        assertNotEquals("Password123", user.getPassword());
        assertEquals(Role.USER, user.getRole());
    }

    @Test
    void loginEndpoint_shouldAuthenticateAndReturnJwt() throws Exception {
        User user = new User();
        user.setUsername("riddhi");
        user.setEmail("riddhi@example.com");
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setRole(Role.USER);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("riddhi", "Password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("riddhi"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void tasksWithoutJwt_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tasksWithValidJwt_shouldReturnOk() throws Exception {
        User user = new User();
        user.setUsername("admin");
        user.setEmail("admin@example.com");
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setRole(Role.ADMIN);
        userRepository.save(user);

        String jwt = jwtService.generateToken(user);

        mockMvc.perform(get("/tasks").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
    }

    @Test
    void userCanOnlySeeAndReadTheirOwnTasks() throws Exception {
        User first = createUser("first", "first@example.com");
        User second = createUser("second", "second@example.com");
        Task mine = createTask("My task", first);
        Task theirs = createTask("Other task", second);
        String jwt = jwtService.generateToken(first);

        mockMvc.perform(get("/tasks").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(mine.getId()));

        mockMvc.perform(get("/tasks/" + theirs.getId()).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void pendingTasksToolOnlyReturnsAuthenticatedUsersTasks() throws Exception {
        User first = createUser("first", "first@example.com");
        User second = createUser("second", "second@example.com");
        Task mine = createTask("My pending", first);
        createTask("Other pending", second);
        String jwt = jwtService.generateToken(first);

        mockMvc.perform(post("/ai/tools/getPendingTasks").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(mine.getId()));
    }

    @Test
    void overdueTasksToolUsesDueDateStatusAndUserScope() throws Exception {
        User first = createUser("first", "first@example.com");
        User second = createUser("second", "second@example.com");
        LocalDate today = LocalDate.now(java.time.Clock.systemUTC());
        Task overdue = createTask("Overdue", first);
        overdue.setDueDate(today.minusDays(1));
        taskRepository.save(overdue);
        Task dueToday = createTask("Due today", first);
        dueToday.setDueDate(today);
        taskRepository.save(dueToday);
        Task completed = createTask("Done overdue", first);
        completed.setDueDate(today.minusDays(5));
        completed.setStatus(TaskStatus.DONE);
        taskRepository.save(completed);
        Task anotherUser = createTask("Other overdue", second);
        anotherUser.setDueDate(today.minusDays(2));
        taskRepository.save(anotherUser);
        String jwt = jwtService.generateToken(first);

        mockMvc.perform(post("/ai/tools/getOverdueTasks").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Overdue"));
    }

    @Test
    void assistantChatRequiresConfiguredOpenAiKeyAndAuthentication() throws Exception {
        mockMvc.perform(post("/assistant/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What should I do today?\"}"))
                .andExpect(status().isUnauthorized());

        User user = createUser("assistant-user", "assistant@example.com");
        String jwt = jwtService.generateToken(user);
        mockMvc.perform(post("/assistant/chat").header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What should I do today?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("OPENAI_API_KEY")));
    }

    @Test
    void userRoleCannotAccessUserAdministration() throws Exception {
        User user = createUser("regular", "regular@example.com");
        String jwt = jwtService.generateToken(user);

        mockMvc.perform(get("/users").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
    }

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    private Task createTask(String title, User assignee) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription("Test task");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(Priority.MEDIUM);
        task.setAssignedTo(assignee);
        return taskRepository.save(task);
    }

    @Test
    void swaggerEndpoints_shouldRemainAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
