package crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class AESUtil {

    private AESUtil() {
    }

    public static String encrypt(String message, SecretKey key) throws Exception {

        Cipher cipher = Cipher.getInstance("AES");

        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encryptedBytes =
                cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder()
                .encodeToString(encryptedBytes);
    }

    public static String decrypt(String encryptedMessage, SecretKey key)
            throws Exception {

        Cipher cipher = Cipher.getInstance("AES");

        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decodedBytes =
                Base64.getDecoder().decode(encryptedMessage);

        byte[] decryptedBytes =
                cipher.doFinal(decodedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}