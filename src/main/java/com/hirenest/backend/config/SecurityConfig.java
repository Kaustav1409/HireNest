package com.hirenest.backend.config;

import com.hirenest.backend.security.JwtFilter;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/login.html",
                                "/register.html",
                                "/forgot-password.html",
                                "/reset-password.html",
                                // Job seeker onboarding shell (HTML + assets)
                                "/onboarding",
                                "/onboarding.html",
                                "/onboarding.htm",
                                "/styles.css",
                                "/login.js",
                                "/register.js",
                                "/forgot-password.js",
                                "/reset-password.js",
                                "/onboarding.js",
                                "/dashboard/**",
                                "/js/**",
                                "/css/**",
                                "/images/**").permitAll()
                        .requestMatchers("/script.js").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/recruiter/**").hasRole("RECRUITER")
                        .requestMatchers(HttpMethod.POST, "/api/jobs").hasRole("RECRUITER")
                        .requestMatchers(HttpMethod.POST, "/api/jobs/save").hasRole("JOB_SEEKER")
                        .requestMatchers(HttpMethod.GET, "/api/jobs/saved/**").hasRole("JOB_SEEKER")
                        .requestMatchers(HttpMethod.GET, "/api/jobs/search").authenticated()
                        .requestMatchers("/api/skills/suggestions/**").hasRole("JOB_SEEKER")
                        .requestMatchers("/api/job-seeker/**").hasRole("JOB_SEEKER")
                        .requestMatchers(HttpMethod.POST, "/api/ai/chat").hasRole("JOB_SEEKER")
                        .requestMatchers(HttpMethod.GET, "/api/quiz/recommended/**").hasRole("JOB_SEEKER")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

