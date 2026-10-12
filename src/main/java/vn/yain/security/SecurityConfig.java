package vn.yain.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"success\":false,\"status\":401,\"message\":\"Phiên đăng nhập đã hết hạn hoặc chưa đăng nhập. Vui lòng đăng nhập lại!\",\"error\":\"Unauthorized\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"success\":false,\"status\":403,\"message\":\"Tài khoản không có quyền thực hiện thao tác này!\",\"error\":\"Forbidden\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Allow static resources
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                // Allow public web routes
                .requestMatchers("/", "/login", "/logout", "/error").permitAll()
                .requestMatchers("/customer/**", "/pos/**", "/manager/**", "/director/**", "/admin/**").permitAll()
                // Allow public auth, webhooks, and websocket endpoints
                .requestMatchers("/api/auth/**", "/ws/**", "/ws-court/**", "/webhook/**", "/api/webhook/**").permitAll()
                .requestMatchers("/api/chatbot/**", "/api/upload/**").permitAll()
                // Public customer booking, courts and catalog lookups
                .requestMatchers(HttpMethod.GET, "/api/courts/**", "/api/branches/**", "/api/products/**", "/api/equipment/**", "/api/director/stats", "/api/manager/stats", "/api/director/branches/**", "/api/policy/**").permitAll()
                .requestMatchers("/api/pricing/**", "/api/promotions/**").permitAll()
                .requestMatchers("/api/director/pricing/**", "/api/director/tournaments/**", "/api/manager/pricing/**", "/api/manager/reports/**").permitAll()
                .requestMatchers("/api/tournaments/**").permitAll()
                .requestMatchers("/api/bookings", "/api/bookings/**").permitAll()
                .requestMatchers("/api/courts", "/api/courts/**").permitAll()
                .requestMatchers("/api/products", "/api/products/**").permitAll()
                .requestMatchers("/api/inventory", "/api/inventory/**").permitAll()
                .requestMatchers("/api/staff", "/api/staff/**").permitAll()
                .requestMatchers("/api/shifts", "/api/shifts/**").permitAll()
                // Administrative & Manager APIs
                .requestMatchers("/api/admin/**").permitAll()
                .requestMatchers("/api/director/**").hasAnyRole("DIRECTOR", "ADMIN")
                .requestMatchers("/api/manager/**").hasAnyRole("MANAGER", "ADMIN", "DIRECTOR")
                .requestMatchers("/api/staff", "/api/staff/**").hasAnyRole("MANAGER", "ADMIN", "DIRECTOR")
                .requestMatchers("/api/inventory", "/api/inventory/**").hasAnyRole("MANAGER", "ADMIN", "DIRECTOR")
                .requestMatchers("/api/shifts", "/api/shifts/**").hasAnyRole("POS", "STAFF", "MANAGER", "ADMIN", "DIRECTOR")
                // Require authentication for all remaining APIs
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
