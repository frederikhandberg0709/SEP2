package via.sep2.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private static PasswordHasher instance;
    private final SecureRandom secureRandom;

    private PasswordHasher() {
        this.secureRandom = new SecureRandom();
    }

    public static synchronized PasswordHasher getInstance() {
        if (instance == null) {
            instance = new PasswordHasher();
        }
        return instance;
    }

    public String hashPassword(String password) {
        byte[] salt = generateSalt();
        byte[] hash = hashPassword(password, salt);

        return Base64.getEncoder().encodeToString(salt) + ":" +
                Base64.getEncoder().encodeToString(hash);
    }

    public boolean verifyPassword(String password, String storedHash) {
        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 2) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] storedHashBytes = Base64.getDecoder().decode(parts[1]);
            byte[] computedHash = hashPassword(password, salt);

            return slowEquals(storedHashBytes, computedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return salt;
    }

    private byte[] hashPassword(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    ITERATIONS,
                    KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    private boolean slowEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
