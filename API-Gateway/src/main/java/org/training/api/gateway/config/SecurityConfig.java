package org.training.api.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange()
                .pathMatchers("/api/users/register", "/api/users/auth/login", "/api/users/auth/refresh",
                        "/api/users/auth/forgot-password").permitAll()
                .pathMatchers("/actuator/health").permitAll()
                .anyExchange().authenticated()
                .and()
                .csrf().disable()
                .oauth2ResourceServer()
                .jwt();
        return http.build();
    }
}
