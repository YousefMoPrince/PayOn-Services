package com.jo.payonservices.auth.filter;

import com.jo.payonservices.auth.model.Role;
import com.jo.payonservices.merchant.model.EnterpriseMerchantStatus;
import com.jo.payonservices.merchant.model.Merchant;
import com.jo.payonservices.merchant.repository.MerchantRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class EnterpriseApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-PAYON-API-KEY";
    private static final String API_SECRET_HEADER = "X-PAYON-API-SECRET";

    private final MerchantRepo merchantRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        if (path.startsWith("/api/v1/enterprise/merchant/form")) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);
        String apiSecret = request.getHeader(API_SECRET_HEADER);

        if (apiKey != null && apiSecret != null) {
            Optional<Merchant> merchantOptional = merchantRepo.findByApiKeyWithUser(apiKey);
            if (merchantOptional.isPresent()) {
                Merchant merchant = merchantOptional.get();

                boolean isValidSecret = false;
                if (merchant.getApiSecret() != null && apiSecret != null) {
                    if (merchant.getApiSecret().startsWith("$2a$")) {
                        isValidSecret = passwordEncoder.matches(apiSecret, merchant.getApiSecret());
                    } else {
                        isValidSecret = merchant.getApiSecret().equals(apiSecret);
                    }
                    boolean isActiveStatus = EnterpriseMerchantStatus.ACTIVE.equals(merchant.getEnterpriseMerchantStatus());
                    boolean hasActiveRole = merchant.getUser() != null && Role.ENTERPRISE_MERCHANT_ACTIVE.equals(merchant.getUser().getRole());
                    System.out.println("DEBUG AUTH -> isValidSecret: " + isValidSecret);
                    System.out.println("DEBUG AUTH -> isActiveStatus: " + isActiveStatus);
                    System.out.println("DEBUG AUTH -> hasActiveRole: " + hasActiveRole);
                    System.out.println("DEBUG AUTH -> User Role in DB: " + (merchant.getUser() != null ? merchant.getUser().getRole() : "User is null"));
                    if (isValidSecret && isActiveStatus && hasActiveRole) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                merchant.getUser().getUserName(),
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ENTERPRISE_MERCHANT_ACTIVE"))
                        );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}