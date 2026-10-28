package com.riddhic.aiengineering.controller;

import com.riddhic.aiengineering.dto.AssistantChatRequest;
import com.riddhic.aiengineering.dto.AssistantChatResponse;
import com.riddhic.aiengineering.service.TaskAssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assistant")
public class AssistantController {
    private final TaskAssistantService assistantService;

    public AssistantController(TaskAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/chat")
    public AssistantChatResponse chat(@Valid @RequestBody AssistantChatRequest request) {
        return new AssistantChatResponse(assistantService.answer(request.message()));
    }
}
