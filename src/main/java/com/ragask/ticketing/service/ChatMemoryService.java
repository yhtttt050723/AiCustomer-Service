package com.ragask.ticketing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragask.ticketing.model.ConversationRecord;
import com.ragask.ticketing.model.dto.ChatMessage;
import com.ragask.ticketing.repository.ConversationRecordRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatMemoryService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String backend;
    private final long ttlSeconds;
    private final ConversationRecordRepository conversationRecordRepository;
    private final List<ChatMessage> fallbackMemory = new ArrayList<>();

    public ChatMemoryService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            ConversationRecordRepository conversationRecordRepository,
            @Value("${chat.memory.backend:redis}") String backend,
            @Value("${chat.memory.ttl-seconds:604800}") long ttlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.conversationRecordRepository = conversationRecordRepository;
        this.backend = backend;
        this.ttlSeconds = ttlSeconds;
    }

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
        } catch (Exception ignored) {
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
        } catch (Exception ignored) {
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
        } catch (Exception ignored) {
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
            return "{\"role\":\"" + message.role() + "\",\"content\":\"" + message.content() + "\"}";
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
            record.setRole(message.role());
            record.setContent(message.content());
            conversationRecordRepository.save(record);
        } catch (Exception ignored) {
            // keep chat flow even if audit persistence fails
        }
    }
}
