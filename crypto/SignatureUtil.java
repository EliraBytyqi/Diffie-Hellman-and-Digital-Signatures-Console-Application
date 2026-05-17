package crypto;

import java.security.*;
import java.util.Base64;

public class SignatureUtil {

    // Gjenero Çiftin e Çelësave RSA
    public static KeyPair generateKeyPair() throws Exception {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");

        keyGen.initialize(2048);

        return keyGen.generateKeyPair();
    }

    // Nënshkruaj Mesazhin
    public static String signMessage(String message, PrivateKey privateKey) throws Exception {

        Signature signature = Signature.getInstance("SHA256withRSA");

        signature.initSign(privateKey);

        signature.update(message.getBytes());

        byte[] signedBytes = signature.sign();

        return Base64.getEncoder().encodeToString(signedBytes);
    }

    // Verifiko Nënshkrimin
    public static boolean verifySignature(
            String message,
            String signatureText,
            PublicKey publicKey
    ) throws Exception {

        Signature signature = Signature.getInstance("SHA256withRSA");

        signature.initVerify(publicKey);

        signature.update(message.getBytes());

        byte[] signatureBytes = Base64.getDecoder().decode(signatureText);

        return signature.verify(signatureBytes);
    }

    // TESTIM KRYESOR
    public static void main(String[] args) {

        try {

            // Gjenero Çelësat
            KeyPair pair = generateKeyPair();

            PrivateKey privateKey = pair.getPrivate();
            PublicKey publicKey = pair.getPublic();

            String message = "Pershendetje kjo eshte nje prove!";

            // HASHIMI
            String hash = HashUtil.sha256(message);

            System.out.println("Original Message:");
            System.out.println(message);

            System.out.println("\nSHA-256 Hash:");
            System.out.println(hash);

            // NËNSHKRIMI
            String digitalSignature = signMessage(message, privateKey);

            System.out.println("\nDigital Signature:");
            System.out.println(digitalSignature);

            // VERIFIKO
            boolean isVerified = verifySignature(
                    message,
                    digitalSignature,
                    publicKey
            );

            System.out.println("\nSignature Verified:");
            System.out.println(isVerified);

            // Kontroll Integriteti
            String modifiedMessage = "Mesazh i ndryshuar";

            boolean integrityCheck = verifySignature(
                    modifiedMessage,
                    digitalSignature,
                    publicKey
            );

            System.out.println("\nIntegrity Check After Modification:");
            System.out.println(integrityCheck);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}