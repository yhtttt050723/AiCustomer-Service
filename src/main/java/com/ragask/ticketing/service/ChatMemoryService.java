package com.ragask.ticketing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragask.ticketing.model.ConversationRecord;
import com.ragask.ticketing.model.dto.ChatMessage;
import com.ragask.ticketing.repository.ConversationRecordRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMemoryService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ConversationRecordRepository conversationRecordRepository;
    private final List<ChatMessage> fallbackMemory = new ArrayList<>();
    @Value("${chat.memory.backend:redis}")
    private String backend;
    @Value("${chat.memory.ttl-seconds:604800}")
    private long ttlSeconds;

    public List<ChatMessage> recent(String sessionId, int limit) {
        if (!useRedis()) {
            return fallbackMemory.stream().skip(Math.max(0, fallbackMemory.size() - limit)).toList();
        }
        try {
            String key = key(sessionId);
            Long size = redisTemplate.opsForList().size(key);
            if (size == null || size == 0) {
                return List.of();
            }
            long start = Math.max(0, size - limit);
            List<String> rows = redisTemplate.opsForList().range(key, start, size - 1);
            if (rows == null) {
                return List.of();
            }
            return rows.stream().map(this::decode).toList();
        } catch (Exception ex) {
            log.warn("Read chat memory from Redis failed, sessionId={}", sessionId, ex);
            return List.of();
        }
    }

    public void append(String sessionId, ChatMessage message) {
        if (!useRedis()) {
            fallbackMemory.add(message);
            return;
        }
        try {
            String key = key(sessionId);
            redisTemplate.opsForList().rightPush(key, encode(message));
            redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
        } catch (Exception ex) {
            log.warn("Write chat memory to Redis failed, fallback to in-memory, sessionId={}", sessionId, ex);
            fallbackMemory.add(message);
        }
        saveRecord(sessionId, message);
    }

    public boolean redisMemoryEnabled() {
        return useRedis();
    }

    public boolean redisReachable() {
        if (!useRedis()) {
            return false;
        }
        try {
            redisTemplate.opsForValue().set("health:redis", "ok", Duration.ofSeconds(10));
            return true;
        } catch (Exception ex) {
            log.debug("Redis health probe failed", ex);
            return false;
        }
    }

    private boolean useRedis() {
        return "redis".equalsIgnoreCase(backend);
    }

    private String key(String sessionId) {
        return "chat:memory:" + sessionId;
    }

    private String encode(ChatMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            return "{\"role\":\"" + message.getRole() + "\",\"content\":\"" + message.getContent() + "\"}";
        }
    }

    private ChatMessage decode(String row) {
        try {
            return objectMapper.readValue(row, ChatMessage.class);
        } catch (JsonProcessingException e) {
            return new ChatMessage("assistant", row);
        }
    }

    private void saveRecord(String sessionId, ChatMessage message) {
        try {
            ConversationRecord record = new ConversationRecord();
            record.setSessionId(sessionId);
            record.setRole(message.getRole());
            record.setContent(message.getContent());
            conversationRecordRepository.save(record);
        } catch (Exception ex) {
            log.warn("Persist conversation record failed, sessionId={}", sessionId, ex);
        }
    }
}
