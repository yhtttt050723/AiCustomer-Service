package com.ragask.ticketing.security;

import java.util.Collections;
import java.util.Set;

/**
 * Thread-local user context populated by token interceptor.
 */
public final class UserContext {

    private static final ThreadLocal<UserPrincipal> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UserPrincipal principal) {
        HOLDER.set(principal);
    }

    public static Long getCurrentUserId() {
        UserPrincipal principal = HOLDER.get();
        return principal == null ? null : principal.userId();
    }

    public static Set<String> getCurrentRoles() {
        UserPrincipal principal = HOLDER.get();
        return principal == null ? Collections.emptySet() : principal.roles();
    }

    public static void clear() {
        HOLDER.remove();
    }
}

