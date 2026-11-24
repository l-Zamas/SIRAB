package com.sirab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

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
                        "/familia/consulta",
                        "/login",
                        "/css/**", "/js/**", "/images/**", "/webjars/**",
                        "/favicon.ico", "/error")
                .permitAll()
                .requestMatchers("/campo/registros/*/necesidades/**")
                .hasAnyRole("PERSONAL_CAMPO", "COORDINACION")
                .requestMatchers("/campo/**")
                .hasRole("PERSONAL_CAMPO")
                .requestMatchers("/coordinacion/**")
                .hasRole("COORDINACION")
                .requestMatchers("/admin/**")
                .denyAll()
                .anyRequest()
                .denyAll());
        http.formLogin(form -> form
                .loginPage("/login")
                .successHandler(redirectorPorRol())
                .failureUrl("/login?error")
                .permitAll());
        http.logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll());
        http.sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.migrateSession()));
        return http.build();
    }

    private AuthenticationSuccessHandler redirectorPorRol() {
        return (request, response, authentication) -> {
            String destino = authentication.getAuthorities().stream()
                    .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_COORDINACION"::equals)
                            ? "/coordinacion/reportes"
                            : "/campo/registros";
            response.sendRedirect(request.getContextPath() + destino);
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
