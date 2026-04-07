package com.example.hrms.controller;

import com.example.hrms.dto.ChatRequest;
import com.example.hrms.dto.ChatResponse;
import com.example.hrms.service.AiService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hrms-ai")
@CrossOrigin(origins = "*") // Cho phép React gọi qua port khác
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/ask")
    public ChatResponse askAi(@RequestBody ChatRequest request) {
        try {
            String answer = aiService.getAiAnswer(request.getMessage());
            return new ChatResponse(answer, "success");
        } catch (Exception e) {
            return new ChatResponse("Lỗi: " + e.getMessage(), "error");
        }
    }
}