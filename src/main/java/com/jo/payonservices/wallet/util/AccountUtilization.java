package com.jo.payonservices.wallet.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;

@Component
public class AccountUtilization {
    private static final long BANK_PREFIX = 587L;
    private static final SecureRandom secureRandom = new SecureRandom();

    public Long generateAccountNumber() {
        long timestampPart = 10_000_000L + (Instant.now().toEpochMilli() % 90_000_000L);
        long randomPart = 1000L + secureRandom.nextInt(9000);
        return (BANK_PREFIX * 1_000_000_000_000L)
                + (timestampPart * 10_000L)
                + randomPart;
    }

}
