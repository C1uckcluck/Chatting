package websocket.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import websocket.demo.config.jwt.JwtAuthenticationFilter;
import websocket.demo.config.jwt.JwtTokenProvider;
import websocket.demo.dto.ApiResponse;
import websocket.demo.repository.MemberRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JsonLoginAuthenticationFilter jsonLoginAuthenticationFilter,
            JwtTokenProvider jwtTokenProvider,
            UserDetailsService userDetailsService
    )
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/ws-stomp/**", "/error", "/uploads/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(200);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            new ObjectMapper().writeValue(response.getWriter(), ApiResponse.success(null));
                        })
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterAt(jsonLoginAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public JsonLoginAuthenticationFilter jsonLoginAuthenticationFilter(
            AuthenticationManager authenticationManager,
            ObjectMapper objectMapper,
            MemberRepository memberRepository,
            JwtTokenProvider jwtTokenProvider
    ) {
        return new JsonLoginAuthenticationFilter(
                authenticationManager,
                objectMapper,
                memberRepository,
                jwtTokenProvider
        );
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:3001"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
