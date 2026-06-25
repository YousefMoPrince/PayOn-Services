package com.jo.payonservices.merchant.model;

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
    @Column(name = "merchant_username",nullable = false)
    private String merchantUserName;
    @Column(name="merchant_url",nullable = false)
    private String merchantUrl;
    @Column(name = "account_number",nullable = false,updatable = false,unique = true)
    private Long accountNumber;
}
