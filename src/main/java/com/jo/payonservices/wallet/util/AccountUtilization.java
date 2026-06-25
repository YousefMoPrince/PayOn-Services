package com.jo.payonservices.wallet.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;

@Component
public class AccountUtilization {
    private static final Long BANK_PREFIX = 587000000000000L;
    private static final SecureRandom secureRandom = new SecureRandom();
    public Long generateAccountNumber() {

        long timestampPart = Instant.now().toEpochMilli() % 100000000L;
        Long randomPart = secureRandom.nextLong(9000) + 1000;

        return BANK_PREFIX + timestampPart + randomPart;
    }

}
