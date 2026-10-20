package com.sirab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/", "/familia/registro", "/familia/registro/consentimiento",
                        "/familia/registro/datos", "/familia/registro/ubicacion",
                        "/familia/registro/situacion", "/familia/registro/necesidades",
                        "/familia/registro/documento", "/familia/registro/resumen",
                        "/familia/registro/confirmar", "/familia/registro/confirmacion",
                        "/login",
                        "/css/**", "/js/**", "/images/**", "/webjars/**",
                        "/favicon.ico", "/error")
                .permitAll()
                .requestMatchers("/campo/**")
                .hasAnyRole("PERSONAL_CAMPO", "ADMIN")
                .requestMatchers("/coordinacion/**")
                .hasAnyRole("COORDINACION", "ADMIN")
                .requestMatchers("/admin/**")
                .hasRole("ADMIN")
                .anyRequest()
                .denyAll());
        http.formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/campo/registros")
                .failureUrl("/login?error")
                .permitAll());
        http.logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll());
        http.sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.migrateSession()));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
