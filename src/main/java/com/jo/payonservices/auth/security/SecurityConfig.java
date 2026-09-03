package com.jo.payonservices.auth.security;

import com.jo.payonservices.auth.filter.EnterpriseApiKeyFilter;
import com.jo.payonservices.auth.filter.JwtFilter;
import com.jo.payonservices.merchant.repository.MerchantRepo;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtAuthFilter;
    private final PasswordEncoder passwordEncoder;
    private final MerchantRepo merchantRepo;

    @Bean
    @Order(1)
    public SecurityFilterChain serverFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(request -> {
                    String path = request.getRequestURI();
                    return path.startsWith("/api/v1/enterprise/") && !path.startsWith("/api/v1/enterprise/merchant/form");
                })                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/enterprise/merchant/rotate",
                                "/api/v1/enterprise/merchant/transfer",
                                "/api/v1/enterprise/merchant/transactions",
                                "/api/v1/enterprise/merchant/receipts/create",
                                "/api/v1/enterprise/merchant/receipts")
                        .hasRole("ENTERPRISE_MERCHANT_ACTIVE")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new EnterpriseApiKeyFilter(merchantRepo, passwordEncoder), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/admin/**", "/WEB-INF/**", "/favicon.ico")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.INCLUDE).permitAll()
                        .requestMatchers("/admin/login", "/admin/get", "/favicon.ico").permitAll()
                        .requestMatchers("/WEB-INF/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                )
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/admin/pending", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/logout",
                                "/api/v1/enterprise/merchant/form",
                                "/api/v1/enterprise/merchant/form/**",
                                "/api/v1/auth/refresh",
                                "/api/v1/wallet/syncContacts",
                                "/api/v1/transactions/transfer/ack",
                                "/api/v1/onPay/receipts/webhook",
                                "/api/v1/onPay/receipts/{transactionCode}",
                                "/api/v1/onPay/verify/**",
                                "/ws/notifications/**",
                                "/error",
                                "/storage/thumbnails/**",
                                "/WEB-INF/jsp/**"
                        ).permitAll()
                        .requestMatchers(
                                "/api/v1/auth/upload-thumbnail",
                                "/api/v1/auth/update",
                                "/api/v1/auth/change/**",
                                "/api/v1/auth/me",
                                "/api/v1/wallet/initial",
                                "/api/v1/wallet/walletInfo",
                                "/api/v1/transactions/deposit",
                                "/api/v1/transactions/withdraw",
                                "/api/v1/transactions/transfer",
                                "/api/v1/transactions/getTransactions",
                                "/api/v1/merchant/becomeMerchant",
                                "/api/v1/merchant/getMerchant",
                                "/api/v1/onPay/receipts/{transactionCode}/pay",
                                "/api/v1/onPay/receipts",
                                "/api/v1/notifications/**"
                        ).authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }


}