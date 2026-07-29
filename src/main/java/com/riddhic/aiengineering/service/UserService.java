package com.riddhic.aiengineering.service;

import com.riddhic.aiengineering.dto.UserRequest;
import com.riddhic.aiengineering.dto.UserResponse;
import com.riddhic.aiengineering.exception.UserNotFoundException;
import com.riddhic.aiengineering.model.User;
import com.riddhic.aiengineering.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
