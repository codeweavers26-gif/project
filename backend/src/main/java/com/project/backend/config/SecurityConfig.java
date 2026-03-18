package com.project.backend.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.project.backend.service.UserDetailsServiceImpl;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	private final JwtAuthFilter jwtAuthFilter;
	private final UserDetailsServiceImpl userDetailsService;

	@Value("${app.cors.allowedOrigins}")
	private String[] allowedOrigins;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http.cors(Customizer.withDefaults())

				.csrf(csrf -> csrf.disable())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth

						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

						.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

						.requestMatchers("/api/auth/**").permitAll()

						.requestMatchers("/api/admin/**").hasRole("ADMIN")

						.requestMatchers("/api/customer/**").hasRole("CUSTOMER")

						.requestMatchers("/api/orders/**").authenticated()
						.requestMatchers("/api/locations/**", "/api/products/**", "/api/categories/**",
								"/api/sections/**", "/api/catalog/**")
						.permitAll()

						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
			

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();

	}
	@Bean
	public CorsConfigurationSource corsConfigurationSource(
	        @Value("${app.cors.allowedOrigins}") String allowedOriginsString) {
	    
	    List<String> allowedOrigins = Arrays.stream(allowedOriginsString.split(","))
	            .map(String::trim)
	            .collect(Collectors.toList());
	    
	    CorsConfiguration configuration = new CorsConfiguration();
	    
	    configuration.setAllowedOriginPatterns(Arrays.asList(
	        "https://richfrontend.vercel.app",
	        "https://www.richnretired.in",
	        "https://www.richnretired.com",
	        "http://localhost:3000",
	        "http://localhost:3001",
	        "https://project-fnwy.onrender.com", 
	        "http://project-fnwy.onrender.com"  
	    ));
	    
	    configuration.setAllowedOriginPatterns(Arrays.asList(
	        "*richfrontend*",
	        "*richnretired*",
	        "*localhost*",
	        "*project-fnwy*",
	        "*.onrender.com",
	        "*.vercel.app"
	    ));
	    
	    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
	    configuration.setAllowedHeaders(Arrays.asList("*"));
	    configuration.setAllowCredentials(true);
	    configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
	    
	    configuration.setMaxAge(3600L);
	    
	    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	    source.registerCorsConfiguration("/**", configuration);
	    
	    return source;
	}
}