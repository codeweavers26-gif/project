package com.project.backend.config;

import java.io.IOException;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.project.backend.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

	private final JwtUtils jwtUtils;
	private final UserDetailsService userDetailsService;
	private final UserRepository UserRepository;

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request,
	                                @NonNull HttpServletResponse response,
	                                @NonNull FilterChain filterChain)
	        throws ServletException, IOException {

	    String header = request.getHeader("Authorization");

	    if (header != null && header.startsWith("Bearer ")) {
	        String token = header.substring(7);

	        if (jwtUtils.validateToken(token)) {

	            String username = jwtUtils.getUsernameFromToken(token);
	            String role = jwtUtils.getRoleFromToken(token); // 👈 ADD THIS

	            // Create authority from role
	            SimpleGrantedAuthority authority =
	                    new SimpleGrantedAuthority("ROLE_" + role); // VERY IMPORTANT

	            UsernamePasswordAuthenticationToken auth =
	                    new UsernamePasswordAuthenticationToken(username, null, List.of(authority));

	            SecurityContextHolder.getContext().setAuthentication(auth);
	        }
	    }

	    filterChain.doFilter(request, response);
	}




}