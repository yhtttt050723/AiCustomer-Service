package com.ragask.ticketing.service;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class PromptSecurityService {

    private static final List<String> INJECTION_PATTERNS = List.of(
            "ignore previous instructions",
            "system prompt",
            "developer message",
            "reveal secret",
            "输出系统提示词",
            "忽略之前指令",
            "越权",
            "泄露"
    );

    public boolean looksLikePromptInjection(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return INJECTION_PATTERNS.stream().anyMatch(normalized::contains);
    }

    public String sanitizeRetrievedContent(String content) {
        if (content == null) {
            return "";
        }
        String sanitized = content.replaceAll("(?i)ignore\\s+previous\\s+instructions", "[blocked]");
        sanitized = sanitized.replaceAll("(?i)system\\s+prompt", "[blocked]");
        return sanitized;
    }
}
