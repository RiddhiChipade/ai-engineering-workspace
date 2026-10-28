package com.riddhic.aiengineering.cache;

import com.riddhic.aiengineering.dto.TaskRequest;
import com.riddhic.aiengineering.enums.Priority;
import com.riddhic.aiengineering.enums.Role;
import com.riddhic.aiengineering.enums.TaskStatus;
import com.riddhic.aiengineering.model.Task;
import com.riddhic.aiengineering.model.User;
import com.riddhic.aiengineering.repository.TaskRepository;
import com.riddhic.aiengineering.repository.UserRepository;
import com.riddhic.aiengineering.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class TaskCachingIntegrationTest {

    @Container
    private static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> REDIS_CONTAINER.getHost());
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
        registry.add("spring.cache.type", () -> "redis");
    }

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @SpyBean
    private TaskRepository taskRepositorySpy;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
        cacheManager.getCache("task-query").clear();
        Mockito.reset(taskRepositorySpy);

        User user = new User();
        user.setUsername("cache-user");
        user.setEmail("cache-user@example.com");
        user.setPassword("hashed-password");
        user.setRole(Role.USER);
        userRepository.save(user);

        Task task = new Task();
        task.setTitle("Cache task");
        task.setDescription("Initial cache task");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(Priority.HIGH);
        task.setAssignedTo(user);
        taskRepository.save(task);
    }

    @Test
    void getFilteredTasks_shouldUseCacheForRepeatedSameQuery() {
        taskService.getFilteredTasks(null, null, null, null, 0, 10, "id", "asc");
        verify(taskRepositorySpy, times(1)).findAll(any(Pageable.class));

        Set<String> keysAfterFirstQuery = redisTemplate.keys("tasks::*");
        assertNotNull(keysAfterFirstQuery);
        assertFalse(keysAfterFirstQuery.isEmpty());

        taskService.getFilteredTasks(null, null, null, null, 0, 10, "id", "asc");
        verify(taskRepositorySpy, times(1)).findAll(any(Pageable.class));

        Set<String> keysAfterSecondQuery = redisTemplate.keys("tasks::*");
        assertEquals(1, keysAfterSecondQuery.size());
    }

    @Test
    void getFilteredTasks_shouldCreateDistinctCacheEntriesForDifferentQueries() {
        taskService.getFilteredTasks(null, null, null, null, 0, 10, "id", "asc");
        taskService.getFilteredTasks("Cache", null, null, null, 0, 10, "id", "asc");

        Set<String> keys = redisTemplate.keys("tasks::*");
        assertNotNull(keys);
        assertTrue(keys.size() >= 2);
    }

    @Test
    void createTask_shouldEvictTaskCache() {
        taskService.getFilteredTasks(null, null, null, null, 0, 10, "id", "asc");
        assertFalse(redisTemplate.keys("tasks::*").isEmpty());

        User user = userRepository.findByUsername("cache-user").orElseThrow();
        taskService.createTask(new TaskRequest("New task", "Fresh content", Priority.MEDIUM, user.getId()));

        assertTrue(redisTemplate.keys("tasks::*").isEmpty());
    }

    @Test
    void updateTask_shouldEvictTaskCache() {
        taskService.getFilteredTasks(null, null, null, null, 0, 10, "id", "asc");
        assertFalse(redisTemplate.keys("tasks::*").isEmpty());

        Task task = taskRepository.findAll(PageRequest.of(0, 10)).getContent().getFirst();
        taskService.updateTask(task.getId(), new TaskRequest("Updated task", "Updated description", Priority.LOW, task.getAssignedTo().getId()));

        assertTrue(redisTemplate.keys("tasks::*").isEmpty());
    }

    @Test
    void deleteTask_shouldEvictTaskCache() {
        taskService.getFilteredTasks(null, null, null, null, 0, 10, "id", "asc");
        assertFalse(redisTemplate.keys("tasks::*").isEmpty());

        Task task = taskRepository.findAll(PageRequest.of(0, 10)).getContent().getFirst();
        taskService.deleteTask(task.getId());

        assertTrue(redisTemplate.keys("tasks::*").isEmpty());
    }

    @Test
    void taskQueryCache_shouldUseConfiguredTtl() {
        RedisCache redisCache = (RedisCache) cacheManager.getCache("task-query");
        assertNotNull(redisCache);
        assertEquals(Duration.ofSeconds(60), redisCache.getCacheConfiguration().getTtl());
    }
}
