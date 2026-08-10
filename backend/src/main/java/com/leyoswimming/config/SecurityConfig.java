package com.leyoswimming.config;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.security.AdminAuthenticationFilter;
import com.leyoswimming.security.CoachAuthenticationFilter;
import com.leyoswimming.security.UserAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final AdminAuthenticationFilter adminAuthenticationFilter;
  private final UserAuthenticationFilter userAuthenticationFilter;
  private final CoachAuthenticationFilter coachAuthenticationFilter;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/user/auth/logout", "/api/coach/auth/logout")
                    .authenticated()
                    .requestMatchers(
                        "/api/admin/auth/**",
                        "/api/user/auth/**",
                        "/api/coach/auth/**",
                        "/api/common/sms/**",
                        "/health",
                        "/error")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/user/**")
                    .hasRole("USER")
                    .requestMatchers("/api/coach/**")
                    .hasRole("COACH")
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(
            adminAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(
            userAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(
            coachAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                        (request, response, authException) -> {
                          response.setStatus(401);
                          response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                          response
                              .getWriter()
                              .write(
                                  "{\"code\":"
                                      + ErrorCode.UNAUTHORIZED.getCode()
                                      + ",\"message\":\""
                                      + ErrorCode.UNAUTHORIZED.getMessage()
                                      + "\",\"data\":null}");
                        })
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) -> {
                          response.setStatus(403);
                          response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                          response
                              .getWriter()
                              .write(
                                  "{\"code\":"
                                      + ErrorCode.FORBIDDEN.getCode()
                                      + ",\"message\":\""
                                      + ErrorCode.FORBIDDEN.getMessage()
                                      + "\",\"data\":null}");
                        }));
    return http.build();
  }
}
