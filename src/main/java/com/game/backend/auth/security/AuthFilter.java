package com.game.backend.auth.security;

import com.game.backend.auth.service.AuthService;
import com.game.backend.auth.service.SessionData;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

/**
 * Security filter that enforces session-token validation on protected player endpoints.
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final HandlerExceptionResolver exceptionResolver;

    public AuthFilter(
        AuthService authService,
        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver
    ) {
        this.authService = authService;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (!isProtected(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            SessionData session = authService.validateToken(request.getHeader("Authorization"));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                session.playerId(),
                null,
                List.of()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            exceptionResolver.resolveException(request, response, null, ex);
        }
    }

    private boolean isProtected(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/v1/players/")) {
            return false;
        }
        String method = request.getMethod();
        return "GET".equals(method) || "PUT".equals(method);
    }
}
