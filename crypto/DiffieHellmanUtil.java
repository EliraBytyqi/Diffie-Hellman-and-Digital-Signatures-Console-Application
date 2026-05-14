package crypto;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

public final class DiffieHellmanUtil {
    private static final String DH_ALGORITHM = "DH";
    private static final String AES_ALGORITHM = "AES";
    private static final int DEFAULT_AES_KEY_BITS = 128;

    private DiffieHellmanUtil() {
    }

    public static DHParameterSpec generateParameters()
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(DH_ALGORITHM);
        generator.initialize(2048, new SecureRandom());
        return ((javax.crypto.interfaces.DHPublicKey) generator.generateKeyPair()
                .getPublic())
                .getParams();
    }

    public static KeyPair generateKeyPair(DHParameterSpec parameterSpec)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(DH_ALGORITHM);
        generator.initialize(parameterSpec, new SecureRandom());
        return generator.generateKeyPair();
    }

    public static byte[] calculateSharedSecret(PrivateKey privateKey, PublicKey peerPublicKey)
            throws NoSuchAlgorithmException, InvalidKeyException {
        KeyAgreement agreement = KeyAgreement.getInstance(DH_ALGORITHM);
        agreement.init(privateKey);
        agreement.doPhase(peerPublicKey, true);
        return agreement.generateSecret();
    }

    public static SecretKey deriveAesKey(byte[] sharedSecret) throws NoSuchAlgorithmException {
        return deriveAesKey(sharedSecret, DEFAULT_AES_KEY_BITS);
    }

    public static SecretKey deriveAesKey(byte[] sharedSecret, int keySizeBits)
            throws NoSuchAlgorithmException {
        if (keySizeBits != 128 && keySizeBits != 192 && keySizeBits != 256) {
            throw new IllegalArgumentException("AES key size must be 128, 192, or 256 bits.");
        }

        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] digest = sha256.digest(sharedSecret);
        byte[] keyBytes = Arrays.copyOf(digest, keySizeBits / 8);
        return new SecretKeySpec(keyBytes, AES_ALGORITHM);
    }

    public static SecretKey calculateAesKey(PrivateKey privateKey, PublicKey peerPublicKey)
            throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] sharedSecret = calculateSharedSecret(privateKey, peerPublicKey);
        return deriveAesKey(sharedSecret);
    }

    public static String encodePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public static PublicKey decodePublicKey(String encodedPublicKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = Base64.getDecoder().decode(encodedPublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance(DH_ALGORITHM).generatePublic(keySpec);
    }

    public static String encodeDhParameters(DHParameterSpec parameterSpec) {
        String p = Base64.getEncoder().encodeToString(parameterSpec.getP().toByteArray());
        String g = Base64.getEncoder().encodeToString(parameterSpec.getG().toByteArray());
        return p + ":" + g + ":" + parameterSpec.getL();
    }

    public static DHParameterSpec decodeDhParameters(String encodedParameters) {
        String[] parts = encodedParameters.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid DH parameters format.");
        }

        BigInteger p = new BigInteger(Base64.getDecoder().decode(parts[0]));
        BigInteger g = new BigInteger(Base64.getDecoder().decode(parts[1]));
        int l = Integer.parseInt(parts[2]);
        return new DHParameterSpec(p, g, l);
    }
}