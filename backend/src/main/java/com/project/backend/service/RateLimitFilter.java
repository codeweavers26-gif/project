package com.project.backend.service;

import java.io.IOException;
import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = request.getRemoteAddr();

        if (path.contains("/api/auth/register")) {

            boolean allowed = rateLimitService.tryConsume(
                    "REGISTER_IP_" + ip,
                    5,
                    Duration.ofMinutes(1)
            );

            if (!allowed) {
                send429(response);
                return;
            }
        }

        if (path.contains("/api/auth/request-otp")) {

            String identifier = request.getParameter("identifier"); 

            boolean allowedIp = rateLimitService.tryConsume(
                    "OTP_IP_" + ip,
                    10,
                    Duration.ofMinutes(1)
            );

            boolean allowedUser = rateLimitService.tryConsume(
                    "OTP_USER_" + identifier,
                    3,
                    Duration.ofMinutes(5)
            );

            if (!allowedIp || !allowedUser) {
                send429(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void send429(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Too many requests\"}");
    }
}
