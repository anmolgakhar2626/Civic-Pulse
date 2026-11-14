package com.smartcity.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPointJwt unauthorizedHandler;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final AuthTokenFilter authTokenFilter;
    private final org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
            UserDetailsServiceImpl userDetailsService,
            AuthEntryPointJwt unauthorizedHandler,
            CustomAccessDeniedHandler accessDeniedHandler,
            AuthTokenFilter authTokenFilter,
            org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource
    ) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
        this.accessDeniedHandler = accessDeniedHandler;
        this.authTokenFilter = authTokenFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedHandler)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/", "/api").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/logout", "/api/auth/otp/**", "/api/auth/login-otp/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/weather/**", "/api/air-quality/**", "/api/gis/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // User management (Admin only)
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // Actuator administrative exposure restriction
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // User profile, password change and notifications
                        .requestMatchers("/api/auth/me", "/api/auth/profile", "/api/auth/change-password").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()

                        // Audit logs
                        .requestMatchers("/api/audit-logs/**").hasAnyRole("ADMIN", "COMMAND_OPERATOR")

                        // Analytics
                        .requestMatchers("/api/analytics/**").hasAnyRole("ADMIN", "COMMAND_OPERATOR", "DEPARTMENT_OFFICER")

                        // Role-based authorization rules
                        // Admin-only endpoints
                        .requestMatchers(HttpMethod.DELETE, "/api/incidents/**").hasRole("ADMIN")

                        // Command Operator & Admin operations
                        .requestMatchers("/api/priority/**").hasAnyRole("ADMIN", "COMMAND_OPERATOR")
                        .requestMatchers("/api/incidents/*/evaluate-priority").hasAnyRole("ADMIN", "COMMAND_OPERATOR")
                        .requestMatchers("/api/incidents/*/assign").hasAnyRole("ADMIN", "COMMAND_OPERATOR")
                        .requestMatchers("/api/incidents/*/auto-assign").hasAnyRole("ADMIN", "COMMAND_OPERATOR")
                        .requestMatchers("/api/incidents/*/generate-response-plan").hasAnyRole("ADMIN", "COMMAND_OPERATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/incidents/**").hasAnyRole("ADMIN", "COMMAND_OPERATOR")

                        // SLA operations
                        .requestMatchers(HttpMethod.POST, "/api/incidents/*/sla").hasAnyRole("ADMIN", "COMMAND_OPERATOR")
                        .requestMatchers("/api/sla/**").hasAnyRole("ADMIN", "COMMAND_OPERATOR", "DEPARTMENT_OFFICER")
                        .requestMatchers(HttpMethod.GET, "/api/incidents/*/sla").hasAnyRole("ADMIN", "COMMAND_OPERATOR", "DEPARTMENT_OFFICER")

                        // Assignment history & Department actions
                        .requestMatchers("/api/incidents/*/assignment-history").hasAnyRole("ADMIN", "COMMAND_OPERATOR", "DEPARTMENT_OFFICER")
                        .requestMatchers("/api/departments/**").hasAnyRole("ADMIN", "COMMAND_OPERATOR", "DEPARTMENT_OFFICER")
                        .requestMatchers(HttpMethod.POST, "/api/incidents/*/actions").hasAnyRole("ADMIN", "COMMAND_OPERATOR", "DEPARTMENT_OFFICER")

                        // IoT telemetry, sensor monitoring, and WebSocket testing
                        .requestMatchers("/api/iot/simulator/**", "/api/websocket/**").hasAnyRole("ADMIN", "COMMAND_OPERATOR")
                        .requestMatchers("/api/iot/**").hasAnyRole("ADMIN", "COMMAND_OPERATOR", "DEPARTMENT_OFFICER")

                        // Response actions & status updates
                        .requestMatchers(HttpMethod.GET, "/api/incidents/*/actions").hasAnyRole("ADMIN", "COMMAND_OPERATOR", "DEPARTMENT_OFFICER", "FIELD_WORKER")
                        .requestMatchers(HttpMethod.PATCH, "/api/actions/*/status").hasAnyRole("ADMIN", "COMMAND_OPERATOR", "DEPARTMENT_OFFICER", "FIELD_WORKER")
                        .requestMatchers(HttpMethod.PATCH, "/api/incidents/*/status").hasAnyRole("ADMIN", "COMMAND_OPERATOR", "DEPARTMENT_OFFICER", "FIELD_WORKER")

                        // Citizen creation and query
                        .requestMatchers(HttpMethod.POST, "/api/incidents").hasAnyRole("ADMIN", "COMMAND_OPERATOR", "CITIZEN")
                        .requestMatchers(HttpMethod.GET, "/api/incidents/**").hasAnyRole("ADMIN", "COMMAND_OPERATOR", "DEPARTMENT_OFFICER", "FIELD_WORKER", "CITIZEN")

                        // Map endpoints
                        .requestMatchers("/api/map/**").authenticated()

                        // Infrastructure monitoring
                        .requestMatchers(HttpMethod.POST, "/api/infrastructure/**").hasAnyRole("ADMIN", "COMMAND_OPERATOR", "OPERATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/infrastructure/**").hasAnyRole("ADMIN", "COMMAND_OPERATOR", "OPERATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/infrastructure/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/infrastructure/**").authenticated()

                        // Citizen complaints and verification
                        .requestMatchers("/api/complaints/attachments/**").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/complaints/*/verify").hasAnyRole("ADMIN", "DEPARTMENT_OFFICER")
                        .requestMatchers(HttpMethod.POST, "/api/complaints/*/external-submission").hasAnyRole("ADMIN", "COMMAND_OPERATOR", "DEPARTMENT_OFFICER", "CITIZEN")
                        .requestMatchers(HttpMethod.POST, "/api/complaints/*/external-sync").hasAnyRole("ADMIN", "COMMAND_OPERATOR", "DEPARTMENT_OFFICER", "CITIZEN")
                        .requestMatchers(HttpMethod.POST, "/api/complaints").hasRole("CITIZEN")
                        .requestMatchers(HttpMethod.GET, "/api/complaints/**").authenticated()
                        .requestMatchers("/api/external-grievances/**").authenticated()

                        // All other API requests require authentication
                        .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
