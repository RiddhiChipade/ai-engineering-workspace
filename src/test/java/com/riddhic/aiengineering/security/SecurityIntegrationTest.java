package com.riddhic.aiengineering.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riddhic.aiengineering.dto.LoginRequest;
import com.riddhic.aiengineering.dto.RegisterRequest;
import com.riddhic.aiengineering.enums.Role;
import com.riddhic.aiengineering.model.User;
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
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
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
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

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
    void swaggerEndpoints_shouldRemainAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
