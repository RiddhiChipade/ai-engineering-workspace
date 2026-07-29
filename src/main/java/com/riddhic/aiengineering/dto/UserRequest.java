package com.riddhic.aiengineering.dto;

public class UserRequest {
    private String username;
    private String email;

    public UserRequest() {
    }

    public UserRequest(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
