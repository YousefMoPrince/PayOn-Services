package com.jo.payonservices.onpay.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
@Component
public class CodeUtilization {
    private static final Long BANK_PREFIX = 587000000000L;
    private static final SecureRandom secureRandom = new SecureRandom();
    public Long getTransactionCode() {
        Long randomPart = secureRandom.nextLong(900000000) + 100000000;
        return BANK_PREFIX + randomPart;
    }

}
