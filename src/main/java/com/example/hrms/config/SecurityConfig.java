package com.example.hrms.config;

import com.example.hrms.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] WHITE_LIST_URL = {
        "/api/auth/**",
        "/api/v1/auth/**",
        "/api/leave-requests/**",
        "/api/admin/**",
        "/api/payroll/**",
        "/api/notifications/**",
        "/api/attendance/admin/export",
        "/api/attendance/admin/import", 
        "/api/attendance/admin/template",
        "/api/qr-attendance/admin/qr",
        "/api/qr-attendance/admin/qr/image",
        "/api/employees/admin/export",
        "/api/employees/admin/import",
        "/api/employees/admin/template",
        "/v2/api-docs",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/swagger-resources",
        "/swagger-resources/**",
        "/configuration/ui",
        "/configuration/security",
        "/swagger-ui/**",
        "/webjars/**",
        "/swagger-ui.html"
    };

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    // Trả 401 khi chưa xác thực (Unauthorized)
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Unauthorized: " + authException.getMessage() + "\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    // Trả 403 khi đã xác thực nhưng thiếu quyền (Forbidden)
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Access denied: " + accessDeniedException.getMessage() + "\"}");
                })
            )
            .authorizeHttpRequests(req ->
                req
                    .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(WHITE_LIST_URL).permitAll()
                    // Chỉ ADMIN hoặc HR mới được gọi các API quản trị
                    .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "HR")
                    .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Cho phép tất cả origin & header (ONLY for dev)
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
