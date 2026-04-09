package com.example.watch_together.security;

import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Service
public class TotpService {

    private static final int SECRET_SIZE = 20;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final String HMAC_ALGORITHM = "HmacSHA1";

    public String generateSecret() {
        byte[] buffer = new byte[SECRET_SIZE];
        new SecureRandom().nextBytes(buffer);

        Base32 base32 = new Base32();
        return base32.encodeToString(buffer).replace("=", "");
    }

    public boolean isCodeValid(String base32Secret, String code) {
        return isCodeValid(base32Secret, code, 1);
    }

    public boolean isCodeValid(String base32Secret, String code, int window) {
        long currentBucket = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;

        for (long i = -window; i <= window; i++) {
            String generated = generateCode(base32Secret, currentBucket + i);
            System.out.println("Checking TOTP code: " + generated + " for bucket shift: " + i);
            if (generated.equals(code)) {
                return true;
            }
        }
        return false;
    }

    public String buildOtpAuthUrl(String issuer, String accountName, String secret) {
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String encodedAccount = URLEncoder.encode(accountName, StandardCharsets.UTF_8);

        return "otpauth://totp/" + encodedIssuer + ":" + encodedAccount
                + "?secret=" + secret
                + "&issuer=" + encodedIssuer
                + "&algorithm=SHA1"
                + "&digits=6"
                + "&period=30";
    }

    private String generateCode(String base32Secret, long timeBucket) {
        try {
            Base32 base32 = new Base32();
            byte[] key = base32.decode(base32Secret);

            byte[] data = ByteBuffer.allocate(8).putLong(timeBucket).array();

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%06d", otp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate TOTP code", e);
        }
    }
}