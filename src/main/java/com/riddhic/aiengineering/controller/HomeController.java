package com.riddhic.aiengineering.controller;

import com.riddhic.aiengineering.service.GreetingService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1") // Sets a base path for all endpoints in this class

public class HomeController {

    private final GreetingService greetingService;

    public HomeController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    // 1. A simple GET endpoint returning a JSON string
        @GetMapping("/home")
        public Map<String, String> sayHello() {
            return Map.of(
                "message", greetingService.getGreeting()
            );
        }


    }
