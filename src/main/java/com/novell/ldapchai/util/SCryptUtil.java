package com.novell.ldapchai.util;

import net.iharder.Base64;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

public class SCryptUtil {
    final static int SCRYPT_LENGTH = 32;              // dkLen

    public static boolean check(String passwd, String hashed) {
        try {
            String[] parts = hashed.split("\\$");

            if (parts.length != 5 || !parts[1].equals("s0")) {
                throw new IllegalArgumentException("Invalid hashed value");
            }

            long params = Long.parseLong(parts[2], 16);
            byte[] salt = Base64.decode(parts[3]);
            byte[] derived0 = Base64.decode(parts[4]);

            int N = (int) Math.pow(2, params >> 16 & 0xffff);
            int r = (int) params >> 8 & 0xff;
            int p = (int) params      & 0xff;

            byte[] derived1 = org.bouncycastle.crypto.generators.SCrypt.generate(passwd.getBytes("UTF-8"), salt, N, r, p, SCRYPT_LENGTH);

            if (derived0.length != derived1.length) {
                return false;
            }

            int result = 0;
            for (int i = 0; i < derived0.length; i++) {
                result |= derived0[i] ^ derived1[i];
            }
            return result == 0;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("JVM doesn't support UTF-8?");
        } catch (IOException e) {
            throw new IllegalStateException("Issue decoding base64 hash: " + e.getMessage());
        }
    }


    public static String scrypt(String passwd) {
        try {
            final int SALT_LENGTH = 16;
            final int COST = 16;                // N
            final int BLOCK_SIZE = 16;          // r
            final int PARALLELIZATION = 16;     // P

            byte[] salt = new byte[SALT_LENGTH ];
            SecureRandom.getInstance("SHA1PRNG").nextBytes(salt);

            final byte[] pwdBytes = passwd.getBytes("UTF-8");
            final byte[] derived = org.bouncycastle.crypto.generators.SCrypt.generate(pwdBytes, salt, COST, BLOCK_SIZE, PARALLELIZATION, SCRYPT_LENGTH);

            String params = Long.toString(log2(COST) << 16L | BLOCK_SIZE << 8 | PARALLELIZATION, 16);

            StringBuilder sb = new StringBuilder((salt.length + derived.length) * 2);
            sb.append("$s0$").append(params).append('$');
            sb.append(Base64.encodeBytes(salt)).append('$');
            sb.append(Base64.encodeBytes(derived));
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("JVM doesn't support UTF-8?");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("JVM doesn't support SHA1PRNG or HMAC_SHA256?");
        }
    }

    private static int log2(int n) {
        int log = 0;
        if ((n & 0xffff0000 ) != 0) { n >>>= 16; log = 16; }
        if (n >= 256) { n >>>= 8; log += 8; }
        if (n >= 16 ) { n >>>= 4; log += 4; }
        if (n >= 4  ) { n >>>= 2; log += 2; }
        return log + (n >>> 1);
    }
}
