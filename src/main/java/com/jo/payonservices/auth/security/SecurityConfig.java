package com.jo.payonservices.auth.security;

import com.jo.payonservices.auth.filter.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtAuthFilter;

    public SecurityConfig(JwtFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/register","/api/v1/auth/login",
                                "/api/v1/auth/logout"
                                ,"/api/v1/auth/refresh","/api/v1/wallet/syncContacts",
                                "/api/v1/transactions/transfer/ack","/api/v1/onPay/receipts/webhook",
                                "/api/v1/onPay/receipts/{transactionCode}",
                                "/ws/notifications/**",
                                "/error").permitAll()
                        .requestMatchers("/api/v1/auth/upload-thumbnail").authenticated()
                        .requestMatchers("/api/v1/auth/update").authenticated()
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/wallet/initial").authenticated()
                        .requestMatchers("/api/v1/wallet/walletInfo").authenticated()
                        .requestMatchers("/api/v1/transactions/deposit").authenticated()
                        .requestMatchers("/api/v1/transactions/withdraw").authenticated()
                        .requestMatchers("/api/v1/transactions/transfer").authenticated()
                        .requestMatchers("/api/v1/transactions/getTransactions").authenticated()
                        .requestMatchers("/api/v1/merchant/becomeMerchant").authenticated()
                        .requestMatchers("/api/v1/merchant/getMerchant").authenticated()
                        .requestMatchers("/api/v1/onPay/receipts/{transactionCode}/pay").authenticated()
                        .requestMatchers("/api/v1/onPay/receipts").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}