package com.ty.infinitechatagent.controller;

import com.ty.infinitechatagent.ai.AiChat;
import dev.langchain4j.service.MemoryId;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiChatController {

    @Resource
    private AiChat aiChat;

    @GetMapping("/chat")
    public String chat(@MemoryId String sessionId, String prompt) {
        return aiChat.chat(sessionId, prompt);
    }
}