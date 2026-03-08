package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(auth -> auth
                    // health & static
                    .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                    .pathMatchers("/eureka/**").permitAll()
                    .anyExchange().authenticated()
            )
            // Resource server (validate JWTs issued by Keycloak)
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(Customizer.withDefaults())
            );

        // If you also want Browser SSO on the gateway, uncomment:
        // http.oauth2Login();
        return http.build();
    }
}
