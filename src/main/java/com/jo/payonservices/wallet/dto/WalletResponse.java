package com.jo.payonservices.wallet.dto;

import com.jo.payonservices.wallet.model.WalletStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WalletResponse {
    private UUID WalletId;
    private String userId;
    private BigDecimal balance;
    private Long accountNumber;
    private WalletStatus walletStatus;
}
