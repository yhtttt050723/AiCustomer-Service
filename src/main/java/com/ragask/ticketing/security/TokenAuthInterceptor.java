package com.ragask.ticketing.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragask.ticketing.common.api.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class TokenAuthInterceptor implements HandlerInterceptor {

    private final AuthTokenProperties authTokenProperties;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        if (method.hasMethodAnnotation(PublicApi.class)
                || method.getBeanType().isAnnotationPresent(PublicApi.class)) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response);
            return false;
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        for (AuthTokenProperties.TokenItem item : authTokenProperties.getTokens()) {
            if (item.getToken() != null && item.getToken().equals(token)) {
                Set<String> roles = new HashSet<>(item.getRoles());
                UserContext.set(new UserPrincipal(item.getUserId(), roles));
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        item.getUserId(),
                        null,
                        roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList()
                ));
                return true;
            }
        }

        writeUnauthorized(response);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.fail(401, "unauthorized"));
    }
}

