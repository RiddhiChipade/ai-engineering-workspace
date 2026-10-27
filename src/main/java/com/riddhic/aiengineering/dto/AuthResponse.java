package com.riddhic.aiengineering.dto;

import com.riddhic.aiengineering.model.User;

public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private String username;
    private String role;

    public AuthResponse() {
    }

    public AuthResponse(String token, String username, String role) {
        this.token = token;
        this.username = username;
        this.role = role;
    }

    public static AuthResponse fromUser(User user, String token) {
        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
