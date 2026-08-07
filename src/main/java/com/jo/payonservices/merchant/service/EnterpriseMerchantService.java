package com.jo.payonservices.merchant.service;

import com.jo.payonservices.auth.model.Role;
import com.jo.payonservices.auth.model.users;
import com.jo.payonservices.auth.repository.UserRepo;
import com.jo.payonservices.merchant.dto.EnterpriseMerchantRegisterRequest;
import com.jo.payonservices.merchant.dto.EnterpriseMerchantResponse;
import com.jo.payonservices.merchant.model.EnterpriseMerchantStatus;
import com.jo.payonservices.merchant.model.Merchant;
import com.jo.payonservices.merchant.model.MerchantType;
import com.jo.payonservices.merchant.repository.MerchantRepo;
import com.jo.payonservices.wallet.model.Wallet;
import com.jo.payonservices.wallet.model.WalletStatus;
import com.jo.payonservices.wallet.repository.WalletRepo;
import com.jo.payonservices.wallet.util.AccountUtilization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EnterpriseMerchantService {

    @Autowired
    private MerchantRepo merchantRepo;

    @Autowired
    private UserRepo userRepository;

    @Autowired
    private WalletRepo walletRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AccountUtilization accountUtilization;

    @Autowired(required = false)
    private MerchantWebhookService merchantWebhookService;


    @Transactional
    public Merchant registerPendingMerchant(EnterpriseMerchantRegisterRequest request) {
        String password = UUID.randomUUID().toString();
        users user = users.builder()
                .userName(request.getCompanyName())
                .fullName(request.getCompanyName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(password))
                .role(Role.ENTERPRISE_MERCHANT_PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
        Merchant merchant = Merchant.builder()
                .user(user)
                .merchantUserName(request.getCompanyName())
                .merchantUrl(request.getWebhookUrl())
                .merchantType(MerchantType.ENTERPRISE)
                .taxId(request.getTaxId())
                .enterpriseMerchantStatus(EnterpriseMerchantStatus.PENDING_VERIFICATION)
                .build();

        return merchantRepo.save(merchant);
    }

    public List<Merchant> getPendingMerchants() {
        List<Merchant> merchants = merchantRepo.findByEnterpriseMerchantStatus(EnterpriseMerchantStatus.PENDING_VERIFICATION)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "No active merchant found"));
        return merchants;
    }

    public Merchant getPendingMerchant(UUID merchantId) {
        Merchant merchant = merchantRepo.findByMerchantIdAndEnterpriseMerchantStatus(merchantId,EnterpriseMerchantStatus.PENDING_VERIFICATION)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "No active merchant found"));
        return merchant;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED)
    public void approveMerchant(UUID merchantId) {
        Merchant merchant = merchantRepo.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found with ID: " + merchantId));

        if (merchant.getEnterpriseMerchantStatus() == EnterpriseMerchantStatus.ACTIVE) {
            throw new IllegalStateException("Merchant is already active.");
        }
        Long accountNumber = accountUtilization.generateAccountNumber();
        users user = merchant.getUser();
        merchant.setAccountNumber(accountNumber);
        Wallet enterpriseWallet = Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .accountNumber(accountNumber)
                .walletStatus(WalletStatus.ENTERPRISE)
                .build();
        walletRepository.save(enterpriseWallet);

        String apiKey = "pk_live_" + UUID.randomUUID().toString().replace("-", "");
        String apiSecret = "sk_live_" + UUID.randomUUID().toString().replace("-", "");

        merchant.setApiKey(apiKey);
        merchant.setApiSecret(apiSecret);
        merchant.setEnterpriseMerchantStatus(EnterpriseMerchantStatus.ACTIVE);
        merchantRepo.save(merchant);

        user.setRole(Role.ENTERPRISE_MERCHANT_ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        EnterpriseMerchantResponse.EnterpriseMerchantDataResponse merchantData =
                EnterpriseMerchantResponse.EnterpriseMerchantDataResponse.builder()
                        .companyName(merchant.getMerchantUserName())
                        .companyEmail(merchant.getUser().getEmail())
                        .companyDial(merchant.getUser().getPhoneNumber())
                        .companyPassword(merchant.getUser().getPassword())
                        .companyType(merchant.getMerchantType())
                        .companyStatus(EnterpriseMerchantStatus.ACTIVE)
                        .companyTaxId(merchant.getTaxId())
                        .companyAccountNumber(merchant.getAccountNumber())
                        .companyBalance(enterpriseWallet.getBalance())
                        .companyApiKey(merchant.getApiKey())
                        .companyApiSecret(apiSecret)
                        .companyUrl(merchant.getMerchantUrl())
                        .createdAt(merchant.getUser().getCreatedAt())
                        .updatedAt(LocalDateTime.now())
                        .build();

        EnterpriseMerchantResponse payload = EnterpriseMerchantResponse.builder()
                .companyRole(Role.ENTERPRISE_MERCHANT_ACTIVE)
                .data(merchantData)
                .build();
        if (merchantWebhookService != null) {
            merchantWebhookService.sendApprovalWebhook(payload);
        }
    }

    @Transactional
    public void rejectAndDeleteMerchant(UUID merchantId) {
        Merchant merchant = merchantRepo.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found with ID: " + merchantId));

        users user = merchant.getUser();
        EnterpriseMerchantResponse.EnterpriseMerchantDataResponse merchantData =
                EnterpriseMerchantResponse.EnterpriseMerchantDataResponse.builder()
                        .companyStatus(EnterpriseMerchantStatus.REJECTED)
                        .build();

        EnterpriseMerchantResponse payload = EnterpriseMerchantResponse.builder()
                .companyRole(Role.INDIVIDUAL)
                .data(merchantData)
                .build();
        if (merchantWebhookService != null) {
            merchantWebhookService.sendRejectionWebhook(payload);
        }

        user.setRole(Role.INDIVIDUAL);
        userRepository.save(user);

        merchantRepo.delete(merchant);
    }

    @Transactional
    public Map<String, String> rotateApiKeys(Authentication auth) {
        String username = auth.getName();
        Merchant merchant = merchantRepo.findByUser_UserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Merchant not found" ));

        if (merchant.getEnterpriseMerchantStatus() != EnterpriseMerchantStatus.ACTIVE) {
            throw new IllegalStateException("Only active merchants can rotate API keys.");
        }

        String newApiKey = "pk_live_" + UUID.randomUUID().toString().replace("-", "");
        String newApiSecret = "sk_live_" + UUID.randomUUID().toString().replace("-", "");

        merchant.setApiKey(newApiKey);
        merchant.setApiSecret(passwordEncoder.encode(newApiSecret));
        merchantRepo.save(merchant);
        return Map.of("apiKey", newApiKey, "apiSecret", newApiSecret);
    }

}