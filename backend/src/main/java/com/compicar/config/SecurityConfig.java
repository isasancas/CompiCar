package com.compicar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Define el bean PasswordEncoder para encriptar contraseñas
     * Usa BCrypt que es el estándar de seguridad recomendado
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura CORS para Spring Security
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Configura la cadena de filtros de seguridad
     * Permitir acceso sin autenticación a los endpoints de registro y login
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authz -> authz
                // Permitir acceso al endpoint de registro sin autenticación
                .requestMatchers("/api/registro/**").permitAll()
                // Permitir acceso al endpoint de login sin autenticación
                .requestMatchers("/api/login/**").permitAll()
                // Permitir acceso a los endpoints de personas sin autenticación (se modificara mas adelante)
                .requestMatchers("/api/personas/**").permitAll()
                // Las demás peticiones requieren autenticación
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf.disable()); // Deshabilitar CSRF para APIs REST

        return http.build();
    }
}
