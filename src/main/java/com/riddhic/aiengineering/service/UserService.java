package com.riddhic.aiengineering.service;

import com.riddhic.aiengineering.dto.UserRequest;
import com.riddhic.aiengineering.dto.UserResponse;
import com.riddhic.aiengineering.dto.TaskResponse;
import com.riddhic.aiengineering.dto.PaginatedResponse;
import com.riddhic.aiengineering.exception.UserNotFoundException;
import com.riddhic.aiengineering.model.User;
import com.riddhic.aiengineering.repository.UserRepository;
import com.riddhic.aiengineering.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public UserService(UserRepository userRepository, TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    public void deleteUserById(Long id) {
        userRepository.deleteById(id);
    }

    public UserResponse createUser(UserRequest userRequest) {
        User user = toEntity(userRequest);
        user = userRepository.save(user);
        return toResponse(user);
    }

    public UserResponse updateUser(Long id, UserRequest userRequest) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        existingUser.setUsername(userRequest.getUsername());
        existingUser.setEmail(userRequest.getEmail());

        User updatedUser = userRepository.save(existingUser);
        return toResponse(updatedUser);
    }

    public PaginatedResponse<TaskResponse> getUserTasks(
            Long userId,
            int page,
            int size,
            String sortBy,
            String sortDirection) {
        
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        
        Sort.Direction direction = Sort.Direction.fromString(sortDirection.toUpperCase());
        String validSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
        int validPage = Math.max(0, page);
        int validSize = Math.max(1, size);
        
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(direction, validSortBy));
        Page<?> taskPage = taskRepository.findByAssignedToId(userId, pageable);
        
        List<TaskResponse> content = taskPage.getContent().stream()
                .map(task -> new TaskResponse((com.riddhic.aiengineering.model.Task) task))
                .toList();
        
        return new PaginatedResponse<>(content, validPage, validSize, taskPage.getTotalPages(), taskPage.getTotalElements(), taskPage.isLast());
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );
    }

    private User toEntity(UserRequest userRequest) {
        User user = new User();
        user.setUsername(userRequest.getUsername());
        user.setEmail(userRequest.getEmail());
        return user;
    }

}
