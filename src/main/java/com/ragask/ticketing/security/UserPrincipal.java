package com.ragask.ticketing.security;

import java.util.Set;

public record UserPrincipal(Long userId, Set<String> roles) {
}

