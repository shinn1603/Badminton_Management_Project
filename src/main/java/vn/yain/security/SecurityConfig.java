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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
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
                .requestMatchers(HttpMethod.GET, "/api/courts/**", "/api/branches/**", "/api/products/**", "/api/tournaments/**", "/api/equipment/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/bookings/slots/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/bookings/slot-lock").permitAll()
                .requestMatchers("/api/bookings/*/deposit").permitAll()
                // Sensitive administrative APIs
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/director/**").hasRole("DIRECTOR")
                .requestMatchers("/api/manager/**").hasAnyRole("MANAGER", "ADMIN", "DIRECTOR")
                .requestMatchers("/api/staff/**", "/api/shifts/week/**", "/api/shifts/assign", "/api/shifts/remove").hasAnyRole("MANAGER", "ADMIN", "DIRECTOR")
                .requestMatchers("/api/inventory/**").hasAnyRole("MANAGER", "ADMIN", "DIRECTOR")
                .requestMatchers("/api/shifts/summary", "/api/shifts/close").hasAnyRole("POS", "MANAGER", "ADMIN", "DIRECTOR")
                .requestMatchers("/api/bookings/*/checkin", "/api/bookings/*/checkout", "/api/bookings/*/transfer", "/api/bookings/*/order-service").hasAnyRole("POS", "MANAGER", "ADMIN", "DIRECTOR")
                // General API fallback
                .requestMatchers("/api/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
