package com.riddhic.aiengineering.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantChatRequest(
        @NotBlank(message = "message cannot be blank")
        @Size(max = 4000, message = "message cannot exceed 4000 characters")
        String message) { }
