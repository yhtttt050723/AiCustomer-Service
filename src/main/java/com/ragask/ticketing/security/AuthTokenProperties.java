package com.ragask.ticketing.security;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "security.auth")
public class AuthTokenProperties {
    private List<TokenItem> tokens = new ArrayList<>();

    @Data
    public static class TokenItem {
        private String token;
        private Long userId;
        private List<String> roles = new ArrayList<>();
    }
}

