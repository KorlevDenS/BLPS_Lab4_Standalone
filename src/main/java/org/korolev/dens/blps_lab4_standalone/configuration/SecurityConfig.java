package org.korolev.dens.blps_lab4_standalone.configuration;

import org.korolev.dens.blps_lab4_standalone.filters.ExceptionHandlerFilter;
import org.korolev.dens.blps_lab4_standalone.filters.JwtAuthFilter;
import org.korolev.dens.blps_lab4_standalone.repositories.ClientRepository;
import org.korolev.dens.blps_lab4_standalone.repositories.NotificationRepository;
import org.korolev.dens.blps_lab4_standalone.repositories.PermissionRepository;
import org.korolev.dens.blps_lab4_standalone.repositories.RoleRepository;
import org.korolev.dens.blps_lab4_standalone.services.ClientService;
import org.korolev.dens.blps_lab4_standalone.services.PasswordService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ExceptionHandlerFilter handlerFilter;

    private final PasswordService passwordService;
    private final ClientRepository clientRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final NotificationRepository notificationRepository;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, ExceptionHandlerFilter handlerFilter,
                          ClientRepository clientRepository, PermissionRepository permissionRepository,
                          RoleRepository roleRepository,
                          PasswordService passwordService, NotificationRepository notificationRepository) {
        this.permissionRepository = permissionRepository;
        this.jwtAuthFilter = jwtAuthFilter;
        this.passwordService = passwordService;
        this.clientRepository = clientRepository;
        this.roleRepository = roleRepository;
        this.handlerFilter = handlerFilter;
        this.notificationRepository = notificationRepository;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new ClientService(this.passwordService, this.clientRepository,
                this.permissionRepository, this.roleRepository, this.notificationRepository);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                        "client/register",
                        "client/login",
                        "chapter/get/all",
                        "comment/get/all/by/topic/*",
                        "topic/get/all/by/chapter/*",
                        "topic/get/by/id/*",
                        "comment/test/message",
                        "topic/get/image/*").permitAll())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(handlerFilter, JwtAuthFilter.class)
                .build();
    }

    @Bean
    static RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy(
                """
                        ROLE_ADMIN > ROLE_MODER
                        ROLE_MODER > ROLE_USER
                        """);
        return hierarchy;
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService());
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

}
