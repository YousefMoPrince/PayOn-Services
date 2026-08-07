package com.jo.payonservices.merchant.model;

import com.jo.payonservices.auth.model.users;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "merchant")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Merchant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name ="merchant_id")
    private UUID merchantId;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "user_id")
    private users user;
    @Column(name = "merchant_username",nullable = false)
    private String merchantUserName;
    @Column(name="merchant_url")
    private String merchantUrl;
    @Enumerated(EnumType.STRING)
    @Column(name = "merchant_type",nullable = false)
    private MerchantType merchantType;
    @Enumerated(EnumType.STRING)
    @Column(name = "enterprise_merchant_status")
    private EnterpriseMerchantStatus enterpriseMerchantStatus;
    @Column(name = "account_number",unique = true)
    private Long accountNumber;
    @Column(name = "tax_id",unique = true)
    private String taxId;
    @Column(name = "api_key", unique = true)
    private String apiKey;
    @Column(name = "api_secret")
    private String apiSecret;
}
