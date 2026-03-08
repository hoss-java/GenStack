import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class PasswordEncryptor {

    private static final String ALGORITHM = "AES";

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java PasswordEncryptor <password>");
            return;
        }

        String password = args[0];
        try {
            // Generate a secret key for AES
            SecretKey secretKey = generateKey();

            // Encrypt the password
            String encryptedPassword = encrypt(password, secretKey);
            System.out.println("Encrypted Password: " + encryptedPassword);
            // Print the base64 representation of the key for later use
            System.out.println("Secret Key (base64): " + Base64.getEncoder().encodeToString(secretKey.getEncoded()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Generate a new secret key for AES
    private static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(128);
        return keyGen.generateKey();
    }

    // Encrypt the given plain text
    private static String encrypt(String plainText, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
}
