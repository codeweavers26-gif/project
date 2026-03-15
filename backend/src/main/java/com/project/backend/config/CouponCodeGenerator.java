package com.project.backend.config;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.util.function.Predicate;

@Component
public class CouponCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom random = new SecureRandom();

    public String generateCode(String prefix) {
        StringBuilder code = new StringBuilder(prefix);
        
        if (!prefix.endsWith("-")) {
            code.append("-");
        }
        
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(index));
        }
        
        return code.toString();
    }

    public String generateUniqueCode(String prefix, Predicate<String> existsChecker) {
        int maxAttempts = 100;
        int attempts = 0;
        
        while (attempts < maxAttempts) {
            String code = generateCode(prefix);
            if (!existsChecker.test(code)) {
                return code;
            }
            attempts++;
        }
        
        throw new RuntimeException("Failed to generate unique coupon code after " + maxAttempts + " attempts");
    }
}