package com.compicar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final com.compicar.autenticacion.inicioSesion.JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(com.compicar.autenticacion.inicioSesion.JwtAuthenticationFilter jwtAuthenticationFilter, CustomAccessDeniedHandler customAccessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    /**
     * Define el bean PasswordEncoder para encriptar contraseñas
     * Usa BCrypt que es el estándar de seguridad recomendado
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
            "/images/**",
            "/static/**",
            "/assets/**",
            "/*.png",
            "/*.ico",
            "/*.svg"
        );
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
            .csrf(csrf -> csrf.disable()) // Deshabilitado para APIs JWT
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/index.html", "/static/**", "/assets/**",
                                "/images/**",
                                "/*.js", "/*.css", "/*.png", "/*.ico", "/*.svg").permitAll()

                .requestMatchers("/api/registro/**").permitAll()
                .requestMatchers("/api/login/**").permitAll()

                // Endpoints publicos de solo lectura para viajes
                .requestMatchers(HttpMethod.GET, "/api/viajes/publicos/**").permitAll()

                .requestMatchers("/api/logout").authenticated()
                .requestMatchers("/api/personas/**").authenticated()

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exception -> exception.accessDeniedHandler(customAccessDeniedHandler))
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(form -> form.disable());

        return http.build();
    }

    @Bean
    public org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
        return username -> {
            throw new org.springframework.security.core.userdetails.UsernameNotFoundException("No user details service for JWT auth");
        };
    }
}

