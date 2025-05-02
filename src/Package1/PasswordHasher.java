
package Package1;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class PasswordHasher {

    // !! IMPORTANT: This is a simple hashing example (SHA-256).
    // For production, use a strong, modern password hashing library like BCrypt or Argon2.
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            // Encode the hash as a Base64 string for storage
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            // In a real application, you would handle this error more robustly
            throw new RuntimeException("Error hashing password", e);
        }
    }


    static boolean verifyPassword(String password, String hashedPasswordFromDB) {
       
        String calculatedHash = hashPassword(password);

        return calculatedHash != null && calculatedHash.equals(hashedPasswordFromDB);
    }
}
